SELECT id, nome, criado_em, ultimo_acesso FROM usuarios ORDER BY nome;

SELECT m.id, u.nome AS autor, m.conteudo, m.enviada_em
FROM mensagens m
JOIN usuarios u ON u.id = m.usuario_id
ORDER BY m.id;

SELECT u.nome, COUNT(m.id) AS total_mensagens
FROM usuarios u
LEFT JOIN mensagens m ON m.usuario_id = u.id
GROUP BY u.id
ORDER BY total_mensagens DESC;
