package br.edu.chat.compartilhado;

import java.util.Locale;

public final class Protocolo {
    public static final int PORTA_PADRAO = 5000;
    public static final int LIMITE_LINHA = 4096;
    public static final int LIMITE_MENSAGEM = 2000;
    public static final String AJUDA = "* Comandos: /users | /msg <nome> <mensagem>"
            + " | /history [1-50] | /help | /quit";

    private Protocolo() {}

    public static int porta(String valor) {
        try {
            int porta = Integer.parseInt(valor);
            if (porta >= 1 && porta <= 65535) {
                return porta;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("A porta deve ser um número entre 1 e 65535.");
    }

    public static boolean nomeValido(String nome) {
        return nome != null && nome.matches("[\\p{L}\\p{N}_-]{1,20}");
    }

    public static String chaveNome(String nome) {
        return nome.toLowerCase(Locale.ROOT);
    }
}
