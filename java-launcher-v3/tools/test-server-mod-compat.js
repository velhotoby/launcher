const assert = require('node:assert/strict');
const catalog = require('../src/main/resources/backend/trusted-mod-catalog.json');
const { validateCatalog } = require('../src/main/resources/backend/trusted-mod-sync');

validateCatalog(catalog);

const byProject = new Map(catalog.mods.map((mod) => [mod.projectId, mod]));
const expected = new Map([
  ['Epm6R3P2', ['7.6.0', 'easy_npc-fabric-1.21.1-7.6.0.jar']],
  ['uTGjf7vA', ['7.6.0', 'easy_npc_config_ui-fabric-1.21.1-7.6.0.jar']],
  ['fFEIiSDQ', ['1.21.1-3.9.7', 'supplementaries-1.21.1-3.9.7-fabric.jar']]
]);

for (const [projectId, [versionNumber, filename]] of expected) {
  const mod = byProject.get(projectId);
  assert.ok(mod, `Projeto obrigatório ausente: ${projectId}`);
  assert.equal(mod.versionNumber, versionNumber,
    `${mod.name} deve acompanhar a versão instalada no servidor.`);
  assert.equal(mod.filename, filename);
}

assert.ok(!byProject.has('ON4VDdCA'),
  'Cobblemon RIze Tweaks não pode voltar ao catálogo: causa incompatibilidade em batalhas.');

console.log('SERVER MOD COMPAT OK: Easy NPC, Supplementaries e bloqueio do RIzeTweaks validados.');
