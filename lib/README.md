# Dependências de execução

O driver SQLite JDBC (`org.xerial:sqlite-jdbc:3.53.4.0`) é baixado automaticamente
por `scripts/dependencias.sh` e verificado com SHA-256. Os arquivos JAR não são
versionados. A conexão com a internet só é necessária no primeiro download.

O Maven também declara esse driver em `pom.xml`. Ele inclui as bibliotecas
nativas do SQLite; não é necessário instalar o executável `sqlite3`.

Projeto e licença: https://github.com/xerial/sqlite-jdbc (Apache 2.0 / BSD 2-Clause).
