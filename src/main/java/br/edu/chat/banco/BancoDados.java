package br.edu.chat.banco;

import br.edu.chat.compartilhado.Protocolo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BancoDados implements AutoCloseable {
    private final Connection conexao;

    public BancoDados(Path arquivo) throws IOException, SQLException {
        Path caminho = arquivo.toAbsolutePath();
        Files.createDirectories(caminho.getParent());
        conexao = DriverManager.getConnection("jdbc:sqlite:" + caminho);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys = ON");
            comando.execute("PRAGMA busy_timeout = 5000");
            comando.execute("PRAGMA journal_mode = WAL");
            try (InputStream recurso = BancoDados.class.getResourceAsStream("/banco/schema.sql")) {
                if (recurso == null) {
                    throw new IOException("Recurso banco/schema.sql não encontrado. Recompile o projeto.");
                }
                String schema = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        comando.execute(sql);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            try {
                conexao.close();
            } catch (SQLException fechamento) {
                e.addSuppressed(fechamento);
            }
            throw e;
        }
    }

    public synchronized void registrarUsuario(String nome) throws SQLException {
        String sql = """
                INSERT INTO usuarios (nome, chave_nome) VALUES (?, ?)
                ON CONFLICT(chave_nome) DO UPDATE SET
                    nome = excluded.nome, ultimo_acesso = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, nome);
            comando.setString(2, Protocolo.chaveNome(nome));
            comando.executeUpdate();
        }
    }

    public synchronized void salvarMensagem(String nome, String texto, String data) throws SQLException {
        String sql = """
                INSERT INTO mensagens (usuario_id, conteudo, enviada_em)
                VALUES ((SELECT id FROM usuarios WHERE chave_nome = ?), ?, ?)
                """;
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, Protocolo.chaveNome(nome));
            comando.setString(2, texto);
            comando.setString(3, data);
            comando.executeUpdate();
        }
    }

    public synchronized List<String> ultimasMensagens(int quantidade) throws SQLException {
        String sql = """
                SELECT u.nome, m.conteudo, m.enviada_em
                FROM mensagens m JOIN usuarios u ON u.id = m.usuario_id
                ORDER BY m.id DESC LIMIT ?
                """;
        List<String> mensagens = new ArrayList<>();
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, quantidade);
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    mensagens.add("[" + resultado.getString("enviada_em") + "] ["
                            + resultado.getString("nome") + "]: " + resultado.getString("conteudo"));
                }
            }
        }
        Collections.reverse(mensagens);
        return List.copyOf(mensagens);
    }

    @Override
    public synchronized void close() throws SQLException {
        conexao.close();
    }
}
