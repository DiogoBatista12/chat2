package br.edu.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Cliente de console: uma thread recebe mensagens enquanto a principal envia. */
public final class ChatClient {
    private ChatClient() {}

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (Socket socket = new Socket(host, port);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter serverOutput = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner console = new Scanner(System.in, StandardCharsets.UTF_8)) {

            Thread receiver = new Thread(() -> {
                try {
                    String message;
                    while ((message = serverInput.readLine()) != null) {
                        System.out.println(message);
                    }
                    System.out.println("Conexão encerrada pelo servidor.");
                } catch (IOException e) {
                    System.out.println("Conexão encerrada: " + e.getMessage());
                }
            }, "chat-receiver");
            receiver.setDaemon(true);
            receiver.start();

            while (console.hasNextLine()) {
                String line = console.nextLine();
                serverOutput.println(line);
                if (line.trim().equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor " + host + ":" + port
                    + " (" + e.getMessage() + ").");
        }
    }
}
