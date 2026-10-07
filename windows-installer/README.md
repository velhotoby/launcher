# Instalador seguro para Windows

Desde a versão 2.0, o instalador é um executável nativo de estágio único. Ele não usa o carregador
do Inno Setup e não extrai nem executa outro instalador em `%TEMP%`, removendo o fluxo que causava
o erro 4551 em computadores protegidos pelo Controle de Aplicativos do Windows.

O pacote instala por usuário em `%LOCALAPPDATA%\Programs\Cobblemon Legacy Launcher`, inclui o
runtime Java 21 e cria atalhos que executam diretamente o `javaw.exe` oficial com um bootstrap
Java. Nenhum executável próprio secundário é iniciado durante a instalação.

O instalador baixa o asset fixo `Cobblemon-Legacy-Launcher-Windows.jar` da release mais recente e
confere seu SHA-256. Se o GitHub estiver temporariamente indisponível, usa a cópia da versão
incorporada no instalador. O log fica em
`%USERPROFILE%\.cobblemon_legacy_launcher\logs\windows-installer.log` e entra no pacote criado
pelo botão **Salvar Logs**.

## Compilação no Linux

Pré-requisitos locais:

- runtime Java 21 x64 para Windows em `payload/runtime`.
- JDK 21 com `javac` e `jar` (ou `JAVAC_BIN`/`JAR_BIN` definidos);
- `zip`, `clang`, `lld` e `llvm-rc`.

Execute:

```bash
./build-installer.sh
```

O resultado será gravado em `dist/Cobblemon-Legacy-Launcher-Installer.exe`.

O instalador foi feito para Windows 10/11 x64 e usa o `tar.exe` nativo e assinado pelo Windows
somente para extrair o arquivo de dados do Java diretamente na pasta final.
