const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {
  PACK_DIRECTORY,
  PACK_ID,
  ensureCompatibilityPack,
  updateOptions
} = require('../src/main/resources/backend/resource-pack-compat');

const updated = updateOptions([
  'lang:pt_br',
  'resourcePacks:["fabric","example"]',
  `incompatibleResourcePacks:["${PACK_ID}","appleskin","file/Meu Pack Antigo"]`,
  ''
].join('\n'));
assert.match(updated, new RegExp(`resourcePacks:.*${PACK_DIRECTORY}`));
assert.match(updated, /incompatibleResourcePacks:\["file\/Meu Pack Antigo"\]/);
assert.doesNotMatch(updated, /appleskin/);
assert.equal((updated.match(new RegExp(PACK_DIRECTORY, 'g')) || []).length, 1,
  'O pacote deve aparecer uma única vez nas opções.');
assert.equal(updateOptions(updated), updated, 'A atualização das opções deve ser idempotente.');

const instance = fs.mkdtempSync(path.join(os.tmpdir(), 'cobblemon-resource-pack-test-'));
(async () => {
  try {
    fs.writeFileSync(path.join(instance, 'options.txt'), 'lang:pt_br\nresourcePacks:["fabric"]\n');
    const first = await ensureCompatibilityPack(instance);
    assert.equal(first.changed, true);
    assert.equal(JSON.parse(fs.readFileSync(path.join(first.packRoot, 'pack.mcmeta'), 'utf8'))
      .pack.pack_format, 34);
    assert.equal(JSON.parse(fs.readFileSync(path.join(first.packRoot,
      'assets/minecraft/models/track_arrow.json'), 'utf8')).parent, 'minecraft:item/generated');
    assert.match(fs.readFileSync(path.join(instance, 'options.txt'), 'utf8'),
      new RegExp(`resourcePacks:.*${PACK_DIRECTORY}`));
    const second = await ensureCompatibilityPack(instance);
    assert.equal(second.changed, false, 'A segunda execução não deve regravar arquivos corretos.');
    console.log('RESOURCE PACK COMPAT OK: formato 34, ativação, escrita atômica e idempotência.');
  } finally {
    fs.rmSync(instance, { recursive: true, force: true });
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
