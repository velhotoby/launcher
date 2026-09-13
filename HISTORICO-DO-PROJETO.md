# Histórico do Cobblemon Legacy Launcher

Este arquivo é a memória técnica persistente do projeto. Ele existe porque o histórico do chat
pode não aparecer ao abrir uma nova sessão no VS Code. Atualize este documento sempre que uma
mudança relevante for concluída, testada ou publicada.

Última atualização deste documento: 13 de setembro de 2026.

## Estado atual

- Repositório oficial: `https://github.com/velhotoby/launcher`
- Branch principal: `main`
- Versão estável publicada: `3.4.15`
- Tag estável: `v3.4.15`
- Versão local em teste: `3.4.17`
- Classe principal: `com.cobblemonlegacy.v3.LauncherApp`
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.15.jar`
- JAR local em teste: `dist/Cobblemon-Legacy-Launcher-3.4.17.jar`
- Release: `https://github.com/velhotoby/launcher/releases/tag/v3.4.15`
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

## Posição do minimapa — versão 3.4.17 local

- A 3.4.16 configurava o minimapa à direita antes de iniciar o Minecraft, mas a limpeza do
  `eml-lib` apagava `config/xaerohud.txt` logo depois; o Xaero o recriava à esquerda
- Na execução real de 13/09/2026, o backend `3.4.16-r1` foi usado e o arquivo voltou a
  `fromRight=false` durante a inicialização do jogo; isso confirmou a falha, não apenas o
  uso anterior de um JAR antigo
- A 3.4.17 preserva `config/` e `xaero/` na limpeza pré-jogo, além de capturas de tela,
  caminhos antigos de waypoints e backups Xaero; a limpeza continua ativa para arquivos não
  protegidos
- Antes de iniciar o Minecraft, configura o Xaero's Minimap no lado direito por meio de
  `config/xaerohud.txt` na instância do jogador
- Aplica `x=0`, `centered=false` e `fromRight=true` apenas ao módulo
  `xaerominimap:minimap`; mantém a posição vertical e as demais preferências/módulos
- Cria a linha do módulo quando o arquivo ainda não existe, sem alterar a instância real nos testes
- A operação é idempotente: ao encontrar os valores desejados, não regrava o arquivo

## Servidores configurados

O launcher verifica e mantém estes três endereços em `servers.dat`:

1. `enx-cirion-16.enx.host:10068`
2. `cobblemonlegacy.com.br:10068`
3. `cobblemonlegacy.com.br`

Nome principal exibido: `cubblemon legacy`.

## Login

- Perfil local disponível
- Desde a versão `3.4.14`, o checkbox `Lembrar de mim?` salva somente o nickname do perfil local
  em `~/.cobblemon_legacy_launcher/local-nickname.txt` no Linux ou
  `%USERPROFILE%\.cobblemon_legacy_launcher\local-nickname.txt` no Windows
- O nickname válido é restaurado na próxima abertura; desmarcar a opção apaga o arquivo local
- A opção não salva credenciais Microsoft e fica desabilitada no modo de conta Microsoft
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

## Diagnóstico de entrada no servidor — versão 3.4.15

- O modpack básico continua sendo sincronizado antes do jogo; a descoberta de mods **novos**
  ocorre somente após erro de mod ao entrar em um dos três servidores configurados
- A tentativa é delimitada pela linha `Connecting to ...`; erros de outros servidores,
  desconexões de rede e desconexões após entrar no mundo não iniciam download
- Um relatório de cada falha detectada é salvo em
  `~/.cobblemon_legacy/logs/launcher-connection-errors/` no Linux ou no equivalente da instância
  em `%USERPROFILE%` no Windows; tokens conhecidos são ocultados e o arquivo é privado no Linux
- O relatório inclui o trecho do log, servidor, namespaces e nome/versão exigidos quando o erro
  os informa; depois registra o nome e a versão encontrados no Modrinth ou o motivo da falha
- Versões exatas exigidas pelo erro não são substituídas por versões mais recentes; exigências
  mínimas numéricas são comparadas antes do download
- O JAR candidato precisa ter o ID exato no `fabric.mod.json` e passar por verificação de
  hostname HTTPS aprovado, tamanho e SHA-512 antes da sincronização e do reinício automático
- Quando o erro não informa a versão, a versão registrada é a candidata compatível encontrada
  no Modrinth, não uma versão confirmada pelo servidor; sem ID verificável, não há download
- Mods já presentes com versão incompatível e mods ausentes das fontes consultáveis exigem
  atualização do manifesto confiável; a versão não é adivinhada

## Visual atual

- Identidade alinhada ao site `https://www.cobblemonlegacy.com.br/`
- Fundo atual com Pokébolas vermelhas em ambiente escuro
- Fontes Chakra Petch e Inter
- Painéis creme, contornos escuros e paleta vermelho, azul e verde
- Recursos visuais ficam em `java-launcher-v3/src/main/resources/ui`

## Desempenho automático — versão 3.4.12

- Detecta memória física e quantidade de processadores lógicos antes de iniciar o Minecraft
- Seleciona automaticamente um perfil `low`, `balanced` ou `high`
- Ajusta a memória máxima do Minecraft entre 2 GB e 6 GB conforme a RAM disponível
- Usa G1GC com processamento paralelo de referências, deduplicação de strings e pausa-alvo de 100 ms
- Configura distância de renderização e simulação, entidades, partículas, modo gráfico, mistura de
  biomas, mipmaps, limite de FPS, VSync, sombras e iluminação ambiente
- Em PCs com menos de 7 GB de RAM ou até 4 processadores lógicos usa o perfil econômico
- Em PCs intermediários usa o perfil equilibrado; computadores com ao menos 14 GB e mais de 8
  processadores lógicos usam o perfil de alto desempenho
- Preserva opções não gerenciadas e todos os atalhos do usuário
- Registra o perfil em `.launcher-performance-v1.json` e só reaplica quando o perfil muda ou o
  `options.txt` precisa ser recriado
- Para diagnóstico, `COBBLEMON_PERFORMANCE_PROFILE=low|balanced|high` permite substituir a seleção
  automática

## Modo PC Fraco — versão 3.4.13

- Adiciona um checkbox discreto `PC Fraco` ao lado da orientação do nome do treinador
- Quando selecionado, força o perfil econômico antes de preparar e iniciar o Minecraft
- Mantém a memória calculada conforme a RAM física do computador, evitando uma alocação fixa
  inadequada para máquinas com pouca memória
- Usa gráficos rápidos, renderização 6, simulação 4, entidades em 50%, partículas mínimas,
  mistura de biomas desativada, sombras de entidades desativadas e iluminação ambiente desativada
- Quando o checkbox fica desmarcado, a seleção automática `low`, `balanced` ou `high` continua
  funcionando como na versão 3.4.12
- O controle é bloqueado durante a inicialização para impedir mudança de perfil no meio do processo

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

- `node java-launcher-v3/tools/test-xaero-minimap.js` na 3.4.17: aprovado também contra a
  rotina real de limpeza do `eml-lib`, confirmando que `config/xaerohud.txt` e `xaero/` são
  preservados enquanto um arquivo descartável é removido
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.17.jar --self-test`: aprovado
- `--backend-probe` do JAR `3.4.17` em perfil temporário isolado: aprovado
- Manifesto do JAR `3.4.17`: `Implementation-Version: 3.4.17` confirmado
- SHA-256 local do JAR `3.4.17`:
  `3271083342b595ef4d5379388719dfe2b59b81247035a4708d765f9365e0217d`
- Testes existentes de diagnóstico, reparo de servidor, descoberta de mods e desempenho:
  aprovados na 3.4.17
- O Minecraft ainda estava aberto durante a correção; não foi encerrado nem reiniciado para
  validar visualmente a nova versão no jogo real
- `node java-launcher-v3/tools/test-xaero-minimap.js`: aprovado para criação, preservação dos
  demais módulos, atualização de uma configuração existente, CRLF e idempotência
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.16.jar --self-test`: aprovado
- `--backend-probe` do JAR `3.4.16` com perfil temporário isolado: aprovado
- Manifesto do JAR `3.4.16`: `Implementation-Version: 3.4.16` confirmado
- SHA-256 local do JAR `3.4.16`:
  `6bbe3796f31d5f8d9aa7598237b67ce437d589e345e1c664bdfaa107afbe41db`
- Testes existentes de diagnóstico, reparo de servidor, descoberta de mods e desempenho: aprovados
- A 3.4.16 falhou no teste real porque a limpeza pré-jogo apagou o arquivo de configuração
- JAR 3.4.15 publicado, baixado novamente e aprovado no autoteste; SHA-256 público:
  `0d094da13ab910f4f20a9788754d7a3125aa566a869aa17ed1fffe5ad4a37bfe`
- Manifesto do JAR público: `Implementation-Version: 3.4.15` e classe principal confirmadas
- Workflow de publicação `34735179687`: aprovado, inclusive os testes de diagnóstico, reparo e
  descoberta de mods
- Retenção após a publicação: versões `3.4.15`, `3.4.14` e a permanente `3.4.11`; os assets
  permanentes `Cobblemon-Legacy-Launcher-3.4.11.jar` e
  `Cobblemon-Legacy-Launcher-Installer.exe` continuam presentes no GitHub
- `node java-launcher-v3/src/main/resources/backend/server-error-diagnostics.js --self-test`: aprovado
- `node java-launcher-v3/tools/test-server-repair.js`: aprovado para outro servidor, falha de
  rede, desconexão após entrada, mod ausente e namespaces de registro
- `node java-launcher-v3/tools/test-mod-discovery.js`: aprovado com API simulada, exigência de
  versão exata, confirmação de `fabric.mod.json` e SHA-512
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.15.jar --self-test`: aprovado
- `--backend-probe` do JAR `3.4.15` com perfil temporário isolado: aprovado
- Manifesto do JAR `3.4.15`: `Implementation-Version: 3.4.15` confirmado
- SHA-256 local do JAR `3.4.15`:
  `c7f54b312ffba616d00bdc2116ee103826c7518e3b6fa91ac1a090ceef2d05f3`
- Não foi realizado teste de falha real contra o servidor; a validação de reparo usou logs e API
  simulados sem alterar a instância real do Minecraft
- JAR 3.4.14 publicado, baixado novamente e aprovado no autoteste; SHA-256 público:
  `8d241db74f25dab98dad50a21af732548267f8234531f023491bd1edfe0c7bac`
- Workflow de publicação `34729639580`: aprovado
- Retenção após a publicação: versões `3.4.14`, `3.4.13` e a permanente `3.4.11`
- Assets permanentes da `v3.4.11` (`JAR` e instalador `EXE`): presentes no GitHub
- JAR local `3.4.14`: autoteste de gravação, restauração, alteração e exclusão do nickname aprovado
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.14.jar --self-test`: aprovado
- `--backend-probe` do JAR `3.4.14` com perfil temporário isolado: aprovado
- Teste do perfil de desempenho em Node: aprovado
- Manifesto do JAR `3.4.14`: `Implementation-Version: 3.4.14` confirmado
- SHA-256 local do JAR `3.4.14`:
  `ec9703dddbc02f26a5970f5d73597ce5db650966b2b01a616e377c0dbd6c25c6`
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.13.jar --self-test`: aprovado
- `java -Duser.home=/tmp/cobblemon-launcher-3413-test -jar
  dist/Cobblemon-Legacy-Launcher-3.4.13.jar --backend-probe`: aprovado
- Teste explícito do modo `PC Fraco`: perfil `low`, renderização 6 e simulação 4: aprovado
- Manifesto do JAR 3.4.13: classe principal e `Implementation-Version: 3.4.13` confirmadas
- JAR 3.4.13 publicado, baixado novamente e aprovado no autoteste; SHA-256 público:
  `10d6828fcb3bb13a216017aa9f6249b9ebe2140ebb0c7fb0b7f71459615fbf8c`
- Workflow de publicação `34650435622`: aprovado
- Retenção após a publicação: versões `3.4.13`, `3.4.12` e a permanente `3.4.11`
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.12.jar --self-test`: aprovado
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.12.jar --backend-probe`: aprovado
- Teste dos três perfis, cálculo de RAM e aplicação preservando outras opções: aprovado
- Perfil detectado na máquina de desenvolvimento: alto desempenho, 15,5 GB de RAM, 16 processadores
  lógicos, 5.632 MB para o Minecraft, renderização 12 e simulação 8
- JAR 3.4.12 publicado e baixado novamente para validação: SHA-256
  `625f5161ab93d90f3810ad60cd6acd7adce90aa1e551e75fd3d5ad71dc0677d0`
- Instalador permanente da `v3.4.11` testado após a publicação: detectou, validou e instalou
  automaticamente o JAR `3.4.12`
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
