// eml-lib remove arquivos não listados antes de iniciar o Minecraft.
// Esses caminhos guardam opções e dados criados pelo jogador e pelos mods.
module.exports = [
  'config/', 'crash-reports/', 'logs/', 'resourcepacks/', 'resources/', 'saves/',
  'screenshots/', 'shaderpacks/', 'xaero/', 'XaeroWaypoints/', 'XaeroWaypoints_BACKUP',
  'options.txt', 'optionsof.txt', 'mods/', 'servers.dat',
  '.launcher-drive-sync-v2.json', '.launcher-modrinth-sync-v1.json', '.launcher-cache-v2/',
  '.launcher-keybinds-v1.json', '.launcher-trusted-sync-v1.json',
  '.launcher-performance-v1.json',
  '.launcher-discovered-mods-v1.json', '.launcher-mods-quarantine-v1/',
  '.launcher-mods-staging-v1/', '.launcher-mod-discovery-v1/'
];
