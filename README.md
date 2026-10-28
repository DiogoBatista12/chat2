# Chat TCP em Java

Aplicação de chat em console feita com `Socket`, `ServerSocket`, streams de entrada/saída e threads. O servidor aceita vários clientes ao mesmo tempo e mantém uma thread de atendimento por conexão. Cada mensagem recebida é distribuída aos clientes conectados.

## Requisitos

- JDK 17 ou superior
- Maven 3.8 ou superior (opcional; também é possível compilar com `javac`)

## Executar com Maven

Na pasta deste projeto, compile:

```bash
mvn package
```

Em um terminal, inicie o servidor (porta padrão `5000`):

```bash
java -cp target/classes br.edu.chat.ChatServer
```

Para escolher outra porta:

```bash
java -cp target/classes br.edu.chat.ChatServer 6000
```

Em cada outro terminal, inicie um cliente:

```bash
java -cp target/classes br.edu.chat.ChatClient
```

Ou informe host e porta:

```bash
java -cp target/classes br.edu.chat.ChatClient 127.0.0.1 6000
```

Cada cliente informa um nome único e pode conversar com os demais. Comandos disponíveis:

- `/users` lista os usuários conectados.
- `/quit` encerra a conexão.

## Tempos limite

O servidor desconecta conexões paradas para não manter threads ocupadas indefinidamente:

- **Nome de usuário:** o cliente tem 30 segundos após conectar para informar o nome.
- **Inatividade:** depois de registrado, quem ficar 10 minutos sem enviar nada é desconectado e os demais recebem o aviso de saída.

Os valores (em segundos) são configuráveis ao iniciar o servidor; `chat.idle=0` desativa o limite de inatividade:

```bash
java -Dchat.handshake=15 -Dchat.idle=300 -cp target/classes br.edu.chat.ChatServer
```

## Compilar sem Maven

Na pasta do projeto:

```bash
javac -encoding UTF-8 -d out src/main/java/br/edu/chat/*.java
java -cp out br.edu.chat.ChatServer
```

Abra outro terminal e rode `java -cp out br.edu.chat.ChatClient` para cada participante.

## Organização e concorrência

- `ChatServer`: abre a porta TCP, mantém o conjunto de clientes e distribui mensagens.
- `ClientHandler`: executa em uma thread por cliente; lê linhas do stream e processa `/users` e `/quit`.
- `ChatClient`: usa a thread principal para ler o teclado/enviar mensagens e uma thread receptora para exibir mensagens recebidas sem bloquear a digitação.
- O protocolo atual transmite texto UTF-8 delimitado por linhas.
