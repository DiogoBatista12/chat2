package br.edu.chat.servidor;

import br.edu.chat.banco.BancoDados;
import br.edu.chat.compartilhado.Protocolo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServidorChat implements AutoCloseable {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ServerSocket socketServidor;
    private final BancoDados banco;
    private final ConcurrentHashMap<String, AtendimentoCliente> usuarios = new ConcurrentHashMap<>();
    private final Set<AtendimentoCliente> conexoes = ConcurrentHashMap.newKeySet();
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final AtomicBoolean encerrado = new AtomicBoolean();
    private final int tempoNome;
    private final int tempoInativo;

    public ServidorChat(int porta, Path arquivoBanco) throws IOException, SQLException {
        tempoNome = tempoConfigurado("chat.handshake", 30, false);
        tempoInativo = tempoConfigurado("chat.idle", 600, true);
        banco = new BancoDados(arquivoBanco);
        try {
            socketServidor = new ServerSocket(porta);
        } catch (IOException e) {
            try {
                banco.close();
            } catch (SQLException fechamento) {
                e.addSuppressed(fechamento);
            }
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 1) {
                throw new IllegalArgumentException("Uso: ServidorChat [porta]");
            }
            int porta = args.length == 1 ? Protocolo.porta(args[0]) : Protocolo.PORTA_PADRAO;
            Path arquivo = Path.of(System.getProperty("chat.database", "dados/chat.db"));
            try (ServidorChat servidor = new ServidorChat(porta, arquivo)) {
                Thread desligamento = new Thread(servidor::close, "chat-desligamento");
                Runtime.getRuntime().addShutdownHook(desligamento);
                try {
                    servidor.executar();
                } finally {
                    try {
                        Runtime.getRuntime().removeShutdownHook(desligamento);
                    } catch (IllegalStateException ignored) {
                    }
                }
            }
        } catch (IOException | SQLException | IllegalArgumentException e) {
            System.err.println("Não foi possível iniciar o servidor: " + e.getMessage());
            System.exit(1);
        }
    }

    public int porta() {
        return socketServidor.getLocalPort();
    }

    public void executar() throws IOException {
        System.out.println("Servidor iniciado na porta " + porta() + ". Aguardando clientes...");
        try {
            while (!encerrado.get()) {
                Socket socket = socketServidor.accept();
                AtendimentoCliente cliente = new AtendimentoCliente(this, socket, tempoNome, tempoInativo);
                conexoes.add(cliente);
                if (encerrado.get()) {
                    finalizar(cliente);
                    break;
                }
                try {
                    threads.execute(cliente);
                } catch (RejectedExecutionException e) {
                    finalizar(cliente);
                    if (!encerrado.get()) {
                        throw e;
                    }
                }
            }
        } catch (SocketException e) {
            if (!encerrado.get()) {
                throw e;
            }
        } finally {
            close();
        }
    }

    boolean registrar(AtendimentoCliente cliente) throws SQLException {
        String chave = Protocolo.chaveNome(cliente.nome());
        if (usuarios.putIfAbsent(chave, cliente) != null) {
            return false;
        }
        try {
            banco.registrarUsuario(cliente.nome());
            return true;
        } catch (SQLException e) {
            usuarios.remove(chave, cliente);
            throw e;
        }
    }

    void finalizar(AtendimentoCliente cliente) {
        cliente.close();
        conexoes.remove(cliente);
        if (cliente.nome() != null
                && usuarios.remove(Protocolo.chaveNome(cliente.nome()), cliente)
                && !encerrado.get()) {
            avisar("* " + cliente.nome() + " saiu do chat.");
        }
    }

    void avisar(String mensagem) {
        usuarios.values().forEach(cliente -> cliente.enviar(mensagem));
        System.out.println(mensagem);
    }

    synchronized void publicar(String nome, String texto) {
        String data = LocalDateTime.now().format(DATA);
        String mensagem = "[" + data + "] [" + nome + "]: " + texto;
        try {
            banco.salvarMensagem(nome, texto, data);
        } catch (SQLException e) {
            System.err.println("Falha ao gravar histórico: " + e.getMessage());
        }
        avisar(mensagem);
    }

    void enviarPrivada(AtendimentoCliente origem, String nomeDestino, String texto) {
        AtendimentoCliente destino = usuarios.get(Protocolo.chaveNome(nomeDestino));
        if (destino == null) {
            origem.enviar("ERRO: usuário não encontrado: " + nomeDestino);
            return;
        }
        String mensagem = "[privado " + origem.nome() + " -> " + destino.nome() + "]: " + texto;
        if (destino.enviar(mensagem)) {
            if (destino != origem) {
                origem.enviar(mensagem);
            }
        } else {
            origem.enviar("ERRO: o destinatário desconectou. Mensagem não enviada.");
        }
    }

    List<String> nomesOnline() {
        return usuarios.values().stream().map(AtendimentoCliente::nome)
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    List<String> historico(int quantidade) throws SQLException {
        return banco.ultimasMensagens(quantidade);
    }

    @Override
    public void close() {
        if (encerrado.compareAndSet(false, true)) {
            try {
                socketServidor.close();
            } catch (IOException e) {
                System.err.println("Falha ao fechar a porta do servidor: " + e.getMessage());
            }
            conexoes.forEach(AtendimentoCliente::close);
            threads.shutdownNow();
            try {
                banco.close();
            } catch (SQLException e) {
                System.err.println("Falha ao fechar o banco: " + e.getMessage());
            }
        }
    }

    private static int tempoConfigurado(String propriedade, int padrao, boolean permiteZero) {
        try {
            int segundos = Integer.parseInt(System.getProperty(propriedade, Integer.toString(padrao)));
            if (segundos < (permiteZero ? 0 : 1)) {
                throw new NumberFormatException();
            }
            return Math.multiplyExact(segundos, 1000);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Valor inválido para -D" + propriedade + " (segundos).");
        }
    }
}
