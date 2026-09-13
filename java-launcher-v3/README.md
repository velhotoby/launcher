# Cobblemon Legacy Launcher 3.4.14

Versão Java multiplataforma do launcher da comunidade.

O visual 3.4.14 reproduz a identidade do site oficial: banner da comunidade, tipografia
Chakra Petch/Inter, cartões creme com contornos escuros e a paleta vermelho, azul e verde.
As funções e configurações da versão 3.4.6 foram preservadas.

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
- perfil local e login original Microsoft/Minecraft por código de dispositivo, usando MinecraftAuth 5.0.2
- opção `Lembrar de mim?` para guardar apenas o nickname local em
  `~/.cobblemon_legacy_launcher/local-nickname.txt` no Linux ou
  `%USERPROFILE%\.cobblemon_legacy_launcher\local-nickname.txt` no Windows;
  desmarcar a opção apaga esse arquivo
- servidor `cubblemon legacy` pré-configurado
- núcleo Node incorporado ao JAR; no Windows ele é instalado automaticamente na pasta do usuário

## Desempenho automático

A versão 3.4.14 detecta a memória física e a quantidade de processadores lógicos antes de iniciar
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

Compile com `./build.sh`. O resultado é salvo em `../dist/Cobblemon-Legacy-Launcher-3.4.14.jar`.

## Sincronização de mods

A versão 3.4.14 usa `trusted-mod-catalog.json` e não acessa o Google Drive. Os arquivos são
baixados apenas de hosts HTTPS aprovados (Modrinth CDN, CurseForge CDN e Maven oficial da FTB),
sempre com validação de tamanho e SHA-512.

Para atualizações automáticas do conjunto do servidor, publique o catálogo completo em HTTPS e
preencha `trustedSync.remoteManifestUrl`. Também é possível definir `COBBLEMON_MANIFEST_URL` no
ambiente. O catálogo remoto é consultado a cada inicialização e pode adicionar, atualizar ou remover
mods; arquivos excedentes são movidos para uma pasta de quarentena recuperável.

Durante o jogo, o launcher acompanha a saída do Minecraft. Ao detectar incompatibilidade
do conjunto de mods durante uma conexão, ele fecha o Minecraft, consulta novamente o
manifesto e tenta identificar os namespaces ausentes no Modrinth. Um resultado automático
só é aceito quando o `fabric.mod.json` do JAR confirma exatamente o ID procurado; tamanho,
SHA-512 e host do download também são validados. Após aplicar os arquivos, o Minecraft é
reiniciado. Se não houver correspondência exata e confiável, o reinício é interrompido para
impedir downloads por aproximação.

O launcher requer Java 17 ou superior para abrir. O Minecraft 1.21.1 usa o Java 21 gerenciado pelo núcleo.
