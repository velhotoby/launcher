# Cobblemon Legacy Launcher 3.4.29

Versão Java multiplataforma do launcher da comunidade.

O visual 3.4.29 reproduz a identidade do site oficial: banner da comunidade, tipografia
Chakra Petch/Inter, cartões creme com contornos escuros e a paleta vermelho, azul e verde.
As funções e configurações da versão 3.4.6 foram preservadas.
O banner panorâmico aparece inteiro e centralizado sobre um fundo escurecido da mesma arte,
sem cortar as personagens e o logotipo nas laterais.

Três endereços ficam disponíveis na lista de servidores: o endereço principal
`enx-cirion-16.enx.host:10068` e os alternativos `cobblemonlegacy.com.br:10068`
e `cobblemonlegacy.com.br`.
Esta versão invalida automaticamente o cache de backend das versões anteriores, garantindo
que os três endereços sejam verificados e gravados em `servers.dat` a cada inicialização.

O login Microsoft pode ser cancelado pelo botão ou pelo fechamento da janela de código.
O processo de autenticação é interrompido imediatamente, o aviso é fechado e a interface
volta ao perfil local sem travar o launcher.
No Linux, a página de autenticação também possui abertura compatível com `xdg-open`, `gio`
e `sensible-browser`, além de um botão para repetir a abertura ou copiar o endereço.

- Minecraft 1.21.1 + Fabric 0.19.5
- sincronização por manifesto confiável, sem Google Drive
- idioma do jogo fixado em Português (Brasil), sem apagar as demais opções
- minimapa do Xaero posicionado no lado direito antes de abrir o Minecraft; as outras opções
  em `config/xaerohud.txt` são preservadas. A limpeza interna também preserva a pasta `config/`
  e os dados de mapa em `xaero/`, evitando que o jogo recrie a posição padrão à esquerda
- perfil local e login original Microsoft/Minecraft por código de dispositivo, usando MinecraftAuth 5.0.2
- opção `Lembrar de mim?` para guardar apenas o nickname local em
  `~/.cobblemon_legacy_launcher/local-nickname.txt` no Linux ou
  `%USERPROFILE%\.cobblemon_legacy_launcher\local-nickname.txt` no Windows;
  desmarcar a opção apaga esse arquivo
- servidor `cubblemon legacy` pré-configurado
- botão `Abrir diretório` para mostrar a pasta da instância do Minecraft no gerenciador de
  arquivos (`~/.cobblemon_legacy` no Linux ou `%APPDATA%\.cobblemon_legacy` no Windows);
  cria a pasta caso ainda não exista
- núcleo Node incorporado ao JAR; no Windows ele é instalado automaticamente na pasta do usuário

## Desempenho automático

A versão 3.4.29 detecta a memória física e a quantidade de processadores lógicos antes de iniciar
o Minecraft. Com esses dados, seleciona um perfil econômico, equilibrado ou de alto desempenho e
define uma quantidade segura de RAM para a JVM (de 2 a 6 GB, conforme o computador).

O checkbox discreto `PC Fraco` força o perfil econômico quando selecionado, mantendo a memória
calculada de acordo com a RAM disponível. Na primeira execução do perfil, o launcher ajusta
distância de renderização e simulação, entidades,
partículas, gráficos, mistura de biomas, mipmaps, FPS, VSync, sombras e iluminação ambiente. Outras
preferências e os atalhos do jogador são preservados. A configuração só é reaplicada se o perfil
detectado mudar ou se o arquivo `options.txt` precisar ser recriado.

Para diagnóstico, `COBBLEMON_PERFORMANCE_PROFILE=low`, `balanced` ou `high` substitui temporariamente
a seleção automática.

Ao iniciar, o launcher consulta a publicação mais recente no GitHub. Se houver atualização,
ele oferece as opções de baixar/reiniciar automaticamente ou continuar na versão instalada.
A versão baixada abre em um processo independente no Linux e no Windows. Depois que a nova
janela inicia, ela valida e exclui somente o JAR anterior do Cobblemon Legacy Launcher; arquivos
que não correspondam a uma versão válida e mais antiga nunca entram nessa limpeza.

Compile com `./build.sh`. O resultado é salvo em `../dist/Cobblemon-Legacy-Launcher-3.4.29.jar`.

## Eventos

A aba `EVENTOS` ocupa o espaço do banner principal quando selecionada. Ela mostra banners com
título, descrição, navegação anterior/próximo e botão de atualização. O banner de teste usa a
arte atual do launcher. Se o GitHub estiver indisponível, o banner de teste incluído no JAR
continua visível.

O catálogo remoto fica em `java-launcher-v3/src/main/resources/ui/events.json` neste repositório. Para publicar
um evento futuro sem atualizar o launcher, adicione uma imagem PNG/JPG em
`java-launcher-v3/src/main/resources/ui/events/`, inclua-a no catálogo com título, descrição e SHA-256, e envie
os arquivos para a branch `main`. O launcher consulta esse catálogo ao abrir a aba e ao clicar
em atualizar. Ele aceita somente imagens desse caminho no GitHub, limita tamanho/dimensões e
confere o SHA-256 antes de exibir.

## Sincronização de mods

A versão 3.4.29 usa `trusted-mod-catalog.json` e não acessa o Google Drive. Os arquivos são
baixados apenas de hosts HTTPS aprovados (Modrinth CDN, CurseForge CDN e Maven oficial da FTB),
sempre com validação de tamanho e SHA-512.

Antes de abrir o jogo, a versão 3.4.29 também cria e ativa o pacote local
`Cobblemon Legacy Compat`. Ele corrige referências de modelos, texturas e JSONs defeituosos
encontrados nos mods oficiais sem modificar os JARs verificados nem alterar o conjunto exigido
pelo servidor.
O mesmo preparo garante que os pacotes opcionais compatíveis de Fabric, Cobblemon, Smartphone,
Mega Showdown, Reborn Core e Minecraft já estejam selecionados, inclusive quando o jogador
ainda não possui `options.txt`.
O pacote `Darker Ropes` também é ativado; como seu metadado oficial ainda declara um formato
antigo, o launcher registra previamente a confirmação exigida pelo Minecraft para evitar o aviso
manual em cada perfil.

Para atualizações automáticas do conjunto do servidor, publique o catálogo completo em HTTPS e
preencha `trustedSync.remoteManifestUrl`. Também é possível definir `COBBLEMON_MANIFEST_URL` no
ambiente. O catálogo remoto é consultado a cada inicialização e pode adicionar, atualizar ou remover
mods; arquivos excedentes são movidos para uma pasta de quarentena recuperável.

O modpack básico continua sendo verificado antes de abrir o jogo. A **descoberta de mods novos**
só começa depois de uma falha ao entrar em um dos três servidores configurados. O launcher salva
o trecho do log em `logs/launcher-connection-errors` dentro da instância, registra o nome e a
versão exigidos quando o erro os informa e procura no Modrinth uma versão Fabric 1.21.1 que
atenda à exigência. Antes de aceitar o resultado, ele confere o ID exato no `fabric.mod.json`,
o tamanho, o SHA-512 e o host HTTPS do download. Quando instala um novo mod, registra o nome e
a versão encontrados no mesmo log e reinicia o Minecraft automaticamente.

Erros de rede, erros de outros servidores e falhas sem ID de mod verificável apenas geram um
diagnóstico: não provocam download. Quando o servidor informa só um namespace, a versão
registrada é a da opção compatível encontrada no Modrinth, **não** uma versão confirmada pelo
servidor. Se o mod já estiver no catálogo com uma versão diferente, o manifesto do modpack
precisa ser atualizado; o launcher não substitui esse arquivo por aproximação.

O launcher requer Java 17 ou superior para abrir. O Minecraft 1.21.1 usa o Java 21 gerenciado pelo núcleo.
