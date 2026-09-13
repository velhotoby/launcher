const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { ensureMinimapOnRight, positionMinimapOnRight } = require('../src/main/resources/backend/xaero-minimap');

const minimapLine = 'module;id=xaerominimap:minimap;x=16;y=24;centered=true;fromRight=false;fromBottom=true;flippedVer=false;flippedHor=false;';
const otherLine = 'module;id=other:widget;x=30;y=5;fromRight=false;';

const changed = positionMinimapOnRight(`${otherLine}\r\n${minimapLine}\r\n`);
assert.equal(changed.split('\r\n')[0], otherLine);
assert.equal(changed.split('\r\n')[1],
  'module;id=xaerominimap:minimap;x=0;y=24;centered=false;fromRight=true;fromBottom=true;flippedVer=false;flippedHor=false;');
assert.equal(positionMinimapOnRight(changed), changed, 'A configuração deve ser idempotente.');
assert.match(positionMinimapOnRight(''), /id=xaerominimap:minimap;x=0;y=0;centered=false;fromRight=true;/);

const instance = fs.mkdtempSync(path.join(os.tmpdir(), 'cobblemon-minimap-test-'));
try {
  const config = path.join(instance, 'config', 'xaerohud.txt');
  assert.equal(ensureMinimapOnRight(instance).changed, true, 'Cria a configuração na primeira execução.');
  assert.equal(ensureMinimapOnRight(instance).changed, false, 'Não regrava um arquivo já correto.');
  fs.writeFileSync(config, `${otherLine}\n${minimapLine}\n`, 'utf8');
  assert.equal(ensureMinimapOnRight(instance).changed, true, 'Corrige uma configuração existente.');
  const result = fs.readFileSync(config, 'utf8');
  assert.match(result, /fromRight=true/);
  assert.ok(result.startsWith(`${otherLine}\n`), 'Preserva os outros módulos.');
  assert.equal(ensureMinimapOnRight(instance).changed, false);
} finally {
  fs.rmSync(instance, { recursive: true, force: true });
}

console.log('XAERO MINIMAP OK: canto direito, criação, preservação e idempotência.');
