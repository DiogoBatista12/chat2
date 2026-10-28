package br.edu.chat.servidor;

import br.edu.chat.compartilhado.CanalTexto;
import br.edu.chat.compartilhado.Protocolo;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

final class AtendimentoCliente implements Runnable, AutoCloseable {
    private final ServidorChat servidor;
    private final Socket socket;
    private final int tempoNome;
    private final int tempoInativo;
    private CanalTexto canal;
    private String nome;

    AtendimentoCliente(ServidorChat servidor, Socket socket, int tempoNome, int tempoInativo) {
        this.servidor = servidor;
        this.socket = socket;
        this.tempoNome = tempoNome;
        this.tempoInativo = tempoInativo;
    }

    String nome() {
        return nome;
    }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(tempoNome);
            canal = new CanalTexto(socket);
            enviar("Informe seu nome de usuário:");
            String solicitado = canal.receber();
            if (solicitado == null) {
                return;
            }
            solicitado = solicitado.strip();
            if (!Protocolo.nomeValido(solicitado)) {
                enviar("ERRO: nome inválido (use de 1 a 20 letras, números, '_' ou '-').");
                return;
            }
            nome = solicitado;
            if (!servidor.registrar(this)) {
                enviar("ERRO: esse nome já está em uso.");
                return;
            }
            socket.setSoTimeout(tempoInativo);
            enviar("* Conectado como " + nome + ".");
            enviar(Protocolo.AJUDA);
            servidor.avisar("* " + nome + " entrou no chat.");

            String mensagem;
            while ((mensagem = canal.receber()) != null) {
                String texto = mensagem.strip();
                if (texto.equalsIgnoreCase("/quit") || texto.equalsIgnoreCase("/sair")) {
                    enviar("* Até logo, " + nome + "!");
                    break;
                }
                if (!texto.isEmpty()) {
                    processar(texto);
                }
            }
        } catch (SocketTimeoutException e) {
            enviar(nome == null ? "ERRO: tempo esgotado para informar o nome."
                    : "* Você foi desconectado por inatividade.");
        } catch (SQLException e) {
            enviar("ERRO: não foi possível registrar o usuário no banco.");
            System.err.println("Falha ao registrar usuário: " + e.getMessage());
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Conexão encerrada para " + (nome == null ? "visitante" : nome)
                        + ": " + e.getMessage());
            }
        } finally {
            servidor.finalizar(this);
        }
    }

    private void processar(String texto) {
        if (texto.length() > Protocolo.LIMITE_MENSAGEM) {
            enviar("ERRO: mensagem deve ter no máximo " + Protocolo.LIMITE_MENSAGEM + " caracteres.");
            return;
        }
        if (!texto.startsWith("/")) {
            servidor.publicar(nome, texto);
            return;
        }
        String[] partes = texto.split("\\s+", 2);
        String comando = partes[0].toLowerCase(Locale.ROOT);
        String argumento = partes.length > 1 ? partes[1] : "";
        switch (comando) {
            case "/users", "/usuarios" -> listarUsuarios();
            case "/help", "/ajuda" -> enviar(Protocolo.AJUDA);
            case "/msg" -> {
                String[] destinoTexto = argumento.split("\\s+", 2);
                if (destinoTexto.length < 2 || destinoTexto[1].isBlank()) {
                    enviar("ERRO: use /msg <nome> <mensagem>.");
                } else {
                    servidor.enviarPrivada(this, destinoTexto[0], destinoTexto[1]);
                }
            }
            case "/history", "/historico" -> mostrarHistorico(argumento);
            default -> enviar("ERRO: comando desconhecido. Digite /help.");
        }
    }

    private void listarUsuarios() {
        List<String> nomes = servidor.nomesOnline();
        enviar("* Usuários online (" + nomes.size() + "):");
        nomes.forEach(usuario -> enviar("* - " + usuario));
        enviar("* Fim dos usuários.");
    }

    private void mostrarHistorico(String argumento) {
        int quantidade = 10;
        if (!argumento.isEmpty()) {
            try {
                quantidade = Integer.parseInt(argumento);
                if (quantidade < 1 || quantidade > 50) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                enviar("ERRO: use /history [quantidade de 1 a 50].");
                return;
            }
        }
        try {
            List<String> mensagens = servidor.historico(quantidade);
            enviar("* Histórico público (" + mensagens.size() + " mensagens):");
            mensagens.forEach(mensagem -> enviar("[histórico] " + mensagem));
            enviar("* Fim do histórico.");
        } catch (SQLException e) {
            enviar("ERRO: histórico indisponível no momento.");
            System.err.println("Falha ao consultar histórico: " + e.getMessage());
        }
    }

    boolean enviar(String mensagem) {
        try {
            if (canal == null || socket.isClosed()) {
                return false;
            }
            canal.enviar(mensagem);
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
