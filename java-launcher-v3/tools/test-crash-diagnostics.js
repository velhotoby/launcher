const assert = require('node:assert/strict');
const fsp = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {
  diagnoseCrash,
  displayExitCode,
  summarize
} = require('../src/main/resources/backend/crash-diagnostics');

(async () => {
  assert.equal(displayExitCode(-1), '-1 (4294967295 no Windows)');
  assert.equal(displayExitCode(4294967295), '-1 (4294967295 no Windows)');
  assert.match(summarize('java.lang.OutOfMemoryError: Java heap space'), /memória insuficiente/);
  assert.match(summarize("Could not execute entrypoint stage 'main', provided by 'supplementaries'"),
    /supplementaries/);
  assert.match(summarize('at com.rize2knight.gui.EffectivenessRenderer'), /CobblemonRIzeTweaks/);
  assert.match(summarize('java.lang.NoClassDefFoundError: exemplo\/Classe'), /exemplo.Classe/);

  const root = await fsp.mkdtemp(path.join(os.tmpdir(), 'cobblemon-crash-test-'));
  try {
    const reports = path.join(root, 'crash-reports');
    await fsp.mkdir(reports, { recursive: true });
    const startedAt = Date.now();
    const report = path.join(reports, 'crash-test-client.txt');
    await fsp.writeFile(report,
      "Description: Initializing game\nCould not execute entrypoint stage 'main' due to errors, provided by 'moonlight'\n");
    const result = await diagnoseCrash(root, -1, startedAt);
    assert.equal(result.reportPath, report);
    assert.match(result.message, /-1 \(4294967295 no Windows\)/);
    assert.match(result.message, /moonlight/);
  } finally {
    await fsp.rm(root, { recursive: true, force: true });
  }
  console.log('CRASH-DIAGNOSTICS OK: código Windows, memória, mods e relatório recente validados.');
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
