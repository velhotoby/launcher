# Histórico do Cobblemon Legacy Launcher

Este arquivo é a memória técnica persistente do projeto. Ele existe porque o histórico do chat
pode não aparecer ao abrir uma nova sessão no VS Code. Atualize este documento sempre que uma
mudança relevante for concluída, testada ou publicada.

Última atualização deste documento: 20 de setembro de 2026.

## Estado atual

- Repositório oficial: `https://github.com/velhotoby/launcher`
- Branch principal: `main`
- Versão estável publicada: `3.4.23`
- Tag estável: `v3.4.23`
- Versão local em teste: `3.4.27`
- Classe principal: `com.cobblemonlegacy.v3.LauncherApp`
- JAR local em teste: `dist/Cobblemon-Legacy-Launcher-3.4.27.jar`
- Release: `https://github.com/velhotoby/launcher/releases/tag/v3.4.23`
- Instalador Windows: `windows-installer/dist/Cobblemon-Legacy-Launcher-Installer.exe`
- Política de retenção pública: a release/tag `v3.4.21` é permanente porque seus links estão no
  site; além dela, manter somente as duas releases/tags rotativas mais recentes. A proteção e a
  limpeza estão automatizadas em `.github/workflows/release.yml`

Em 11 de setembro de 2026, as releases e tags `v3.4.8` e `v3.4.9` foram removidas do GitHub.
Em 19 de setembro de 2026, a proteção permanente foi migrada da `v3.4.11` para a `v3.4.21`;
o instalador genérico foi preservado na nova release e a antiga foi removida. A `v3.4.21` e os
assets `Cobblemon-Legacy-Launcher-3.4.21.jar` e `Cobblemon-Legacy-Launcher-Installer.exe` nunca
entram na limpeza. Ao publicar novas versões, o workflow preserva esses links e exclui
automaticamente as releases rotativas que ultrapassarem o limite de duas.

## Configuração do jogo

- Minecraft `1.21.1`
- Fabric Loader `0.19.5`
- Cobblemon `1.8.1`
- Pasta da instância: `~/.cobblemon_legacy` no Linux e
  `%APPDATA%\.cobblemon_legacy` no Windows (pasta usada pelo `eml-lib`)
- Pasta interna do launcher: `~/.cobblemon_legacy_launcher` no Linux e
  `%USERPROFILE%\.cobblemon_legacy_launcher` no Windows
- Idioma forçado para Português (Brasil), preservando as demais opções do jogo
- Atalhos de teclado normalizados para um conjunto padrão para novos jogadores

## Catálogo de mods recebido — versão 3.4.27 local

- `java-launcher-v3/src/main/resources/backend/trusted-mod-catalog.json` foi substituído
  exatamente pelo arquivo fornecido pelo usuário, com `packVersion` `2026.09.20.2` e 128 mods
- Comparado ao catálogo anterior: 30 projetos foram atualizados, 2 foram removidos
  (`Cobblemon Size Variations` e `SimpleTMs`) e 1 foi adicionado (`Cobblemon RIze Tweaks 1.2.3`)
- A substituição também atualiza `Supplementaries` de `3.9.7` para `3.9.9`; tanto essa troca
  quanto a volta do `Cobblemon RIze Tweaks` contrariam ressalvas registradas em versões anteriores,
  mas foram mantidas para preservar sem alterações o catálogo explicitamente fornecido
- O validador interno aprovou esquema, nomes, tamanhos, hashes SHA-512 e hosts: 125 arquivos vêm
  do CDN do Modrinth e 3 do Maven oficial da FTB
- Os 31 arquivos adicionados ou alterados foram baixados das URLs declaradas e conferidos
  integralmente; os 31 tamanhos e hashes SHA-512 coincidem com o catálogo (104.894.046 bytes)
- Autotestes do launcher, backend, desempenho, diagnóstico, reparo de servidor, minimapa,
  descoberta de mods e resource packs: aprovados
- O catálogo incorporado no JAR é idêntico ao anexo, com SHA-256
  `6512755fe16a1972bf7e4c4887108f1bb7e708579fab7558422f0432adfa9249`
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.27.jar`
- SHA-256 local: `6eee0dd1ad0c4295882cc3d2b4097661e4015dc8dd6825bd95ab4232e70b747a`
- A versão `3.4.27` ainda não foi publicada no GitHub

## Todos os resource packs disponíveis ativados — versão 3.4.26 local

- Além dos cinco pacotes já tratados na 3.4.25, o launcher passa a ativar automaticamente
  `cobblemon:uniqueshinyforms`, `cobblemon_smartphone:oldsmartphone`, `high_contrast`,
  `programmer_art`, `mega_showdown:gyaradosjumpingmega`, `mega_showdown:regionbiasmsd`,
  `reborncore:reborncore_darkmode` e `supplementaries:darker_ropes`
- Os IDs foram confirmados diretamente no bytecode e nos `pack.mcmeta` dos JARs instalados;
  o pacote de compatibilidade do Adorn não é registrado porque o mod Adorn não está instalado
- O `Darker Ropes` oficial declara formato 15. Como o usuário solicitou todos os pacotes, ele é
  selecionado e incluído em `incompatibleResourcePacks` como confirmação prévia; isso evita a
  caixa de confirmação do Minecraft 1.21.1 e mantém o pacote carregável
- A ordem dos 14 pacotes obrigatórios agora é normalizada para todos; pacotes extras do jogador
  vêm depois e, por último, fica `file/Cobblemon Legacy Compat` com prioridade máxima
- A instância real ficou com os 14 IDs obrigatórios selecionados, o pacote adicional de tradução
  preservado e somente `supplementaries:darker_ropes` na confirmação de incompatibilidade
- Autotestes do resource pack, JAR, desempenho, diagnóstico, minimapa e descoberta: aprovados
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.26.jar`
- SHA-256 local: `b922855a7d249ad6193733d6e4045b7cca1363afb48947a3015c8510c69e1efa`
- A versão 3.4.26 ainda não foi publicada no GitHub

## Resource packs integrados ativados — versão 3.4.25 local

- O launcher agora garante em toda inicialização, inclusive sem `options.txt`, que estes pacotes
  internos estejam selecionados: `fabric`, `cobblemon:regionbiasforms`,
  `cobblemon:gyaradosjump`, `$polymer-resources` e `moonlight:merged_pack`
- Na interface do Minecraft eles correspondem a Mods do Fabric, Region Bias Forms, Gyarados
  Jump Patterns, Polymer Resources e Recursos Dinâmicos do Moonlight
- Pacotes já escolhidos pelo jogador são preservados e não há duplicação. O pacote local
  `file/Cobblemon Legacy Compat` continua no final da lista, com prioridade máxima
- O autoteste cobre perfil novo, perfil existente, os cinco IDs obrigatórios, ordem do pacote de
  compatibilidade e idempotência. Autotestes do JAR, desempenho, diagnóstico, minimapa e
  descoberta de mods também foram aprovados
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.25.jar`
- SHA-256 local: `09c22c06c9ac172bf2b3640a96e64e677c6450e990dca219190cbebbca292e43`
- A versão 3.4.25 ainda não foi publicada no GitHub

## Compatibilidade dos recursos dos mods — versão 3.4.24 local

- Os 129 JARs ativos e os recursos carregados pelo Minecraft foram auditados; não havia
  resource pack externo instalado nem falha fatal de recarregamento, mas foram encontrados
  JSONs inválidos, referências incorretas de modelos/texturas e 24 referências antigas no
  `options.txt`
- As versões oficiais mais recentes compatíveis com Fabric 1.21.1 foram consultadas no Modrinth.
  Elas não corrigiam o conjunto de defeitos encontrado ou poderiam mudar o mod exigido pelo
  servidor; por isso nenhum JAR verificado foi adulterado ou trocado apenas no cliente
- O launcher agora cria e ativa, antes de abrir o jogo, o resource pack local
  `resourcepacks/Cobblemon Legacy Compat`, formato 34 e prioridade máxima
- A camada corrige os quatro diálogos JSON do KantoNPCs, os JSONs coreanos inválidos do Macaw's
  Trapdoors e Mega Showdown, o parent ausente do modelo `track_arrow` do CobbleNav, as texturas
  do item de tenda e da pelúcia shiny do Pokeblocks e quatro referências de cartas com nomes
  divergentes no Cobble Card Quest
- O launcher remove somente as 24 referências obsoletas de pacotes internos da lista
  `incompatibleResourcePacks`; pacotes externos do jogador são preservados. A instância local
  ficou com `incompatibleResourcePacks:[]` e o pacote de compatibilidade ativado
- Avisos internos inofensivos que pertencem aos JARs oficiais — arquivos de documentação com
  nomes inválidos, blockstates de renderização dinâmica, shaders e um card sem arte fornecida
  pelo autor — não são tratados alterando JARs ou inventando recursos, para preservar a
  verificação SHA-512 e a compatibilidade com o servidor
- Teste com cópias reais dos seis mods: 15 arquivos gerados, todos os JSONs válidos e segunda
  execução idempotente. Autotestes de resource pack, desempenho, diagnóstico, minimapa,
  descoberta de mods, JAR e `--backend-probe`: aprovados; o teste de reparo com subprocesso
  também foi aprovado fora do sandbox
- JAR local: `dist/Cobblemon-Legacy-Launcher-3.4.24.jar`
- SHA-256 local: `65b6b5fb5c62cb289e327aaa37e2eb4e4984d056cdc2f80c9f8d0fcb70054cae`
- A versão 3.4.24 ainda não foi publicada no GitHub

## Remoção dos dois Cobblemon Alphas — versão 3.4.23 local

- Foram retirados do catálogo confiável o `CobblemonAlphas 1.4.1`, ID Fabric
  `cobblemonalphas`, e o `Cobblemon Alphas 2.2`, ID Fabric `mr_cobblemon_alphas`
- Os projetos Modrinth `QnWyhFGf` e `3dnjbPgt` e os arquivos
  `cobblemonalphas-1.4.1.jar` e `cobblemon-alphas-2.2.jar` não fazem mais parte do pacote; o
  catálogo passou de 131 para 129 mods
- A inspeção dos metadados `fabric.mod.json` dos mods ativos confirmou que nenhum outro mod
  declara dependência dessas duas IDs
- A instância local foi sincronizada com os 129 arquivos exatos do catálogo. Os dois JARs
  removidos foram movidos para `.launcher-mods-quarantine-v1` e não permanecem em `mods/`
- Teste real aprovado no servidor principal: conexão aceita, mundo carregado e 619 avanços
  sincronizados, sem tela de registros incompatíveis e sem os dois mods carregados
- Autotestes de diagnóstico, descoberta, reparo, minimapa, desempenho, JAR e `--backend-probe`:
  aprovados; os 129 arquivos locais também foram validados por nome, tamanho e SHA-512
- SHA-256 local do JAR `3.4.23`:
  `a6174462bc16cdb940dc368cee1b8130cd67000da63411bd84fc3161ea650e91`
- A versão `3.4.23` foi publicada no GitHub pelo workflow `35543767125`; compilação, autotestes,
  criação da release e retenção foram aprovados
- O JAR público foi baixado novamente e aprovado em `--self-test` e `--backend-probe`; manifesto
  com `Implementation-Version: 3.4.23`, catálogo com 129 mods e nenhuma das duas entradas
  Cobblemon Alphas confirmados
- SHA-256 do JAR público `3.4.23`:
  `a071de146fd15fad3c673696452403a2b1180f543f44d18d2788e25bae2b01e0`
- Retenção após a publicação: versões rotativas `3.4.23` e `3.4.22`, além da permanente
  `3.4.21`; o JAR e o instalador da versão permanente continuam disponíveis

## Remoção do CobblemonRIzeTweaks — versão 3.4.22 local

- O `CobblemonRIzeTweaks 1.2.3` foi retirado porque estava causando incompatibilidade durante
  batalhas
- O projeto Modrinth `ON4VDdCA`, a versão `YxsbA4Hz` e o arquivo
  `cobblemonRIzetweaks-1.2.3.jar` foram removidos do catálogo confiável incorporado; o catálogo
  passou de 132 para 131 mods
- A entrada de atalho obsoleta `key_key.cobblemonrizetweaks.jumpPCBox` também foi removida da
  configuração padrão
- A inspeção dos metadados `fabric.mod.json` de todos os mods ativos confirmou que nenhum outro
  mod declara dependência do `cobblemonrizetweaks`
- A instância local foi sincronizada com os 131 arquivos exatos do catálogo. O JAR removido foi
  movido para `.launcher-mods-quarantine-v1`, permitindo recuperação, e não permanece em `mods/`
- Teste real aprovado no servidor principal: conexão aceita, mundo carregado e 522 avanços
  inicialmente sincronizados, sem tela de registros incompatíveis e sem o RIzeTweaks carregado
- Autotestes de diagnóstico, descoberta, reparo, minimapa, desempenho, JAR e `--backend-probe`:
  aprovados; os 131 arquivos locais também foram validados por nome, tamanho e SHA-512
- SHA-256 local do JAR `3.4.22`:
  `6263a2cdfb7c39cdd34aed59b0551b42c0a9a95e0c7c004f11694f490391a224`
- A versão `3.4.22` foi publicada no GitHub pelo workflow `35521301629`; compilação, autotestes,
  criação da release e retenção foram aprovados
- O JAR público foi baixado novamente e aprovado em `--self-test` e `--backend-probe`; manifesto
  com `Implementation-Version: 3.4.22`, catálogo com 131 mods e nenhuma referência ao
  RIzeTweaks confirmados
- SHA-256 do JAR público `3.4.22`:
  `64fe372acadbc57ff048484f845e21b619cebca4312c72b3fb7ca128ba6691e2`
- Retenção após a publicação: versões rotativas `3.4.22` e `3.4.19`, além da permanente
  `3.4.21`; o JAR e o instalador da versão permanente continuam disponíveis

## Correção da sincronização com o servidor — versão 3.4.21 local

- O erro real de entrada com `1057 entradas de registro` foi reproduzido e corrigido; os
  namespaces apontados eram `cobblemon`, `cobblemon_picnic`, `cobblesafari`,
  `mega_showdown` e `supplementaries`
- O catálogo incorporado continua com 132 mods e foi alinhado ao conjunto atual do servidor:
  Cobblemon `1.8.1`, Cobblemon Armors `1.6.0+1.8.1`, Picnic `2.4.5`, Trainer Battle
  `1.11.13+1.8.1`, Raid Dens `0.12.1`, CobbleSafari `0.3.5`, Daycare+ `1.5.0`, Mega
  Showdown `1.2.0+1.8.1`, Horret's Extended Megas `1.7.8`, Zamega `1.8.1+1.8`, Moonlight
  `3.6.5` e Supplementaries `3.9.7`
- O Supplementaries deve permanecer em `3.9.7`: o servidor registra
  `supplementaries:cooperative_pistons`; a classe de registro existe nessa versão e foi removida
  das versões `3.9.8` e `3.9.9`
- O diagnóstico agora conserva até 256 KiB do log, reconhece a mensagem em Português e a forma
  `missing from local registry`, e ignora o falso namespace XML `log4j`
- O autorreparo passou a aceitar atualização de um projeto já conhecido somente quando o erro de
  conexão informa a versão exigida; sem versão explícita, ele não escolhe cegamente a versão mais
  nova. A substituição local fica vinculada à versão incorporada que substituiu para não sobrepor
  um catálogo futuro
- A instância de teste foi sincronizada com os 132 arquivos; versões antigas foram movidas para
  `.launcher-mods-quarantine-v1`, sem exclusão definitiva
- Teste real concluído no endereço principal: conexão aceita, dados do servidor sincronizados e
  `Loaded 518 advancements`; não houve nova tela de registros desconhecidos
- Permanece um aviso não bloqueante do script CraftTweaker do servidor: `itens_bloqueados.zs`
  referencia o item removido `mega_showdown:wishing_star_crystal`; a correção pertence ao script
  administrado pelo servidor, não ao conjunto de mods do cliente
- Autotestes de diagnóstico, reparo, descoberta, minimapa, JAR e `--backend-probe`: aprovados
- SHA-256 local do JAR `3.4.21`:
  `81854c5d3f7d45cf22bb9969621d118a14f204ee5bd58dc404a5f658fd7e5b11`
- A versão `3.4.21` foi publicada no GitHub pelo workflow `35469794960`; compilação, autotestes,
  criação da release e retenção foram aprovados
- JAR público baixado novamente e aprovado em `--self-test` e `--backend-probe`; manifesto com
  `Implementation-Version: 3.4.21` confirmado
- SHA-256 do JAR público `3.4.21`:
  `4c83ed3d992e8526ec6fa397355c19304c0df7707b23897e44903a2ee782418b`
- A `3.4.21` passou a ser a versão permanente; o instalador genérico foi copiado para essa release
  e a `3.4.11` foi removida. A `3.4.19` permanece como versão rotativa

## Aba Eventos — versão 3.4.20 local

- As abas `DESTAQUE` e `EVENTOS` ficam sobre o banner. Ao selecionar `EVENTOS`, apenas a área
  do banner é substituída; o painel do launcher e as funções do jogo permanecem iguais
- Há título, descrição, posição, controles de banner anterior/próximo e botão `ATUALIZAR`
- O catálogo `java-launcher-v3/src/main/resources/ui/events.json` será lido da branch `main`
  no GitHub ao abrir a aba ou atualizar; assim, novos eventos poderão aparecer sem novo JAR
- Imagens remotas são aceitas apenas de `events/*.png|jpg|jpeg` no repositório oficial; nomes,
  tamanhos, dimensões e SHA-256 são validados antes de exibir. O carregamento não bloqueia a UI
- O banner de teste reaproveita a arte atual e fica incorporado ao JAR como fallback se o
  GitHub não responder; o catálogo remoto foi incluído na `main` com a publicação da 3.4.21
- A janela real foi aberta no Linux e a aba Eventos foi selecionada; o banner, texto e controles
  apareceram corretamente. O símbolo de atualização inicialmente não estava na fonte e foi
  substituído pelo texto `ATUALIZAR`, validado na janela reaberta
- A limpeza automática existente removeu os JARs locais antigos 3.4.19 e 3.4.17 quando a nova
  versão foi aberta; a release pública da 3.4.19 continua disponível

## Banner centralizado — versão 3.4.19

- A arte panorâmica original é exibida inteira, proporcional e centralizada no cartão visual,
  evitando o corte das laterais que ocorria com o preenchimento pela altura
- Uma cópia escurecida da própria imagem ocupa o fundo do cartão, mantendo a área preenchida sem
  competir com o banner nítido no centro
- A imagem nítida recebe contorno suave, cantos arredondados e sombra discreta; o restante do
  visual e as funções da versão 3.4.18 foram preservados
- O autoteste verifica que o enquadramento cabe na área e mantém os centros alinhados

## Abrir diretório — versão 3.4.18

- O painel inclui o botão `ABRIR DIRETÓRIO` ao lado de `DESINSTALAR`, mantendo a altura da área
  de ações e o visual atual
- Abre a pasta da instância `~/.cobblemon_legacy` no Linux ou
  `%APPDATA%\.cobblemon_legacy` no Windows, onde ficam os mods e arquivos do jogo
- Cria a pasta se ainda não existir; tenta a integração padrão do Java com o gerenciador de
  arquivos e usa `xdg-open`/`gio` no Linux ou `explorer.exe` no Windows como alternativa
- O caminho Windows foi corrigido ao conferir o cálculo da biblioteca `eml-lib`; a verificação
  de segurança de `Desinstalar` usa a mesma localização para não recusar a instância real
- A abertura ocorre fora da thread da interface para não travar o launcher; falhas mostram o
  caminho exato da pasta ao usuário; as ações ficam temporariamente desabilitadas para evitar
  concorrência com `Desinstalar`

## Posição do minimapa — versão 3.4.17

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
  em `%APPDATA%` no Windows; tokens conhecidos são ocultados e o arquivo é privado no Linux
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
  `https://github.com/velhotoby/launcher/releases/download/v3.4.21/Cobblemon-Legacy-Launcher-Installer.exe`
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

- JAR `3.4.20` local: compilação, `--self-test` (incluindo manifesto/imagem de eventos) e
  `--backend-probe` em perfil temporário isolado aprovados
- Aba Eventos renderizada e aberta na janela real do launcher no Linux; o fallback local
  apareceu, a interface permaneceu responsiva e o botão `ATUALIZAR` ficou legível
- Testes de desempenho, diagnóstico, minimapa, reparo de servidor e descoberta de mods: aprovados
- Manifesto do JAR `3.4.20`: `Implementation-Version: 3.4.20` confirmado
- SHA-256 local do JAR `3.4.20`:
  `73db748745030ef6f924506ffe59f4bbcee1e5633f9ac1024ee59f718c013bd5`
- JAR 3.4.19 publicado, baixado novamente e aprovado no autoteste; SHA-256 público:
  `933038fc64002695609707e989e8cb77215852405fd0e60a3b3521b5b2224aec`
- Manifesto do JAR público: `Implementation-Version: 3.4.19` e classe principal confirmados
- Workflow de publicação `34777443468`: aprovado, incluindo todos os testes e a retenção
- Retenção após a publicação: versões rotativas `3.4.19` e `3.4.18`, além da permanente
  `3.4.11`; os assets `Cobblemon-Legacy-Launcher-3.4.11.jar` e
  `Cobblemon-Legacy-Launcher-Installer.exe` continuam disponíveis
- `3.4.19` local: compilação, `--self-test` e `--backend-probe` isolado aprovados
- Prévia do componente do banner renderizada sem abrir o launcher em `746 × 535` e inspecionada
  visualmente; a arte inteira, incluindo personagem e logotipo, aparece centralizada
- Testes de desempenho, diagnóstico, minimapa, reparo de servidor e descoberta de mods: aprovados
- Manifesto do JAR `3.4.19`: `Implementation-Version: 3.4.19` confirmado
- SHA-256 local do JAR `3.4.19`:
  `48552469c0b50e7524d875e8e6f417a9f2a3116cb820976364aca1378407b3fe`
- JAR 3.4.18 publicado, baixado novamente e aprovado no autoteste; SHA-256 público:
  `21ca5f1fbee5c9d389fca9501ef745b0f6fdc2f5ea913f739fc5d40add7ea605`
- Manifesto do JAR público: `Implementation-Version: 3.4.18` e classe principal confirmados
- Workflow de publicação `34774697163`: aprovado, incluindo todos os testes e a retenção
- Retenção após a publicação: versões rotativas `3.4.18` e `3.4.17`, além da permanente
  `3.4.11`; os assets `Cobblemon-Legacy-Launcher-3.4.11.jar` e
  `Cobblemon-Legacy-Launcher-Installer.exe` continuam disponíveis
- `java -jar dist/Cobblemon-Legacy-Launcher-3.4.18.jar --self-test`: aprovado para criação da
  pasta da instância em perfil temporário, localização em `%APPDATA%` no Windows e seleção dos
  comandos Linux/Windows, além dos autotestes anteriores
- `--backend-probe` do JAR `3.4.18` com perfil temporário isolado: aprovado
- Manifesto do JAR `3.4.18`: `Implementation-Version: 3.4.18` confirmado
- SHA-256 local do JAR `3.4.18`:
  `95a7f5c4fcde358dc26f0d6242ce042ab97e7add9fc87a67908c3c6b2034f65f`
- Testes de minimapa e desempenho existentes: aprovados; a abertura visual de uma pasta
  pelo botão ainda não foi testada manualmente no Windows
- JAR 3.4.17 publicado, baixado novamente e aprovado no autoteste e `--backend-probe` isolado;
  SHA-256 público: `44828a5ff79f614ff39a9d553730175e6e367c506e14c348bbac1a3d9f8a8f12`
- Manifesto do JAR público: `Implementation-Version: 3.4.17` e classe principal confirmadas
- Workflow de publicação `34738150556`: aprovado, incluindo o teste do minimapa e da limpeza
- Retenção após a publicação: versões `3.4.17`, `3.4.15` e a permanente `3.4.11`; os assets
  `Cobblemon-Legacy-Launcher-3.4.11.jar` e `Cobblemon-Legacy-Launcher-Installer.exe`
  permanecem no GitHub
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
