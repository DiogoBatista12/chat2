package br.edu.chat.cliente;

import br.edu.chat.compartilhado.CanalTexto;
import br.edu.chat.compartilhado.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClienteChat {
    private ClienteChat() {}

    public static void main(String[] args) {
        try {
            if (args.length > 2) {
                throw new IllegalArgumentException("Uso: ClienteChat [host] [porta]");
            }
            String host = args.length > 0 ? args[0] : "localhost";
            int porta = args.length > 1 ? Protocolo.porta(args[1]) : Protocolo.PORTA_PADRAO;
            executar(host, porta);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Falha no cliente: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void executar(String host, int porta) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), 5000);
            socket.setTcpNoDelay(true);
            CanalTexto canal = new CanalTexto(socket);
            AtomicBoolean saindo = new AtomicBoolean();
            Thread teclado = new Thread(() -> enviarTeclado(socket, canal, saindo), "chat-teclado");
            teclado.setDaemon(true);
            teclado.start();

            System.out.println("Conectado a " + host + ":" + porta);
            try {
                String mensagem;
                while ((mensagem = canal.receber()) != null) {
                    System.out.println(mensagem);
                }
                System.out.println("Conexão encerrada.");
            } catch (IOException e) {
                if (!saindo.get()) {
                    throw e;
                }
            }
        }
    }

    private static void enviarTeclado(Socket socket, CanalTexto canal, AtomicBoolean saindo) {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try {
            String linha;
            while ((linha = teclado.readLine()) != null) {
                if (linha.length() > Protocolo.LIMITE_MENSAGEM) {
                    System.out.println("Mensagem muito longa. Limite: " + Protocolo.LIMITE_MENSAGEM);
                    continue;
                }
                canal.enviar(linha);
                if (linha.strip().equalsIgnoreCase("/quit") || linha.strip().equalsIgnoreCase("/sair")) {
                    saindo.set(true);
                    socket.shutdownOutput();
                    return;
                }
            }
            saindo.set(true);
            socket.shutdownOutput();
        } catch (IOException e) {
            saindo.set(true);
            System.err.println("Não foi possível enviar: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
