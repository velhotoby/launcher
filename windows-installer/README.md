# Instalador online para Windows

O instalador consulta `https://api.github.com/repos/velhotoby/launcher/releases/latest`
sempre que é executado. O JAR correspondente à tag mais recente é baixado diretamente da
release e validado com o digest SHA-256 informado pela API do GitHub.

O pacote inclui um runtime Java 21 para Windows e instala um executável nativo que encontra
semanticamente o JAR de maior versão na pasta da aplicação. Por isso, os mesmos atalhos continuam
abrindo o launcher correto depois das atualizações automáticas.

## Compilação no Linux

Pré-requisitos locais:

- `clang`, `lld` e `llvm-rc`;
- Wine;
- Inno Setup em `tools/inno`;
- runtime Java 21 x64 para Windows em `payload/runtime`.

Execute:

```bash
./build-installer.sh
```

O resultado será gravado em `dist/Cobblemon-Legacy-Launcher-Installer.exe`.
