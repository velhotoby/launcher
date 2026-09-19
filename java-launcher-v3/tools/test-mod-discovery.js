const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const yazl = require('yazl');
const { discoverMissingMods } = require('../src/main/resources/backend/trusted-mod-discovery');
const { loadCatalog, validateFilename } = require('../src/main/resources/backend/trusted-mod-sync');

async function fabricJar(modId) {
  const zip = new yazl.ZipFile();
  zip.addBuffer(Buffer.from(JSON.stringify({ schemaVersion: 1, id: modId, version: '1.2.3' })), 'fabric.mod.json');
  zip.end();
  const chunks = [];
  for await (const chunk of zip.outputStream) chunks.push(chunk);
  return Buffer.concat(chunks);
}

async function main() {
  assert.equal(validateFilename('example-mod.jar'), true);
  assert.equal(validateFilename('../example-mod.jar'), false);
  assert.equal(validateFilename('..\\example-mod.jar'), false);
  const temporary = await fs.mkdtemp(path.join(os.tmpdir(), 'cobblemon-discovery-test-'));
  const originalFetch = global.fetch;
  try {
    const jar = await fabricJar('example_mod');
    const cobblemonUpdateJar = await fabricJar('cobblemon');
    const sha512 = crypto.createHash('sha512').update(jar).digest('hex');
    const cobblemonSha512 = crypto.createHash('sha512').update(cobblemonUpdateJar).digest('hex');
    const fileUrl = 'https://cdn.modrinth.com/data/TEST1234/versions/RIGHT123/example-mod-1.2.3.jar';
    const cobblemonUrl = 'https://cdn.modrinth.com/data/MdwFAVRL/versions/TESTUPDATE/cobblemon-test-update.jar';
    const versions = [
      { id: 'NEWER124', project_id: 'TEST1234', version_number: '1.2.4', files: [], dependencies: [] },
      {
        id: 'RIGHT123', project_id: 'TEST1234', version_number: '1.2.3', dependencies: [],
        files: [{ primary: true, filename: 'example-mod-1.2.3.jar', url: fileUrl,
          size: jar.length, hashes: { sha512 } }]
      }
    ];
    const fetched = [];
    global.fetch = async (input) => {
      const url = String(input);
      fetched.push(url);
      let response;
      if (url.endsWith('/project/example_mod')) {
        response = new Response(JSON.stringify({ id: 'TEST1234', title: 'Example Mod', project_type: 'mod' }));
      } else if (url.endsWith('/project/cobblemon')) {
        response = new Response(JSON.stringify({ id: 'MdwFAVRL', title: 'Cobblemon', project_type: 'mod' }));
      } else if (url.includes('/search?')) {
        response = new Response(JSON.stringify({ hits: [] }));
      } else if (url.includes('/project/TEST1234/version?')) {
        response = new Response(JSON.stringify(versions));
      } else if (url.includes('/project/MdwFAVRL/version?')) {
        response = new Response(JSON.stringify([{
          id: 'TESTUPDATE', project_id: 'MdwFAVRL', version_number: '9.9.9', version_type: 'release',
          dependencies: [], files: [{ primary: true, filename: 'cobblemon-test-update.jar',
            url: cobblemonUrl, size: cobblemonUpdateJar.length, hashes: { sha512: cobblemonSha512 } }]
        }]));
      } else if (url === fileUrl) {
        response = new Response(jar);
      } else if (url === cobblemonUrl) {
        response = new Response(cobblemonUpdateJar);
      } else {
        throw new Error(`Consulta inesperada: ${url}`);
      }
      Object.defineProperty(response, 'url', { value: url });
      return response;
    };

    const result = await discoverMissingMods(
      [{ id: 'example_mod', name: 'Example Mod', requiredVersion: '1.2.3', rule: 'exact' }],
      ['example_mod'], temporary
    );
    assert.equal(result.addedCount, 1);
    assert.equal(result.resolved[0].name, 'Example Mod');
    assert.equal(result.resolved[0].versionNumber, '1.2.3');
    assert.ok(fetched.includes(fileUrl));
    assert.ok(!fetched.some((url) => url.includes('NEWER124')));
    const saved = JSON.parse(await fs.readFile(path.join(temporary, '.launcher-discovered-mods-v1.json'), 'utf8'));
    assert.equal(saved.mods[0].sha512, sha512);

    const update = await discoverMissingMods([
      { id: 'cobblemon', name: 'Cobblemon', requiredVersion: '9.9.9', rule: 'exact' }
    ], ['cobblemon'], temporary);
    assert.equal(update.changedCount, 1);
    assert.equal(update.resolved[0].action, 'updated');
    const updatedCatalog = await loadCatalog({}, () => {}, temporary);
    const updatedCobblemon = updatedCatalog.mods.find((mod) => mod.projectId === 'MdwFAVRL');
    assert.equal(updatedCobblemon.versionId, 'TESTUPDATE');
    assert.equal(updatedCobblemon.replacesVersionId, 'gBW3vLC7');
  } finally {
    global.fetch = originalFetch;
    await fs.rm(temporary, { recursive: true, force: true });
  }
  process.stdout.write('MOD-DISCOVERY OK: ID, versão exata e SHA-512 verificados.\n');
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
