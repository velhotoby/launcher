const fsp = require('fs/promises');
const path = require('path');
const nbt = require('prismarine-nbt');

function createServer(server) {
  return nbt.comp({
    name: nbt.string(server.name),
    ip: nbt.string(server.ip),
    hidden: nbt.byte(0),
    acceptTextures: nbt.byte(1)
  }).value;
}

async function ensureServer(gamePath, server) {
  const destination = path.join(gamePath, 'servers.dat');
  let root;
  try {
    const { parsed } = await nbt.parse(await fsp.readFile(destination), 'big');
    root = parsed;
  } catch (error) {
    if (error.code !== 'ENOENT') throw new Error(`Não foi possível ler servers.dat: ${error.message}`);
    root = nbt.comp({ servers: nbt.list(nbt.comp([])) });
  }

  const servers = root.value?.servers?.value?.value;
  if (!Array.isArray(servers)) throw new Error('O arquivo servers.dat possui formato inválido.');
  const existing = servers.find((item) => item.name?.value === server.name)
    || servers.find((item) => item.ip?.value === server.ip);
  if (existing) {
    existing.name = nbt.string(server.name);
    existing.ip = nbt.string(server.ip);
    for (let index = servers.length - 1; index >= 0; index -= 1) {
      const item = servers[index];
      if (item !== existing && (item.name?.value === server.name || item.ip?.value === server.ip)) {
        servers.splice(index, 1);
      }
    }
  } else {
    servers.push(createServer(server));
  }

  const temporaryFile = `${destination}.tmp`;
  await fsp.mkdir(gamePath, { recursive: true });
  await fsp.writeFile(temporaryFile, nbt.writeUncompressed(root, 'big'));
  await fsp.rm(destination, { force: true });
  await fsp.rename(temporaryFile, destination);
  return { added: !existing, total: servers.length };
}

module.exports = { ensureServer };
