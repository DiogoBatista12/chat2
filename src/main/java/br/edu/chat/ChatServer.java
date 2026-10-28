package br.edu.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class ChatServer {
    private static final int HANDSHAKE_TIMEOUT_MS = Integer.getInteger("chat.handshake", 30) * 1000;
    private static final int IDLE_TIMEOUT_MS = Integer.getInteger("chat.idle", 600) * 1000;
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Set<String> nicknames = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientThreads = Executors.newCachedThreadPool();

    private ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        new ChatServer(port).start();
    }

    private void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port + ". Aguardando clientes...");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                clientThreads.execute(new ClientHandler(socket));
            }
        } finally {
            clientThreads.shutdownNow();
        }
    }

    private synchronized boolean register(ClientHandler client, String nickname) {
        if (!nicknames.add(nickname.toLowerCase(Locale.ROOT))) {
            return false;
        }
        client.nickname = nickname;
        clients.add(client);
        return true;
    }

    private synchronized void unregister(ClientHandler client) {
        if (clients.remove(client)) {
            nicknames.remove(client.nickname.toLowerCase(Locale.ROOT));
            broadcast("* " + client.nickname + " saiu do chat.");
        }
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
        System.out.println(message);
    }

    private String onlineUsers() {
        StringBuilder names = new StringBuilder("* Usuários online: ");
        boolean first = true;
        for (ClientHandler client : clients) {
            if (!first) names.append(", ");
            names.append(client.nickname);
            first = false;
        }
        return names.toString();
    }

    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter writer;
        private String nickname;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket clientSocket = socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         clientSocket.getInputStream(), StandardCharsets.UTF_8))) {
                writer = new PrintWriter(new OutputStreamWriter(
                        clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                // Evita que uma conexão que não envia o nome ocupe uma thread para sempre.
                clientSocket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
                send("Informe seu nome de usuário:");
                String requestedName = reader.readLine();
                if (!validNickname(requestedName)) {
                    send("ERRO: nome inválido (use de 1 a 20 letras, números, '_' ou '-').");
                    return;
                }
                if (!register(this, requestedName.trim())) {
                    send("ERRO: esse nome já está em uso.");
                    return;
                }

                clientSocket.setSoTimeout(IDLE_TIMEOUT_MS);

                send("* Conectado. Digite /users para ver quem está online e /quit para sair.");
                broadcast("* " + nickname + " entrou no chat.");

                String message;
                while ((message = reader.readLine()) != null) {
                    String trimmed = message.trim();
                    if (trimmed.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    if (trimmed.equalsIgnoreCase("/users")) {
                        send(onlineUsers());
                    } else if (!trimmed.isEmpty()) {
                        broadcast("[" + nickname + "]: " + message);
                    }
                }
            } catch (SocketTimeoutException e) {
                if (nickname == null) {
                    send("ERRO: tempo esgotado para informar o nome de usuário.");
                    System.out.println("Conexão descartada: nome não informado a tempo.");
                } else {
                    send("* Você foi desconectado por inatividade.");
                    System.out.println("Desconectado por inatividade: " + nickname);
                }
            } catch (IOException e) {
                if (nickname != null) {
                    System.out.println("Conexão encerrada para " + nickname + ": " + e.getMessage());
                }
            } finally {
                if (nickname != null) {
                    unregister(this);
                }
            }
        }

        private synchronized void send(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }
    }

    private static boolean validNickname(String nickname) {
        return nickname != null && nickname.trim().matches("[\\p{L}\\p{N}_-]{1,20}");
    }
}