const fs = require('node:fs');
const path = require('node:path');

const MINIMAP_ID = 'xaerominimap:minimap';
const DEFAULT_MODULE = `module;id=${MINIMAP_ID};x=0;y=0;centered=false;fromRight=true;fromBottom=false;flippedVer=false;flippedHor=false;`;

function positionMinimapOnRight(content) {
  const newline = content.includes('\r\n') ? '\r\n' : '\n';
  const lines = content ? content.split(/\r?\n/) : [];
  if (content.endsWith('\n')) lines.pop();
  let found = false;
  const updated = lines.map((line) => {
    const fields = line.split(';');
    if (fields[0] !== 'module' || !fields.includes(`id=${MINIMAP_ID}`)) return line;
    found = true;
    for (const [key, value] of [['x', '0'], ['centered', 'false'], ['fromRight', 'true']]) {
      const index = fields.findIndex((field) => field.startsWith(`${key}=`));
      if (index < 0) fields.splice(fields[fields.length - 1] === '' ? fields.length - 1 : fields.length, 0, `${key}=${value}`);
      else fields[index] = `${key}=${value}`;
    }
    return fields.join(';');
  });
  if (!found) updated.push(DEFAULT_MODULE);
  return `${updated.join(newline)}${newline}`;
}

function ensureMinimapOnRight(instanceRoot) {
  const config = path.join(instanceRoot, 'config', 'xaerohud.txt');
  let original = '';
  try { original = fs.readFileSync(config, 'utf8'); }
  catch (error) { if (error.code !== 'ENOENT') throw error; }
  const updated = positionMinimapOnRight(original);
  if (updated === original) return { changed: false, path: config };
  fs.mkdirSync(path.dirname(config), { recursive: true });
  const temporary = `${config}.launcher.tmp`;
  try {
    fs.writeFileSync(temporary, updated, 'utf8');
    fs.renameSync(temporary, config);
  } catch (error) {
    try { fs.unlinkSync(temporary); } catch { /* A falha original é mais importante. */ }
    throw error;
  }
  return { changed: true, path: config };
}

module.exports = { positionMinimapOnRight, ensureMinimapOnRight };
