const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { AutoRepairLauncher } = require('../src/main/resources/backend/auto-repair-launcher');
const { versionMatches } = require('../src/main/resources/backend/trusted-mod-discovery');

async function simulate(gamePath, server, errorLine) {
  const diagnostics = [];
  const launcher = new AutoRepairLauncher({
    root: 'cobblemon-repair-self-test',
    minecraft: { version: '1.21.1', loader: { loader: 'fabric', version: '0.19.5' } }
  }, {
    servers: [{ ip: 'enx-cirion-16.enx.host:10068' }],
    onDiagnostic: (diagnostic) => diagnostics.push(diagnostic)
  });
  launcher.config.root = gamePath;
  const script = `console.log(${JSON.stringify(`[INFO]: Connecting to ${server}`)});`
    + `console.log(${JSON.stringify(errorLine)});setTimeout(()=>{},2500);`;
  await launcher.run(process.execPath, ['-e', script]);
  return { diagnostics, repair: launcher.repairRequested };
}

async function main() {
  const temporary = await fs.mkdtemp(path.join(os.tmpdir(), 'cobblemon-repair-test-'));
  try {
    const wrongServer = await simulate(temporary, 'other-server.example, 25565',
      'Missing required mod example_mod@1.2.3');
    assert.equal(wrongServer.diagnostics.length, 0);
    assert.equal(wrongServer.repair, null);

    const network = await simulate(temporary, 'enx-cirion-16.enx.host, 10068',
      'Client disconnected with reason: Connection timed out');
    assert.equal(network.diagnostics.length, 1);
    assert.equal(network.diagnostics[0].canRepair, false);
    assert.equal(network.repair, null);
    assert.ok(await fs.readFile(network.diagnostics[0].logPath, 'utf8'));

    const afterJoin = await simulate(temporary, 'enx-cirion-16.enx.host, 10068',
      'Loaded 349 advancements\nClient disconnected with reason: Desconectado');
    assert.equal(afterJoin.diagnostics.length, 0);
    assert.equal(afterJoin.repair, null);

    const missing = await simulate(temporary, 'enx-cirion-16.enx.host, 10068',
      'Missing required mod example_mod@1.2.3');
    assert.equal(missing.diagnostics.length, 1);
    assert.equal(missing.repair.requirements[0].requiredVersion, '1.2.3');
    assert.ok((await fs.readFile(missing.repair.logPath, 'utf8')).includes('example_mod'));

    const registry = await simulate(temporary, 'enx-cirion-16.enx.host, 10068',
      'Received 4850 registry entries that are unknown to this client.\n'
      + 'The following registry entry namespaces may be related:\n\ncobblemonalphas');
    assert.ok(registry.repair.namespaces.includes('cobblemonalphas'));

    assert.equal(versionMatches('1.2.3', { requiredVersion: '1.2.3', rule: 'exact' }), true);
    assert.equal(versionMatches('1.2.4', { requiredVersion: '1.2.3', rule: 'exact' }), false);
    assert.equal(versionMatches('1.2.4', { requiredVersion: '1.2.3', rule: 'minimum' }), true);
    assert.equal(versionMatches('1.2.2', { requiredVersion: '1.2.3', rule: 'minimum' }), false);
  } finally {
    await fs.rm(temporary, { recursive: true, force: true });
  }
  process.stdout.write('SERVER-REPAIR OK: só reage a erro de mod no servidor configurado e salva o log.\n');
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
