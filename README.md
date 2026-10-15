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

## Desafios para evoluir

### Persistência em banco de dados

Adicionar uma camada de repositório para armazenar usuários e histórico de mensagens, usando JDBC e prepared statements. A conexão deve ser gerenciada fora do fluxo de broadcast para que uma operação lenta no banco não bloqueie a entrega de mensagens.

### Envio de imagens

O protocolo atual é textual. Para imagens, definir quadros binários explícitos (por exemplo, tipo da mensagem, tamanho em bytes e conteúdo), validar tamanho/tipo no servidor e transferir os bytes com `InputStream`/`OutputStream`. Não é seguro inserir bytes arbitrários em uma linha de texto; o protocolo do cliente e do servidor precisa mudar em conjunto.
