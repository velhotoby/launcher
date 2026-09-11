const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const { Readable, Transform } = require('node:stream');
const { pipeline } = require('node:stream/promises');

const STATE_FILENAME = '.launcher-trusted-sync-v1.json';
const DISCOVERED_FILENAME = '.launcher-discovered-mods-v1.json';
const QUARANTINE_DIRECTORY = '.launcher-mods-quarantine-v1';
const MANIFEST_MAX_BYTES = 4 * 1024 * 1024;
const TRUSTED_DOWNLOAD_HOSTS = new Set([
  'cdn.modrinth.com',
  'maven.ftb.dev',
  'edge.forgecdn.net',
  'mediafilez.forgecdn.net',
  'media.forgecdn.net'
]);
const TRUSTED_MANIFEST_HOSTS = new Set([
  'cdn.modrinth.com',
  'raw.githubusercontent.com',
  'api.github.com'
]);

function validateFilename(filename) {
  return typeof filename === 'string' && filename.endsWith('.jar') && path.basename(filename) === filename;
}

function validateCatalog(catalog) {
  if (!catalog || catalog.schemaVersion !== 1 || catalog.packId !== 'cobblemon-legacy'
      || catalog.minecraft !== '1.21.1' || catalog.loader !== 'fabric' || !Array.isArray(catalog.mods)) {
    throw new Error('O manifesto remoto não pertence ao modpack Cobblemon Legacy 1.21.1 Fabric.');
  }
  if (catalog.mods.length === 0 || catalog.mods.length > 500) throw new Error('Quantidade de mods inválida no manifesto.');
  const filenames = new Set();
  for (const mod of catalog.mods) {
    if (!validateFilename(mod.filename) || filenames.has(mod.filename)) {
      throw new Error(`Nome de mod inválido ou duplicado: ${mod.filename}`);
    }
    filenames.add(mod.filename);
    if (typeof mod.name !== 'string' || !mod.name || !/^[a-f0-9]{128}$/i.test(mod.sha512)
        || !Number.isSafeInteger(mod.size) || mod.size <= 0 || mod.size > 1024 * 1024 * 1024
        || !Array.isArray(mod.urls) || mod.urls.length === 0) {
      throw new Error(`Metadados inválidos para ${mod.filename}.`);
    }
    for (const rawUrl of mod.urls) assertTrustedDownload(rawUrl, mod.filename);
  }
  return catalog;
}

function assertTrustedDownload(rawUrl, filename) {
  const url = new URL(rawUrl);
  if (url.protocol !== 'https:' || !TRUSTED_DOWNLOAD_HOSTS.has(url.hostname)) {
    throw new Error(`Fonte não autorizada para ${filename}: ${url.hostname}`);
  }
  return url;
}

async function sha512(filename) {
  const hash = crypto.createHash('sha512');
  await pipeline(fs.createReadStream(filename), new Transform({
    transform(chunk, _encoding, callback) { hash.update(chunk); callback(); }
  }));
  return hash.digest('hex');
}

async function isCurrent(filename, mod) {
  try {
    const stats = await fsp.stat(filename);
    return stats.isFile() && stats.size === mod.size && await sha512(filename) === mod.sha512;
  } catch (error) {
    if (error.code === 'ENOENT') return false;
    throw error;
  }
}

async function downloadMod(mod, destination, onProgress) {
  const temporary = `${destination}.part`;
  await fsp.rm(temporary, { force: true });
  let lastError;
  for (const rawUrl of mod.urls) {
    try {
      assertTrustedDownload(rawUrl, mod.filename);
      const response = await fetch(rawUrl, {
        redirect: 'follow', headers: { 'User-Agent': 'CobblemonLegacyLauncher/3.4.13' }
      });
      if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`);
      assertTrustedDownload(response.url, mod.filename);
      let downloaded = 0;
      const hash = crypto.createHash('sha512');
      const verifier = new Transform({
        transform(chunk, _encoding, callback) {
          downloaded += chunk.length;
          if (downloaded > mod.size) return callback(new Error(`Tamanho excedido em ${mod.filename}.`));
          hash.update(chunk);
          onProgress(downloaded);
          callback(null, chunk);
        }
      });
      await pipeline(Readable.fromWeb(response.body), verifier, fs.createWriteStream(temporary, { flags: 'wx' }));
      if (downloaded !== mod.size || hash.digest('hex') !== mod.sha512) {
        throw new Error(`A verificação SHA-512 falhou para ${mod.filename}.`);
      }
      await fsp.rename(temporary, destination);
      return;
    } catch (error) {
      lastError = error;
      await fsp.rm(temporary, { force: true });
    }
  }
  throw new Error(`Não foi possível baixar ${mod.name} de uma fonte confiável: ${lastError?.message || 'erro desconhecido'}`);
}

async function loadCatalog(configuration, notify, gamePath) {
  const embedded = validateCatalog(require('./trusted-mod-catalog.json'));
  const remoteValue = process.env.COBBLEMON_MANIFEST_URL || configuration.remoteManifestUrl;
  if (!remoteValue) {
    notify({ type: 'status', message: `Catálogo confiável incorporado: ${embedded.mods.length} mods.` });
    return mergeDiscovered(embedded, gamePath, notify);
  }
  const remoteUrl = new URL(remoteValue);
  const extraHosts = Array.isArray(configuration.trustedManifestHosts) ? configuration.trustedManifestHosts : [];
  if (remoteUrl.protocol !== 'https:' || !(TRUSTED_MANIFEST_HOSTS.has(remoteUrl.hostname) || extraHosts.includes(remoteUrl.hostname))) {
    throw new Error(`Servidor de manifesto não autorizado: ${remoteUrl.hostname}`);
  }
  notify({ type: 'status', message: 'Procurando atualizações do modpack no servidor...' });
  const response = await fetch(remoteUrl, { headers: {
    'User-Agent': 'CobblemonLegacyLauncher/3.4.13',
    'Cache-Control': 'no-cache'
  } });
  if (!response.ok) throw new Error(`O manifesto remoto respondeu HTTP ${response.status}.`);
  const declaredSize = Number(response.headers.get('content-length'));
  if (Number.isFinite(declaredSize) && declaredSize > MANIFEST_MAX_BYTES) throw new Error('Manifesto remoto muito grande.');
  const text = await response.text();
  if (Buffer.byteLength(text) > MANIFEST_MAX_BYTES) throw new Error('Manifesto remoto muito grande.');
  const remote = validateCatalog(JSON.parse(text));
  notify({ type: 'status', message: `Manifesto remoto ${remote.packVersion}: ${remote.mods.length} mods.` });
  return mergeDiscovered(remote, gamePath, notify);
}

async function mergeDiscovered(catalog, gamePath, notify) {
  let local;
  try { local = validateCatalog(JSON.parse(await fsp.readFile(path.join(gamePath, DISCOVERED_FILENAME), 'utf8'))); }
  catch (error) {
    if (error.code === 'ENOENT') return catalog;
    throw new Error(`Catálogo local de mods descobertos inválido: ${error.message}`);
  }
  const projectIds = new Set(catalog.mods.map((mod) => mod.projectId).filter(Boolean));
  const filenames = new Set(catalog.mods.map((mod) => mod.filename));
  const additions = local.mods.filter((mod) => !projectIds.has(mod.projectId) && !filenames.has(mod.filename));
  if (additions.length === 0) return catalog;
  notify({ type: 'status', message: `${additions.length} mod(s) descoberto(s) pelo autorreparo.` });
  return validateCatalog({ ...catalog, mods: [...catalog.mods, ...additions] });
}

async function quarantineUnexpected(modsPath, expectedNames, gamePath, notify) {
  const unexpected = (await fsp.readdir(modsPath, { withFileTypes: true }))
    .filter((entry) => entry.isFile() && entry.name.endsWith('.jar') && !expectedNames.has(entry.name));
  if (unexpected.length === 0) return 0;
  const quarantine = path.join(gamePath, QUARANTINE_DIRECTORY);
  await fsp.mkdir(quarantine, { recursive: true });
  for (const entry of unexpected) {
    let destination = path.join(quarantine, entry.name);
    if (await fsp.stat(destination).then(() => true, () => false)) {
      destination = path.join(quarantine, `${Date.now()}-${entry.name}`);
    }
    await fsp.rename(path.join(modsPath, entry.name), destination);
  }
  notify({ type: 'status', message: `${unexpected.length} mod(s) fora do manifesto movido(s) para quarentena.` });
  return unexpected.length;
}

async function syncTrustedMods(gamePath, configuration = {}, notify = () => {}) {
  const catalog = await loadCatalog(configuration, notify, gamePath);
  const modsPath = path.join(gamePath, 'mods');
  const staging = path.join(gamePath, '.launcher-mods-staging-v1');
  await fsp.mkdir(modsPath, { recursive: true });
  await fsp.rm(staging, { recursive: true, force: true });
  await fsp.mkdir(staging, { recursive: true });

  const expectedNames = new Set(catalog.mods.map((mod) => mod.filename));
  const total = catalog.mods.reduce((sum, mod) => sum + mod.size, 0);
  let completed = 0;
  let downloadedCount = 0;
  try {
    for (const mod of catalog.mods) {
      const destination = path.join(modsPath, mod.filename);
      if (await isCurrent(destination, mod)) {
        completed += mod.size;
        notify({ type: 'progress', message: `${mod.name} verificado.`, current: completed, total });
        continue;
      }
      const staged = path.join(staging, mod.filename);
      const sourceName = mod.source === 'modrinth' ? 'Modrinth'
        : mod.source === 'curseforge' ? 'CurseForge' : 'fonte oficial confiável';
      notify({ type: 'status', message: `Baixando ${mod.name} de ${sourceName}...` });
      await downloadMod(mod, staged, (current) => notify({
        type: 'progress', message: `Baixando ${mod.name}...`, current: completed + current, total
      }));
      completed += mod.size;
      downloadedCount += 1;
    }

    for (const mod of catalog.mods) {
      const staged = path.join(staging, mod.filename);
      if (await fsp.stat(staged).then(() => true, () => false)) {
        await fsp.rm(path.join(modsPath, mod.filename), { force: true });
        await fsp.rename(staged, path.join(modsPath, mod.filename));
      }
    }
    const quarantinedCount = await quarantineUnexpected(modsPath, expectedNames, gamePath, notify);
    const state = {
      schemaVersion: 1,
      packId: catalog.packId,
      packVersion: catalog.packVersion,
      checkedAt: new Date().toISOString(),
      files: catalog.mods.map(({ name, source, projectId, versionId, versionNumber, filename, sha512, size }) =>
        ({ name, source, projectId, versionId, versionNumber, filename, sha512, size }))
    };
    const statePath = path.join(gamePath, STATE_FILENAME);
    await fsp.writeFile(`${statePath}.tmp`, `${JSON.stringify(state, null, 2)}\n`, 'utf8');
    await fsp.rename(`${statePath}.tmp`, statePath);
    notify({ type: 'status', message: `${catalog.mods.length} mods sincronizados (${downloadedCount} baixados, ${quarantinedCount} em quarentena).` });
    return { ...state, downloadedCount, quarantinedCount, changedCount: downloadedCount + quarantinedCount };
  } finally {
    await fsp.rm(staging, { recursive: true, force: true });
  }
}

module.exports = { syncTrustedMods, validateCatalog, downloadMod, loadCatalog };
