# Banco SQLite

O servidor cria `dados/chat.db` e suas tabelas automaticamente na primeira execução.
Não é necessário instalar um serviço de banco de dados. Somente o servidor acessa o
SQLite, por meio de JDBC; os clientes se comunicam exclusivamente por sockets TCP.

- `schema.sql`: definição das tabelas e índice, também empacotada como recurso Java.
- `consultas.sql`: exemplos para consultar usuários e mensagens no SQLite.
- `../src/main/java/br/edu/chat/banco/BancoDados.java`: persistência com JDBC e consultas parametrizadas.

`usuarios` guarda o nome, sua chave única sem distinção entre maiúsculas e minúsculas,
a criação e o último acesso. Reconectar com o mesmo nome reaproveita o registro.
Os nomes identificam participantes, sem autenticação por senha.

`mensagens` guarda autor (chave estrangeira para `usuarios`), conteúdo e data/hora
das mensagens públicas. Mensagens privadas não são persistidas nem exibidas no histórico.
O comando `/history` consulta entre 1 e 50 mensagens, mesmo após reiniciar o servidor.
Os usuários online são mantidos em memória e removidos ao desconectar, sem apagar o histórico.

A conexão JDBC é protegida por sincronização, com chaves estrangeiras habilitadas,
modo WAL e espera de até 5 segundos para bloqueios do SQLite. Use um arquivo por
instância do servidor.

Para escolher o arquivo:

```bash
java --enable-native-access=ALL-UNNAMED -Dchat.database=dados/turma-b.db -cp 'build/classes:lib/*' br.edu.chat.servidor.ServidorChat 6000
```

Opcionalmente, com o utilitário `sqlite3` instalado:

```bash
sqlite3 dados/chat.db < banco/consultas.sql
```

Mensagens usam o horário local do servidor. Os campos de acesso usam `CURRENT_TIMESTAMP`
do SQLite (UTC). Os arquivos `.db-wal` e `.db-shm` durante a execução são auxiliares do SQLite.
