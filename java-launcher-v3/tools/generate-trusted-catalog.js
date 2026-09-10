const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');

const instanceMods = process.argv[2] || path.join(process.env.HOME, '.cobblemon_legacy', 'mods');
const output = process.argv[3] || path.join(__dirname, '..', 'src', 'main', 'resources', 'backend', 'trusted-mod-catalog.json');
const ftbArtifacts = new Map([
  ['ftb-library-fabric-2101.1.35.jar', ['FTB Library', 'ftb-library-fabric', '2101.1.35']],
  ['ftb-quests-fabric-2101.1.31.jar', ['FTB Quests', 'ftb-quests-fabric', '2101.1.31']],
  ['ftb-teams-fabric-2101.1.10.jar', ['FTB Teams', 'ftb-teams-fabric', '2101.1.10']]
]);

function hashFile(filename) {
  return crypto.createHash('sha512').update(fs.readFileSync(filename)).digest('hex');
}

async function main() {
  const filenames = (await fsp.readdir(instanceMods)).filter((name) => name.endsWith('.jar')).sort();
  const local = await Promise.all(filenames.map(async (filename) => {
    const absolute = path.join(instanceMods, filename);
    const stats = await fsp.stat(absolute);
    return { filename, sha512: hashFile(absolute), size: stats.size };
  }));

  const versions = {};
  for (let index = 0; index < local.length; index += 40) {
    const batch = local.slice(index, index + 40);
    const response = await fetch('https://api.modrinth.com/v2/version_files', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'User-Agent': 'CobblemonLegacyLauncher/3.2.0' },
      body: JSON.stringify({ hashes: batch.map((item) => item.sha512), algorithm: 'sha512' })
    });
    if (!response.ok) throw new Error(`Modrinth respondeu HTTP ${response.status}: ${await response.text()}`);
    Object.assign(versions, await response.json());
  }

  const mods = local.map((item) => {
    const version = versions[item.sha512];
    if (version) {
      const remote = version.files.find((file) => file.hashes?.sha512 === item.sha512);
      if (!remote) throw new Error(`Arquivo correspondente ausente na versão do Modrinth: ${item.filename}`);
      return {
        name: version.name,
        source: 'modrinth',
        projectId: version.project_id,
        versionId: version.id,
        versionNumber: version.version_number,
        filename: remote.filename,
        urls: [remote.url],
        sha512: item.sha512,
        size: remote.size
      };
    }

    const ftb = ftbArtifacts.get(item.filename);
    if (!ftb) throw new Error(`Sem fonte confiável cadastrada: ${item.filename}`);
    const [name, artifact, versionNumber] = ftb;
    return {
      name,
      source: 'ftb-maven',
      projectId: artifact,
      versionId: versionNumber,
      versionNumber,
      filename: item.filename,
      urls: [`https://maven.ftb.dev/releases/dev/ftb/mods/${artifact}/${versionNumber}/${item.filename}`],
      sha512: item.sha512,
      size: item.size
    };
  });

  const catalog = {
    schemaVersion: 1,
    packId: 'cobblemon-legacy',
    packVersion: '2026.09.07',
    minecraft: '1.21.1',
    loader: 'fabric',
    mods
  };
  await fsp.mkdir(path.dirname(output), { recursive: true });
  await fsp.writeFile(output, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8');
  console.log(`${mods.length} mods gravados em ${output}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
