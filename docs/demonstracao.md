# Roteiro de apresentação

## Preparação

Abra quatro terminais na pasta do projeto. É necessário JDK 17 ou superior.
No primeiro uso, a compilação baixa o driver SQLite automaticamente.

| Terminal | Comando | Nome a informar |
| --- | --- | --- |
| 1 | `bash scripts/servidor.sh` | — |
| 2 | `bash scripts/cliente.sh` | `Ana` |
| 3 | `bash scripts/cliente.sh` | `Bruno` |
| 4 | `bash scripts/cliente.sh` | `Carla` |

Mantenha os três clientes abertos durante as etapas 1 a 3.

## Apresentação

1. **Conexões simultâneas.** Ana digita `/users`. A lista deve conter Ana, Bruno e
   Carla. Mostre os três terminais para comprovar que as conexões estão ativas.
2. **Mensagens públicas.** Ana digita `Olá, pessoal!`. Todos recebem. Bruno e Carla
   respondem; mostre as respostas nos outros terminais. Explique que o servidor
   atende cada conexão em uma thread e encaminha as mensagens pelos streams.
3. **Mensagem privada.** Ana digita `/msg Bruno Esta mensagem é privada.`.
   A mensagem aparece somente nos terminais de Ana e Bruno.
4. **Desconexão normal.** Carla digita `/quit`. Ana e Bruno recebem o aviso de saída
   e continuam trocando mensagens.
5. **Reconexão e queda.** Abra Carla novamente com o mesmo nome, depois encerre
   abruptamente o processo cliente. Ana e Bruno devem continuar conversando.
6. **Persistência.** Ana digita `/history 50`. Pare o servidor com Ctrl+C, inicie-o
   novamente e reconecte Ana. `/history 50` recupera as mensagens públicas anteriores.
7. **SQLite.** Abra `dados/chat.db` em um visualizador SQLite ou use as consultas
   de `banco/consultas.sql`. Mostre `usuarios` e `mensagens`; a mensagem privada
   não aparece no banco.

## Demonstração reproduzível

Execute:

```bash
bash scripts/demonstrar.sh
```

O teste inicia os processos reais dos clientes e verifica comunicação, isolamento
da mensagem privada, saídas normal e abrupta, reconexão, encerramento e persistência.
Não utiliza nem altera o banco normal `dados/chat.db`.

O resultado esperado é:

```text
[OK] Três processos clientes conectados simultaneamente: Ana, Bruno e Carla.
[OK] Mensagens dos três participantes recebidas por todos, com acentos preservados.
[OK] Mensagem privada recebida somente por Ana e Bruno.
[OK] Nome duplicado e comandos inválidos tratados sem interromper o chat.
[OK] Saída com /quit preservou a troca de mensagens dos demais.
[OK] Reconexão com o mesmo nome e queda abrupta não afetaram os demais.
[OK] SQLite consultado: três usuários, cinco mensagens públicas e nenhuma mensagem privada.
[OK] Encerramento do servidor encerrou também os processos clientes.
[OK] Histórico recuperado do SQLite após reiniciar o servidor.

DEMONSTRAÇÃO APROVADA: 9 verificações.
```

Cada execução cria uma pasta em `build/demonstracao/execucao-*` com seu banco
`chat.db` e um `resultado.txt` contendo as verificações aprovadas. PIDs, portas e
horários variam a cada execução.
