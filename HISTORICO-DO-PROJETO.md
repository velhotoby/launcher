# Histórico do Cobblemon Legacy Launcher

Este arquivo é a memória técnica persistente do projeto. Ele existe porque o histórico do chat
pode não aparecer ao abrir uma nova sessão no VS Code. Atualize este documento sempre que uma
mudança relevante for concluída, testada ou publicada.

Última atualização deste documento: 11 de setembro de 2026.

## Estado atual

- Repositório oficial: `https://github.com/velhotoby/launcher`
- Branch principal: `main`
- Versão estável publicada: `3.4.11`
- Tag estável: `v3.4.11`
- Classe principal: `com.cobblemonlegacy.v3.LauncherApp`
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.11.jar`
- Release: `https://github.com/velhotoby/launcher/releases/tag/v3.4.11`
- Instalador Windows: `windows-installer/dist/Cobblemon-Legacy-Launcher-Installer.exe`
- Política de retenção pública: a release/tag `v3.4.11` é permanente porque seus links estão no
  site; além dela, manter somente as duas releases/tags rotativas mais recentes. A proteção e a
  limpeza estão automatizadas em `.github/workflows/release.yml`

Em 11 de setembro de 2026, as releases e tags `v3.4.8` e `v3.4.9` foram removidas do GitHub.
Permaneceram publicadas somente `v3.4.10` e `v3.4.11`. A `v3.4.11` e os assets
`Cobblemon-Legacy-Launcher-3.4.11.jar` e `Cobblemon-Legacy-Launcher-Installer.exe` nunca entram
na limpeza. Ao publicar novas versões, o workflow preserva esses links e exclui automaticamente
as releases rotativas que ultrapassarem o limite de duas.

## Configuração do jogo

- Minecraft `1.21.1`
- Fabric Loader `0.19.5`
- Cobblemon `1.7.3`
- Pasta da instância: `~/.cobblemon_legacy` no Linux e
  `%USERPROFILE%\.cobblemon_legacy` no Windows
- Pasta interna do launcher: `~/.cobblemon_legacy_launcher` no Linux e
  `%USERPROFILE%\.cobblemon_legacy_launcher` no Windows
- Idioma forçado para Português (Brasil), preservando as demais opções do jogo
- Atalhos de teclado normalizados para um conjunto padrão para novos jogadores

## Servidores configurados

O launcher verifica e mantém estes três endereços em `servers.dat`:

1. `enx-cirion-16.enx.host:10068`
2. `cobblemonlegacy.com.br:10068`
3. `cobblemonlegacy.com.br`

Nome principal exibido: `cubblemon legacy`.

## Login

- Perfil local disponível
- Login original Microsoft/Minecraft por código de dispositivo
- O navegador é aberto no fluxo Microsoft quando possível
- Fechar ou cancelar a janela de código interrompe a autenticação e devolve o controle à tela
  principal sem travar o launcher
- No Linux há tentativas por `xdg-open`, `gio` e `sensible-browser`, além das opções de repetir
  a abertura ou copiar o endereço

## Mods e sincronização

- A sincronização não depende mais do Google Drive
- Downloads permitidos somente por HTTPS em fontes aprovadas, incluindo Modrinth, CurseForge CDN
  e Maven oficial da FTB
- Arquivos são verificados por tamanho e SHA-512
- O catálogo local fica em
  `java-launcher-v3/src/main/resources/backend/trusted-mod-catalog.json`
- Um catálogo remoto pode ser definido em `trustedSync.remoteManifestUrl` ou pela variável
  `COBBLEMON_MANIFEST_URL`
- Arquivos excedentes são enviados para quarentena recuperável
- Durante o jogo, o launcher acompanha incompatibilidades de mods; quando encontra um namespace
  ausente com correspondência exata e confiável, baixa o mod e reinicia o Minecraft
- A identificação automática exige confirmação do ID dentro de `fabric.mod.json`

## Visual atual

- Identidade alinhada ao site `https://www.cobblemonlegacy.com.br/`
- Fundo atual com Pokébolas vermelhas em ambiente escuro
- Fontes Chakra Petch e Inter
- Painéis creme, contornos escuros e paleta vermelho, azul e verde
- Recursos visuais ficam em `java-launcher-v3/src/main/resources/ui`

## Atualizador do launcher

- Consulta `https://api.github.com/repos/velhotoby/launcher/releases/latest`
- Oferece atualizar agora ou continuar com a versão instalada
- Valida nome, host, classe principal e versão interna do JAR baixado
- Desde a `3.4.11`, a nova JVM é desacoplada do processo anterior no Linux e Windows
- No Windows é utilizado `javaw.exe`; no Linux é utilizado `setsid` ou `nohup`
- A versão nova abre automaticamente mesmo quando foi chamada por um atualizador antigo
- Depois de abrir, valida e remove com segurança o JAR anterior
- O arquivo anterior só é apagado se tiver nome válido, classe principal correta e versão interna
  inferior à versão atual
- O workflow `.github/workflows/release.yml` compila e publica JARs quando uma tag `vX.Y.Z` é enviada

## Instalador online para Windows

- Arquivo público:
  `https://github.com/velhotoby/launcher/releases/download/v3.4.11/Cobblemon-Legacy-Launcher-Installer.exe`
- Construído com Inno Setup e o design atual do launcher
- Sempre consulta `releases/latest` durante a instalação
- Baixa o JAR correspondente diretamente do GitHub
- Valida o SHA-256 retornado pela API do GitHub antes de instalar
- Inclui runtime Java 21 x64 para Windows
- Instala o executável nativo `CobblemonLegacyLauncher.exe`
- O executável localiza semanticamente o JAR de maior versão instalado, mantendo os atalhos válidos
  depois dos auto-updates
- Cria atalhos no menu Iniciar, Área de Trabalho e diretório de itens fixados da barra de tarefas
- O desinstalador remove também JARs que tenham sido baixados em atualizações posteriores
- Ainda não possui assinatura Authenticode; o SmartScreen pode mostrar um aviso

SHA-256 do instalador publicado:

`36278cbbe96075dc6283d8162953624a1211dd41a23e549b8cddf69a9eff03f3`

## Validações mais recentes

- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.11.jar --self-test`: aprovado
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.11.jar --backend-probe`: aprovado
- Migração simulada `3.4.10` → `3.4.11`: nova JVM permaneceu aberta e JAR anterior foi removido
- Instalador executado em perfil Wine isolado: aprovado
- JAR baixado pelo instalador: versão `3.4.11`, SHA-256
  `687e857a4352317e4aa972002f0def1a5bf82a6da94274a4324100e65653de7f`
- Runtime Windows executou o autoteste do JAR: aprovado
- Criação dos quatro atalhos previstos no log do instalador: aprovada

## Comandos principais

Compilar o JAR:

```bash
npm ci --omit=dev
bash java-launcher-v3/build.sh
```

Compilar o instalador Windows no ambiente de desenvolvimento atual:

```bash
cd windows-installer
./build-installer.sh
```

Publicar uma nova versão do launcher:

1. Atualizar todas as ocorrências da versão no código, manifesto e documentação.
2. Compilar e executar `--self-test` e `--backend-probe`.
3. Fazer commit.
4. Criar e enviar a tag `vX.Y.Z`.
5. Aguardar o GitHub Actions e validar o asset publicado.

## Decisões anteriores importantes

- A proposta visual `3.5` foi descartada; o desenvolvimento voltou à linha `3.4.x`.
- O launcher atual é Java multiplataforma e deve continuar compatível com Linux e Windows.
- As funções existentes devem ser preservadas quando apenas o visual for alterado.
- Não reintroduzir Google Drive como fonte de mods.
- Não baixar mods de hosts não aprovados ou por correspondência aproximada.

## Solicitações que não devem ser consideradas concluídas sem nova validação

- Foi discutida uma versão APK para Android, mas o estado atual documentado e publicado é o
  launcher Java para PC e o instalador `.exe` para Windows. Não afirmar que existe APK final sem
  localizar e testar um artefato Android correspondente.
