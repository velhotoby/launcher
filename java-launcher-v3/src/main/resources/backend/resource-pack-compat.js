const fsp = require('node:fs/promises');
const path = require('node:path');
const yauzl = require('yauzl');

const PACK_DIRECTORY = 'Cobblemon Legacy Compat';
const PACK_ID = `file/${PACK_DIRECTORY}`;
const REQUIRED_RESOURCE_PACK_IDS = [
  'vanilla',
  'fabric',
  'cobblemon:regionbiasforms',
  'cobblemon:gyaradosjump',
  '$polymer-resources',
  'moonlight:merged_pack',
  'cobblemon:uniqueshinyforms',
  'cobblemon_smartphone:oldsmartphone',
  'high_contrast',
  'programmer_art',
  'mega_showdown:gyaradosjumpingmega',
  'mega_showdown:regionbiasmsd',
  'reborncore:reborncore_darkmode',
  'supplementaries:darker_ropes'
];
const CONFIRMED_INCOMPATIBLE_PACK_IDS = ['supplementaries:darker_ropes'];
// São pacotes internos sempre ativados pelo Fabric. Mantê-los na lista de incompatíveis do
// options.txt faz o Minecraft emitir "Removed resource pack" em toda abertura, embora os mods
// os carreguem novamente logo depois.
const STALE_MOD_PACK_IDS = new Set([
  'appleskin', 'balm', 'bookshelf', 'botanypots', 'botanypotstiers', 'chatnotify',
  'cobbledgacha', 'cobblesafari', 'collective', 'crafttweaker', 'easy_npc',
  'easy_npc_config_ui', 'failskins_10_0', 'jade', 'mcwdoors', 'modernfix',
  'mr_extra_moveanimscobblemon', 'torchmaster', 'watut', 'waystones', 'xaerolib',
  'xaerominimap', 'xaeroworldmap', 'yet_another_config_lib_v3'
]);

const STATIC_FILES = {
  'pack.mcmeta': JSON.stringify({
    pack: {
      pack_format: 34,
      description: 'Correções de recursos do Cobblemon Legacy para Minecraft 1.21.1'
    }
  }, null, 2) + '\n',
  // O CobbleNav 2.3.3 referencia um parent sem namespace que não existe.
  'assets/minecraft/models/track_arrow.json': JSON.stringify({
    parent: 'minecraft:item/generated'
  }, null, 2) + '\n'
};

const JAR_PATCHES = [
  ...['bootstrap/dialogs/en_us_arrays.json', 'bootstrap/dialogs/pt_br_arrays.json',
    'dialogs/en_us_arrays.json', 'dialogs/pt_br_arrays.json'].map((relative) => ({
    jar: 'kantonpcs',
    source: `assets/kantonpcs/${relative}`,
    target: `assets/kantonpcs/${relative}`,
    patch: (value) => value
      .replace('pressing "M" or', 'pressing \\"M\\" or')
      .replace('pressionando "M"', 'pressionando \\"M\\"')
      .replace('""SLOWPOKE."",', '"\\"SLOWPOKE.\\"",')
  })),
  {
    jar: 'mcw-trapdoors',
    source: 'assets/mcwtrpdoors/lang/ko_kr.json',
    target: 'assets/mcwtrpdoors/lang/ko_kr.json',
    patch: (value) => value.replace(
      /("block\.mcwtrpdoors\.warped_tropical_trapdoor":"뒤틀린 열대지방 다락문")\r?\n/,
      '$1,\n')
  },
  {
    jar: 'mega_showdown',
    source: 'assets/cobblemon/lang/ko_kr.json',
    target: 'assets/cobblemon/lang/ko_kr.json',
    patch: (value) => value.replace(
      /("cobblemon\.move\.dragoncheer\.desc": "용의 북돋움으로 사기를 높여서 같은 편의 기술이 급소에 맞기 쉬워진다\. 드래곤타입이면 더욱더 사기가 북돋는다\.")\r?\n/,
      '$1,\n')
  },
  {
    jar: 'cobblemontents',
    source: 'assets/cobblemontents/models/block/cobblemon_tent.json',
    target: 'assets/cobblemontents/models/block/cobblemon_tent.json',
    patch: (value) => value.replace('simplytents:item/tent', 'cobblemontents:block/cobblemon_tent')
  },
  {
    jar: 'cobblemontents',
    source: 'assets/cobblemontents/models/item/cobblemon_tent.json',
    target: 'assets/cobblemontents/models/item/cobblemon_tent.json',
    patch: (value) => value.replace('simplytents:item/tent', 'cobblemontents:item/cobblemon_tent')
  },
  {
    jar: 'pokeblocks',
    source: 'assets/pokeblocks/models/block/gigantic_pokedoll_shiny_cubchoo_animated.json',
    target: 'assets/pokeblocks/models/block/gigantic_pokedoll_shiny_cubchoo_animated.json',
    patch: (value) => value.replace(
      'pokedoll_shiny_cubchoo_animated_texture', 'pokedoll_cubchoo_animated_shiny_texture')
  },
  ...[
    ['0152_chikorita', '0152_chicorita'],
    ['0171_lanturn', '0171_lantern'],
    ['0233_porygon2', '0233_porygon'],
    ['0251_celebi', '0251_Celebi']
  ].map(([model, texture]) => ({
    jar: 'cobblecardquest',
    source: `assets/tcgcobblemon/models/item/cards/${model}.json`,
    target: `assets/tcgcobblemon/models/item/cards/${model}.json`,
    patch: (value) => value.replaceAll(`tcgcobblemon:item/${model}`, `tcgcobblemon:item/${texture}`)
  }))
];

function openZip(filename) {
  return new Promise((resolve, reject) => {
    yauzl.open(filename, { lazyEntries: true }, (error, zip) => error ? reject(error) : resolve(zip));
  });
}

async function readZipEntry(filename, entryName) {
  const zip = await openZip(filename);
  return new Promise((resolve, reject) => {
    let settled = false;
    const finish = (error, value) => {
      if (settled) return;
      settled = true;
      try { zip.close(); } catch { /* nada a fazer */ }
      if (error) reject(error); else resolve(value);
    };
    zip.on('error', (error) => finish(error));
    zip.on('end', () => finish(new Error(`Recurso ausente em ${path.basename(filename)}: ${entryName}`)));
    zip.on('entry', (entry) => {
      if (entry.fileName !== entryName) return zip.readEntry();
      zip.openReadStream(entry, (error, stream) => {
        if (error) return finish(error);
        const chunks = [];
        stream.on('data', (chunk) => chunks.push(chunk));
        stream.on('error', (streamError) => finish(streamError));
        stream.on('end', () => finish(null, Buffer.concat(chunks)));
      });
    });
    zip.readEntry();
  });
}

async function writeIfChanged(filename, content) {
  const buffer = Buffer.isBuffer(content) ? content : Buffer.from(content, 'utf8');
  try {
    if ((await fsp.readFile(filename)).equals(buffer)) return false;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
  await fsp.mkdir(path.dirname(filename), { recursive: true });
  const temporary = `${filename}.launcher.tmp`;
  await fsp.writeFile(temporary, buffer);
  await fsp.rename(temporary, filename);
  return true;
}

function updateOptions(content) {
  const newline = content.includes('\r\n') ? '\r\n' : '\n';
  const lines = content ? content.split(/\r?\n/) : [];
  if (content.endsWith('\n')) lines.pop();
  let resourcePacksFound = false;
  let incompatibleFound = false;
  const updated = lines.map((line) => {
    if (line.startsWith('resourcePacks:')) {
      resourcePacksFound = true;
      let packs;
      try { packs = JSON.parse(line.slice('resourcePacks:'.length)); }
      catch { packs = ['fabric']; }
      if (!Array.isArray(packs)) packs = ['fabric'];
      const extras = packs.filter(
        (item) => item !== PACK_ID && !REQUIRED_RESOURCE_PACK_IDS.includes(item));
      packs = [...REQUIRED_RESOURCE_PACK_IDS, ...extras, PACK_ID];
      return `resourcePacks:${JSON.stringify(packs)}`;
    }
    if (line.startsWith('incompatibleResourcePacks:')) {
      incompatibleFound = true;
      let packs;
      try { packs = JSON.parse(line.slice('incompatibleResourcePacks:'.length)); }
      catch { packs = []; }
      if (!Array.isArray(packs)) packs = [];
      const confirmed = packs.filter(
        (item) => item !== PACK_ID && !STALE_MOD_PACK_IDS.has(item)
          && !CONFIRMED_INCOMPATIBLE_PACK_IDS.includes(item));
      confirmed.push(...CONFIRMED_INCOMPATIBLE_PACK_IDS);
      return `incompatibleResourcePacks:${JSON.stringify(confirmed)}`;
    }
    return line;
  });
  if (!resourcePacksFound) updated.push(
    `resourcePacks:${JSON.stringify([...REQUIRED_RESOURCE_PACK_IDS, PACK_ID])}`);
  if (!incompatibleFound) updated.push(
    `incompatibleResourcePacks:${JSON.stringify(CONFIRMED_INCOMPATIBLE_PACK_IDS)}`);
  return `${updated.join(newline)}${newline}`;
}

async function ensureCompatibilityPack(instanceRoot) {
  const root = path.resolve(instanceRoot);
  const packRoot = path.join(root, 'resourcepacks', PACK_DIRECTORY);
  let changedCount = 0;
  for (const [relative, content] of Object.entries(STATIC_FILES)) {
    if (await writeIfChanged(path.join(packRoot, relative), content)) changedCount += 1;
  }

  let modFiles = [];
  try { modFiles = await fsp.readdir(path.join(root, 'mods')); }
  catch (error) { if (error.code !== 'ENOENT') throw error; }
  for (const patch of JAR_PATCHES) {
    const filename = modFiles.find((name) => name.toLowerCase().includes(patch.jar) && name.endsWith('.jar'));
    if (!filename) continue;
    const source = await readZipEntry(path.join(root, 'mods', filename), patch.source);
    const corrected = patch.patch(source.toString('utf8'));
    try { JSON.parse(corrected); }
    catch (error) { throw new Error(`Correção inválida para ${patch.target}: ${error.message}`); }
    if (await writeIfChanged(path.join(packRoot, patch.target), `${corrected.trimEnd()}\n`)) changedCount += 1;
  }

  const optionsPath = path.join(root, 'options.txt');
  let original = '';
  try { original = await fsp.readFile(optionsPath, 'utf8'); }
  catch (error) { if (error.code !== 'ENOENT') throw error; }
  const options = updateOptions(original);
  if (options !== original && await writeIfChanged(optionsPath, options)) changedCount += 1;
  return { changed: changedCount > 0, changedCount, packRoot, packId: PACK_ID };
}

module.exports = {
  PACK_DIRECTORY,
  PACK_ID,
  REQUIRED_RESOURCE_PACK_IDS,
  CONFIRMED_INCOMPATIBLE_PACK_IDS,
  ensureCompatibilityPack,
  updateOptions
};
