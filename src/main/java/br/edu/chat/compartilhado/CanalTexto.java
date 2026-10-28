package br.edu.chat.compartilhado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class CanalTexto {
    private final BufferedReader entrada;
    private final BufferedWriter saida;

    public CanalTexto(Socket socket) throws IOException {
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public String receber() throws IOException {
        StringBuilder linha = new StringBuilder();
        int caractere;
        while ((caractere = entrada.read()) != -1) {
            if (caractere == '\n') {
                break;
            }
            if (linha.length() >= Protocolo.LIMITE_LINHA) {
                throw new IOException("Linha excedeu " + Protocolo.LIMITE_LINHA + " caracteres.");
            }
            linha.append((char) caractere);
        }
        if (caractere == -1 && linha.length() == 0) {
            return null;
        }
        if (linha.length() > 0 && linha.charAt(linha.length() - 1) == '\r') {
            linha.setLength(linha.length() - 1);
        }
        return linha.toString();
    }

    public synchronized void enviar(String mensagem) throws IOException {
        saida.write(mensagem);
        saida.write('\n');
        saida.flush();
    }
}
