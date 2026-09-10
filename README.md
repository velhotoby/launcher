# Cobblemon Legacy Launcher

Launcher oficial multiplataforma da comunidade Cobblemon Legacy.

## Download

Baixe o JAR mais recente na página de [Releases](../../releases/latest). O launcher exige
Java 17 ou superior; o Java 21 usado pelo Minecraft é gerenciado automaticamente.

## Recursos

- Minecraft 1.21.1, Fabric 0.19.5 e Cobblemon 1.7.3
- login local e conta original Microsoft/Minecraft
- sincronização e reparo automático do modpack por fontes confiáveis
- idioma Português (Brasil) e atalhos padronizados
- três endereços do servidor configurados automaticamente
- atualização do próprio launcher pela publicação mais recente do GitHub
- suporte a Linux e Windows por um único arquivo JAR

## Atualizações

Em cada execução, o launcher consulta a publicação estável mais recente deste repositório.
Quando encontra uma versão superior, o jogador pode escolher entre atualizar automaticamente
e reiniciar ou continuar temporariamente na versão instalada.

Para publicar uma nova versão:

1. atualize `CURRENT_VERSION`, `Implementation-Version` e o nome do JAR;
2. faça commit e envie uma tag no formato `vX.Y.Z`;
3. o fluxo de publicação compilará o JAR e criará ou atualizará a Release correspondente.

## Compilação local

```bash
npm ci --omit=dev
bash java-launcher-v3/build.sh
```

O código Java e os detalhes técnicos estão em [java-launcher-v3](java-launcher-v3/README.md).
