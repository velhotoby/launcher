const fsp = require('node:fs/promises');
const path = require('node:path');

const MAX_READ_BYTES = 512 * 1024;

function displayExitCode(code) {
  const numeric = Number(code);
  if (numeric === -1 || numeric === 4294967295) return '-1 (4294967295 no Windows)';
  return Number.isFinite(numeric) ? String(numeric) : 'desconhecido';
}

async function newestCurrentCrashReport(root, startedAt) {
  const directory = path.join(root, 'crash-reports');
  let entries;
  try { entries = await fsp.readdir(directory, { withFileTypes: true }); }
  catch (error) { if (error.code === 'ENOENT') return null; throw error; }
  const candidates = [];
  for (const entry of entries) {
    if (!entry.isFile() || !/^crash-.*\.txt$/i.test(entry.name)) continue;
    const filename = path.join(directory, entry.name);
    const stats = await fsp.stat(filename);
    if (stats.mtimeMs >= startedAt - 5000) candidates.push({ filename, mtimeMs: stats.mtimeMs });
  }
  candidates.sort((left, right) => right.mtimeMs - left.mtimeMs);
  return candidates[0]?.filename || null;
}

async function readTail(filename) {
  const handle = await fsp.open(filename, 'r');
  try {
    const stats = await handle.stat();
    const length = Math.min(stats.size, MAX_READ_BYTES);
    const buffer = Buffer.alloc(length);
    await handle.read(buffer, 0, length, Math.max(0, stats.size - length));
    return buffer.toString('utf8');
  } finally { await handle.close(); }
}

function firstMatch(text, expression) {
  return text.match(expression)?.[1]?.trim() || '';
}

function summarize(text) {
  if (/OutOfMemoryError|Java heap space|GC overhead limit exceeded/i.test(text)) {
    return 'memória insuficiente. Ative “PC Fraco”, feche outros programas e tente novamente';
  }
  if (/EXCEPTION_ACCESS_VIOLATION|Problematic frame:|A fatal error has been detected by the Java Runtime/i.test(text)) {
    return 'falha nativa de Java ou do driver de vídeo. Atualize o driver e use Restaurar';
  }
  const entrypoint = firstMatch(text, /Could not execute entrypoint[^\n]*provided by ['"]([^'"]+)/i);
  if (entrypoint) return `incompatibilidade ao carregar o mod ${entrypoint}. Use Restaurar para conferir os arquivos`;
  if (/cobblemonrizetweaks|com\.rize2knight/i.test(text)) {
    return 'CobblemonRIzeTweaks incompatível durante batalha. Use Restaurar para removê-lo';
  }
  const mixinMod = firstMatch(text, /Mixin(?: apply)? failed[^\n]*?(?:from mod|mod)\s+['"]?([a-z0-9_.-]+)/i);
  if (mixinMod) return `falha de compatibilidade no mod ${mixinMod}. Use Restaurar para conferir os arquivos`;
  const missingClass = firstMatch(text, /(?:NoClassDefFoundError|ClassNotFoundException):\s*([^\r\n]+)/i);
  if (missingClass) return `classe ausente ou mod incompatível (${missingClass.replaceAll('/', '.')}). Use Restaurar`;
  const missingMethod = firstMatch(text, /NoSuchMethodError:\s*([^\r\n]+)/i);
  if (missingMethod) return `versões incompatíveis entre mods (${missingMethod.slice(0, 90)}). Use Restaurar`;
  if (/ModResolutionException|Incompatible mod set|Some of your mods are incompatible/i.test(text)) {
    return 'conjunto de mods incompatível. Use Restaurar para sincronizar novamente';
  }
  return 'encerramento inesperado. Use Restaurar e, se continuar, envie o relatório indicado';
}

async function diagnoseCrash(root, code, startedAt = Date.now()) {
  let reportPath = await newestCurrentCrashReport(root, startedAt);
  if (!reportPath) {
    const latest = path.join(root, 'logs', 'latest.log');
    try {
      if ((await fsp.stat(latest)).mtimeMs >= startedAt - 5000) reportPath = latest;
    } catch (error) { if (error.code !== 'ENOENT') throw error; }
  }
  let summary = 'encerramento inesperado. Use Restaurar e tente novamente';
  if (reportPath) {
    try { summary = summarize(await readTail(reportPath)); }
    catch { /* O caminho ainda é útil mesmo se outro processo bloquear o arquivo. */ }
  }
  return {
    code: displayExitCode(code),
    reportPath,
    summary,
    message: `Minecraft fechou com o código ${displayExitCode(code)}: ${summary}.` +
      (reportPath ? ` Relatório: ${reportPath}` : '')
  };
}

module.exports = { diagnoseCrash, displayExitCode, summarize, newestCurrentCrashReport };
