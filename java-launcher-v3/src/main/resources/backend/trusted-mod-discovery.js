const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const yauzl = require('yauzl');
const { downloadMod, validateCatalog } = require('./trusted-mod-sync');

const MODRINTH_API = 'https://api.modrinth.com/v2';
const DISCOVERED_FILENAME = '.launcher-discovered-mods-v1.json';
const USER_AGENT = 'CobblemonLegacyLauncher/3.4.14';

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
  if (!file || !file.hashes?.sha512 || !Number.isSafeInteger(file.size)) return null;
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

async function discoverMissingMods(namespaces, gamePath, configuration = {}, notify = () => {}) {
  if (configuration.allowModrinthDiscovery === false || !Array.isArray(namespaces) || namespaces.length === 0) {
    return { addedCount: 0, namespaces: [] };
  }
  const embedded = require('./trusted-mod-catalog.json');
  const discovered = await loadDiscovered(gamePath);
  const mods = [...discovered];
  const knownProjects = new Set([...embedded.mods, ...mods].map((mod) => mod.projectId).filter(Boolean));
  const knownFiles = new Set([...embedded.mods, ...mods].map((mod) => mod.filename));
  const temporaryDirectory = path.join(gamePath, '.launcher-mod-discovery-v1');
  await fsp.rm(temporaryDirectory, { recursive: true, force: true });
  await fsp.mkdir(temporaryDirectory, { recursive: true });
  const resolvedNamespaces = [];

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
    for (const namespace of [...new Set(namespaces)].slice(0, 16)) {
      notify({ type: 'status', message: `Procurando o mod ${namespace} no Modrinth...` });
      let matched = false;
      for (const project of await candidatesFor(namespace)) {
        if (knownProjects.has(project.id)) continue;
        for (const version of (await compatibleVersions(project.id)).slice(0, 4)) {
          const mod = descriptor(project, version);
          if (!mod || knownFiles.has(mod.filename)) continue;
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

          mods.push(mod);
          knownProjects.add(mod.projectId);
          knownFiles.add(mod.filename);
          resolvedNamespaces.push(namespace);
          await addDependencies(version);
          matched = true;
          notify({ type: 'status', message: `${namespace} identificado com segurança como ${mod.name}.` });
          break;
        }
        if (matched) break;
      }
    }

    if (mods.length > discovered.length) {
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
    return { addedCount: mods.length - discovered.length, namespaces: resolvedNamespaces };
  } finally {
    await fsp.rm(temporaryDirectory, { recursive: true, force: true });
  }
}

module.exports = { discoverMissingMods, readFabricIds };
