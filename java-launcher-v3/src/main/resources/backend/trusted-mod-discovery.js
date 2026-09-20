const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const yauzl = require('yauzl');
const { downloadMod, validateCatalog, validateFilename } = require('./trusted-mod-sync');

const MODRINTH_API = 'https://api.modrinth.com/v2';
const DISCOVERED_FILENAME = '.launcher-discovered-mods-v1.json';
const USER_AGENT = 'CobblemonLegacyLauncher/3.4.23';

async function apiJson(url) {
  const parsed = new URL(url);
  if (parsed.protocol !== 'https:' || parsed.hostname !== 'api.modrinth.com') {
    throw new Error('Consulta de descoberta fora da API confiável do Modrinth.');
  }
  const response = await fetch(parsed, { headers: { 'User-Agent': USER_AGENT, 'Cache-Control': 'no-cache' } });
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error(`API do Modrinth respondeu HTTP ${response.status}.`);
  }
  return response.json();
}

function readFabricIds(filename) {
  return new Promise((resolve, reject) => {
    yauzl.open(filename, { lazyEntries: true }, (openError, zip) => {
      if (openError) return reject(openError);
      let settled = false;
      const finish = (error, ids = []) => {
        if (settled) return;
        settled = true;
        zip.close();
        error ? reject(error) : resolve(ids);
      };
      zip.on('error', finish);
      zip.on('end', () => finish(null, []));
      zip.on('entry', (entry) => {
        if (entry.fileName !== 'fabric.mod.json') return zip.readEntry();
        if (entry.uncompressedSize > 1024 * 1024) return finish(new Error('fabric.mod.json muito grande.'));
        zip.openReadStream(entry, (streamError, stream) => {
          if (streamError) return finish(streamError);
          const chunks = [];
          let size = 0;
          stream.on('data', (chunk) => {
            size += chunk.length;
            if (size <= 1024 * 1024) chunks.push(chunk);
          });
          stream.on('error', finish);
          stream.on('end', () => {
            try {
              if (size > 1024 * 1024) throw new Error('fabric.mod.json muito grande.');
              const metadata = JSON.parse(Buffer.concat(chunks).toString('utf8'));
              const entries = Array.isArray(metadata) ? metadata : [metadata];
              const ids = new Set();
              for (const item of entries) {
                if (typeof item?.id === 'string') ids.add(item.id.toLowerCase());
                if (Array.isArray(item?.provides)) {
                  for (const provided of item.provides) if (typeof provided === 'string') ids.add(provided.toLowerCase());
                }
              }
              finish(null, [...ids]);
            } catch (error) { finish(error); }
          });
        });
      });
      zip.readEntry();
    });
  });
}

function descriptor(project, version) {
  const file = version.files.find((candidate) => candidate.primary) || version.files[0];
  if (!file || !validateFilename(file.filename) || !/^[a-f0-9]{128}$/i.test(file.hashes?.sha512 || '')
      || !Number.isSafeInteger(file.size)) return null;
  return {
    name: project.title || project.name || project.slug || version.name,
    source: 'modrinth',
    projectId: project.id || version.project_id,
    versionId: version.id,
    versionNumber: version.version_number,
    filename: file.filename,
    urls: [file.url],
    size: file.size,
    sha512: file.hashes.sha512
  };
}

async function compatibleVersions(projectId) {
  const loaders = encodeURIComponent(JSON.stringify(['fabric']));
  const versions = encodeURIComponent(JSON.stringify(['1.21.1']));
  return await apiJson(`${MODRINTH_API}/project/${encodeURIComponent(projectId)}/version?loaders=${loaders}&game_versions=${versions}`) || [];
}

function versionMatches(versionNumber, requirement) {
  if (!requirement.requiredVersion) return true;
  if (requirement.rule !== 'minimum') return versionNumber === requirement.requiredVersion;
  const numeric = (value) => {
    const match = String(value).match(/^(\d+(?:\.\d+){0,3})(?:[+._-].*)?$/);
    return match ? match[1].split('.').map(Number) : null;
  };
  const current = numeric(versionNumber);
  const required = numeric(requirement.requiredVersion);
  if (!current || !required) return false;
  for (let index = 0; index < Math.max(current.length, required.length); index += 1) {
    const difference = (current[index] || 0) - (required[index] || 0);
    if (difference !== 0) return difference > 0;
  }
  return true;
}

async function candidatesFor(namespace) {
  const candidates = [];
  const exact = await apiJson(`${MODRINTH_API}/project/${encodeURIComponent(namespace)}`);
  if (exact?.project_type === 'mod') candidates.push(exact);

  const facets = encodeURIComponent(JSON.stringify([
    ['project_type:mod'], ['categories:fabric'], ['versions:1.21.1']
  ]));
  const search = await apiJson(`${MODRINTH_API}/search?query=${encodeURIComponent(namespace)}&facets=${facets}&limit=10`);
  for (const hit of search?.hits || []) {
    if (!candidates.some((item) => item.id === hit.project_id)) {
      candidates.push({ ...hit, id: hit.project_id, title: hit.title });
    }
  }
  return candidates.slice(0, 10);
}

async function loadDiscovered(gamePath) {
  try {
    return validateCatalog(JSON.parse(await fsp.readFile(path.join(gamePath, DISCOVERED_FILENAME), 'utf8'))).mods;
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw new Error(`Catálogo de mods descobertos inválido: ${error.message}`);
  }
}

async function discoverMissingMods(requirements, namespaces, gamePath, configuration = {}, notify = () => {}) {
  const requested = new Map();
  const allowedNamespaces = new Set(Array.isArray(namespaces) ? namespaces : []);
  for (const requirement of Array.isArray(requirements) ? requirements : []) {
    if (requirement && allowedNamespaces.has(requirement.id)
        && /^[a-z][a-z0-9_-]{1,63}$/.test(requirement.id)) {
      requested.set(requirement.id, requirement);
    }
  }
  for (const namespace of Array.isArray(namespaces) ? namespaces : []) {
    if (/^[a-z][a-z0-9_-]{1,63}$/.test(namespace) && !requested.has(namespace)) {
      requested.set(namespace, { id: namespace, name: namespace, requiredVersion: null, rule: 'unknown' });
    }
  }
  if (configuration.allowModrinthDiscovery === false || requested.size === 0) {
    return { addedCount: 0, namespaces: [], resolved: [] };
  }
  const embedded = require('./trusted-mod-catalog.json');
  const discovered = await loadDiscovered(gamePath);
  const mods = [...discovered];
  const knownProjects = new Set([...embedded.mods, ...mods].map((mod) => mod.projectId).filter(Boolean));
  const knownByProject = new Map([...embedded.mods, ...mods]
    .filter((mod) => mod.projectId).map((mod) => [mod.projectId, mod]));
  const knownFiles = new Set([...embedded.mods, ...mods].map((mod) => mod.filename));
  const temporaryDirectory = path.join(gamePath, '.launcher-mod-discovery-v1');
  await fsp.rm(temporaryDirectory, { recursive: true, force: true });
  await fsp.mkdir(temporaryDirectory, { recursive: true });
  const resolvedNamespaces = [];
  const resolved = [];
  let changedCount = 0;

  function storeDiscovered(mod, previous) {
    const stored = previous ? {
      ...mod,
      // This prevents a locally discovered update from overriding a newer
      // catalog embedded in a future launcher release.
      replacesVersionId: previous.replacesVersionId || previous.versionId
    } : mod;
    const index = mods.findIndex((item) => item.projectId && item.projectId === stored.projectId);
    if (index >= 0) mods[index] = stored;
    else mods.push(stored);
    if (previous?.filename) knownFiles.delete(previous.filename);
    knownFiles.add(stored.filename);
    knownProjects.add(stored.projectId);
    knownByProject.set(stored.projectId, stored);
    changedCount += 1;
    return stored;
  }

  async function addDependencies(version, seen = new Set()) {
    for (const dependency of version.dependencies || []) {
      if (dependency.dependency_type !== 'required' || !dependency.project_id || knownProjects.has(dependency.project_id)
          || seen.has(dependency.project_id)) continue;
      seen.add(dependency.project_id);
      const project = await apiJson(`${MODRINTH_API}/project/${encodeURIComponent(dependency.project_id)}`);
      let dependencyVersion = dependency.version_id
        ? await apiJson(`${MODRINTH_API}/version/${encodeURIComponent(dependency.version_id)}`) : null;
      if (!dependencyVersion) dependencyVersion = (await compatibleVersions(dependency.project_id))[0];
      const mod = project && dependencyVersion ? descriptor(project, dependencyVersion) : null;
      if (!mod || knownFiles.has(mod.filename)) continue;
      mods.push(mod);
      knownProjects.add(mod.projectId);
      knownFiles.add(mod.filename);
      await addDependencies(dependencyVersion, seen);
    }
  }

  try {
    for (const requirement of [...requested.values()].slice(0, 16)) {
      const namespace = requirement.id;
      notify({ type: 'status', message: `Procurando ${requirement.name}` +
        (requirement.requiredVersion ? ` ${requirement.requiredVersion}` : '') + ' no Modrinth...' });
      let matched = false;
      for (const project of await candidatesFor(namespace)) {
        const previous = knownByProject.get(project.id);
        if (previous && !requirement.requiredVersion) {
          notify({ type: 'status', message: `${namespace} já consta no modpack; o servidor não informou a versão exigida.` });
          continue;
        }
        const versions = (await compatibleVersions(project.id)).filter((version) =>
          versionMatches(version.version_number, requirement)
          && (requirement.requiredVersion || version.version_type === 'release'));
        for (const version of versions.slice(0, 4)) {
          const mod = descriptor(project, version);
          if (!mod || mod.versionId === previous?.versionId || (!previous && knownFiles.has(mod.filename))) continue;
          const probe = path.join(temporaryDirectory, mod.filename);
          let ids;
          try {
            await downloadMod(mod, probe, () => {});
            ids = await readFabricIds(probe);
          } catch (_error) {
            await fsp.rm(probe, { force: true });
            continue;
          }
          await fsp.rm(probe, { force: true });
          if (!ids.includes(namespace)) continue;

          storeDiscovered(mod, previous);
          resolvedNamespaces.push(namespace);
          resolved.push({
            id: namespace,
            name: mod.name,
            versionNumber: mod.versionNumber,
            requiredVersion: requirement.requiredVersion,
            source: mod.source,
            action: previous ? 'updated' : 'added'
          });
          await addDependencies(version);
          matched = true;
          notify({ type: 'status', message: previous
            ? `${namespace} atualizado com segurança para ${mod.name} ${mod.versionNumber}.`
            : `${namespace} identificado com segurança como ${mod.name} ${mod.versionNumber}.` });
          break;
        }
        if (matched) break;
      }
      if (!matched && requirement.requiredVersion) {
        notify({ type: 'status', message: `Nenhum arquivo Fabric 1.21.1 confirmado para ${namespace} ${requirement.requiredVersion}.` });
      }
    }

    if (changedCount > 0) {
      const catalog = validateCatalog({
        schemaVersion: 1,
        packId: 'cobblemon-legacy',
        packVersion: `discovered-${new Date().toISOString()}`,
        minecraft: '1.21.1',
        loader: 'fabric',
        mods
      });
      const destination = path.join(gamePath, DISCOVERED_FILENAME);
      await fsp.writeFile(`${destination}.tmp`, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8');
      await fsp.rename(`${destination}.tmp`, destination);
    }
    return { addedCount: changedCount, changedCount, namespaces: resolvedNamespaces, resolved };
  } finally {
    await fsp.rm(temporaryDirectory, { recursive: true, force: true });
  }
}

module.exports = { discoverMissingMods, readFabricIds, versionMatches };
