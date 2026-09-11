const fs = require('node:fs');
const path = require('node:path');
const { CrackAuth } = require('eml-lib');
const config = require('./launcher-config.json');
const { AutoRepairLauncher } = require('./auto-repair-launcher');
const { syncTrustedMods } = require('./trusted-mod-sync');
const { discoverMissingMods } = require('./trusted-mod-discovery');
const { ensureServer } = require('./server-list');
const { installReliableFetch } = require('./fetch-retry');
const { ensureBundledMinecraftFiles } = require('./minecraft-fallback');
const { applyDefaultKeybinds } = require('./keybinds');
const { applyPerformanceProfile, detectPerformanceProfile } = require('./performance-profile');

const INSTANCE_ID = 'cobblemon-legacy';
const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,16}$/;
const IGNORED_PATHS = [
  'crash-reports/', 'logs/', 'resourcepacks/', 'resources/', 'saves/',
  'shaderpacks/', 'options.txt', 'optionsof.txt', 'mods/', 'servers.dat',
  '.launcher-drive-sync-v2.json', '.launcher-modrinth-sync-v1.json', '.launcher-cache-v2/',
  '.launcher-keybinds-v1.json', '.launcher-trusted-sync-v1.json',
  '.launcher-performance-v1.json',
  '.launcher-discovered-mods-v1.json', '.launcher-mods-quarantine-v1/',
  '.launcher-mods-staging-v1/', '.launcher-mod-discovery-v1/'
];

function emit(type, message, current = 0, total = 0) {
  const encoded = Buffer.from(String(message), 'utf8').toString('base64');
  process.stdout.write(`@@COBBLEMON\t${type}\t${current}\t${total}\t${encoded}\n`);
}

function loadAccount(mode, value) {
  if (mode === 'offline') {
    const username = String(value || '').trim();
    if (!USERNAME_PATTERN.test(username)) throw new Error('Use de 3 a 16 letras, números ou underscore.');
    return new CrackAuth().auth(username);
  }
  if (mode !== 'microsoft') throw new Error('Modo de login desconhecido.');
  const accountFile = path.resolve(String(value || ''));
  const account = JSON.parse(fs.readFileSync(accountFile, 'utf8'));
  for (const field of ['name', 'uuid', 'accessToken', 'clientToken']) {
    if (typeof account[field] !== 'string' || !account[field]) throw new Error(`Sessão Microsoft inválida: ${field}.`);
  }
  return {
    name: account.name,
    uuid: account.uuid,
    accessToken: account.accessToken,
    clientToken: account.clientToken,
    userProperties: account.userProperties || {},
    meta: { online: true, type: 'msa' }
  };
}

function forceBrazilianPortuguese(instanceRoot) {
  const options = path.join(instanceRoot, 'options.txt');
  let lines = [];
  try { lines = fs.readFileSync(options, 'utf8').split(/\r?\n/).filter(Boolean); } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  let languageFound = false;
  let versionFound = false;
  lines = lines.map((line) => {
    if (line.startsWith('lang:')) {
      languageFound = true;
      return 'lang:pt_br';
    }
    if (line.startsWith('version:')) {
      versionFound = true;
      return 'version:3955';
    }
    return line;
  });
  if (!versionFound) lines.unshift('version:3955');
  if (!languageFound) lines.push('lang:pt_br');
  fs.mkdirSync(instanceRoot, { recursive: true });
  const temporary = `${options}.launcher.tmp`;
  fs.writeFileSync(temporary, `${lines.join('\n')}\n`, 'utf8');
  fs.renameSync(temporary, options);
}

async function main() {
  const mode = String(process.argv[2] || 'offline');
  const account = loadAccount(mode, process.argv[3]);
  emit('status', mode === 'microsoft' ? `Conta Microsoft: ${account.name}` : `Perfil local: ${account.name}`);
  const performance = detectPerformanceProfile();
  emit('status', `PC detectado: ${Math.round(performance.detectedMemoryMB / 1024)} GB de RAM, ` +
    `${performance.logicalCpuCount} processadores lógicos. Perfil ${performance.label}, ` +
    `${performance.memory.max} MB para o Minecraft.`);

  const launcher = new AutoRepairLauncher({
    root: INSTANCE_ID,
    account,
    minecraft: {
      version: config.minecraft.version,
      loader: { loader: config.minecraft.loader.type, version: config.minecraft.loader.version }
    },
    cleaning: { ignored: IGNORED_PATHS },
    java: { args: performance.javaArgs },
    memory: performance.memory
  }, () => emit('status', 'Diferença de mods detectada. Fechando o jogo para reparar...'));

  launcher.on('launch_compute_download', () => emit('status', 'Calculando arquivos do Minecraft...'));
  launcher.on('launch_download', ({ total }) => emit('progress', `${total.amount} arquivo(s) do Minecraft para baixar.`, 0, total.size));
  launcher.on('download_progress', ({ downloaded, total }) => emit('progress', 'Baixando arquivos do Minecraft...', downloaded.size, total.size || total.amount));
  launcher.on('launch_install_loader', ({ loader }) => emit('status', `Instalando Fabric ${loader.version}...`));
  launcher.on('launch_extract_natives', () => emit('status', 'Preparando bibliotecas nativas...'));
  launcher.on('launch_check_java', () => emit('status', 'Verificando o Java 21...'));
  launcher.on('launch_launch', () => emit('running', 'Cobblemon iniciado em Português (Brasil). Boa aventura!'));
  launcher.on('launch_close', (code) => {
    if (launcher.repairRequested) emit('status', 'Minecraft fechado pelo autorreparo. Verificando mods...');
    else emit(code === 0 ? 'success' : 'error', code === 0
      ? 'Jogo encerrado normalmente.' : `O jogo encerrou com o código ${code ?? 'desconhecido'}.`);
  });
  launcher.on('launch_crash', ({ code }) => emit('error', `Minecraft encerrou com o código ${code}. Consulte os relatórios de crash.`));

  const report = ({ type = 'status', message, current = 0, total = 0 }) => emit(type, message, current, total);
  const restoreFetch = installReliableFetch(({ attempt, totalAttempts, hostname }) => {
    emit('status', `Conexão instável com ${hostname}. Tentando novamente (${attempt}/${totalAttempts})...`);
  });
  await syncTrustedMods(launcher.config.root, config.trustedSync, report);
  const configuredServers = Array.isArray(config.servers) ? config.servers : [config.server].filter(Boolean);
  let addedServers = 0;
  for (const configuredServer of configuredServers) {
    const result = await ensureServer(launcher.config.root, configuredServer);
    if (result.added) addedServers += 1;
  }
  emit('status', `${configuredServers.length} servidores configurados (${addedServers} novos).`);
  await ensureBundledMinecraftFiles(launcher.config.root);
  const keybinds = await applyDefaultKeybinds(launcher.config.root);
  if (keybinds.changed) emit('status', `${keybinds.count} atalhos padrão configurados.`);
  forceBrazilianPortuguese(launcher.config.root);
  emit('status', 'Idioma definido como Português (Brasil).');
  const performanceResult = await applyPerformanceProfile(launcher.config.root, performance);
  emit('status', performanceResult.changed
    ? `Minecraft otimizado para o perfil ${performance.label}: ${performanceResult.count} ajustes aplicados.`
    : `Perfil de desempenho ${performance.label} já está configurado.`);

  const maximumRepairs = config.autoRepair?.enabled === false ? 0 : Math.max(1, Number(config.autoRepair?.maxAttempts) || 1);
  try {
    for (let attempt = 0; ; attempt += 1) {
      launcher.resetRepairDetection();
      await launcher.launch();
      if (!launcher.repairRequested) break;
      if (attempt >= maximumRepairs) {
        throw new Error('O servidor ainda exige mods diferentes depois do autorreparo. O manifesto do servidor precisa ser atualizado.');
      }

      emit('status', `Autorreparo de mods (${attempt + 1}/${maximumRepairs})...`);
      const discovery = await discoverMissingMods(
        launcher.repairRequested.namespaces, launcher.config.root, config.trustedSync, report
      );
      if (discovery.addedCount > 0) {
        emit('status', `${discovery.addedCount} mod(s) identificado(s) em fonte confiável.`);
      }
      const repaired = await syncTrustedMods(launcher.config.root, config.trustedSync, report);
      if (repaired.changedCount === 0) {
        throw new Error('O servidor exige um mod que ainda não consta no manifesto confiável. Atualize o manifesto do servidor para permitir o download seguro.');
      }
      emit('status', `${repaired.changedCount} alteração(ões) aplicada(s). Reiniciando o Minecraft automaticamente...`);
    }
  } finally { restoreFetch(); }
}

main().catch((error) => {
  emit('error', error instanceof Error ? error.message : 'Erro desconhecido ao iniciar.');
  process.exitCode = 1;
});
