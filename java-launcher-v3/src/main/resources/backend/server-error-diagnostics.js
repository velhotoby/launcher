const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');

// Registry sync failures can contain more than one thousand lines. Keep enough
// context to retain the first affected namespaces and the final disconnect reason.
const MAX_LOG_CHARS = 256 * 1024;
const MOD_MISMATCH_PATTERNS = [
  /mismatched mod set/i,
  /registry entr(?:y|ies).*unknown/i,
  /unknown.*registry entr(?:y|ies)/i,
  /registry entr(?:y|ies).*missing from local registry/i,
  /entradas? de registro.*desconhecid/i,
  /incompatibilidade entre os mods do cliente e servidor/i,
  /incompatible mod set/i,
  /mod resolution encountered an incompatible/i,
  /missing required mods?/i,
  /required mods?:.*@(?:>=|=)?\d/i,
  /requires version .+ of mod/i,
  /incomplete set of tags received from server/i,
  /failed to synchronize registries?/i
];
const CONNECTION_FAILURE_PATTERNS = [
  /client disconnected with reason:/i,
  /failed to connect to server/i,
  /connection refused/i,
  /connection timed out/i,
  /connection lost/i
];
const IGNORED_NAMESPACES = new Set([
  'and', 'authlib', 'brigadier', 'c', 'client', 'com', 'fabric', 'forge', 'http',
  'https', 'fabricloader', 'java', 'log4j', 'minecraft', 'more', 'net', 'neoforge', 'org', 'registry', 'server', 'the'
]);

function mismatchReason(text) {
  return MOD_MISMATCH_PATTERNS.find((pattern) => pattern.test(text))?.source || null;
}

function isConnectionFailure(text) {
  return CONNECTION_FAILURE_PATTERNS.some((pattern) => pattern.test(text));
}

function isJoinComplete(text) {
  return /loaded \d+ advancements|joined (?:the )?(?:game|world)|\[system\] \[chat\]/i.test(text);
}

function endpointKey(host, port) {
  return `${String(host).trim().toLowerCase()}:${Number(port) || 25565}`;
}

function parseConnectionTarget(line) {
  const match = String(line).match(/\bConnecting to ([a-z0-9.-]+),\s*(\d{1,5})\b/i);
  return match ? endpointKey(match[1], match[2]) : null;
}

function configuredServerKeys(servers) {
  return new Set((servers || []).map((server) => {
    const [host, port] = String(server.ip || '').split(':');
    return endpointKey(host, port);
  }));
}

function extractNamespaces(text) {
  const found = new Set();
  const add = (value) => {
    const normalized = String(value).toLowerCase();
    if (/^[a-z][a-z0-9_-]{1,63}$/.test(normalized) && !IGNORED_NAMESPACES.has(normalized)) {
      found.add(normalized);
    }
  };
  for (const match of text.matchAll(/\b([a-z][a-z0-9_-]{1,63}):[a-z0-9_./-]+\b/gi)) add(match[1]);
  const marker = text.search(/(?:namespaces? (?:may be )?related|namespaces? de entrada do registro podem estar relacionados)/i);
  if (marker >= 0) {
    for (const line of text.slice(marker).split(/\r?\n/).slice(1, 24)) {
      const cleaned = line.replace(/\[[^\]]+\]/g, ' ').replace(/[^a-z0-9_-]+/gi, ' ').trim();
      if (/^[a-z][a-z0-9_-]{1,63}$/i.test(cleaned)) add(cleaned);
    }
  }
  return [...found].slice(0, 16);
}

function extractModRequirements(text) {
  const found = new Map();
  const add = (id, name, version, rule = 'exact') => {
    const normalized = String(id).toLowerCase();
    const cleanVersion = String(version || '').replace(/[!.,;]+$/, '');
    if (!/^[a-z][a-z0-9_-]{1,63}$/.test(normalized)
        || !/^[0-9][a-z0-9.+_-]{0,63}$/i.test(cleanVersion)) return;
    found.set(normalized, {
      id: normalized,
      name: String(name || normalized).slice(0, 100),
      requiredVersion: cleanVersion,
      rule
    });
  };

  for (const match of text.matchAll(/requires? version\s+([0-9][a-z0-9.+_-]*)\s+(or later|or newer)?\s*of mod\s+['"]([^'"]+)['"]\s+\(([a-z][a-z0-9_-]{1,63})\)/gi)) {
    add(match[4], match[3], match[1], match[2] ? 'minimum' : 'exact');
  }
  for (const match of text.matchAll(/(?:missing|required)\s+(?:required\s+)?mod\s+['"]([^'"]+)['"]\s+\(([a-z][a-z0-9_-]{1,63})\)\s*(?:version\s+|v)?([0-9][a-z0-9.+_-]*)/gi)) {
    add(match[2], match[1], match[3]);
  }
  for (const match of text.matchAll(/\b([a-z][a-z0-9_-]{1,63})@(?:(>=|=))?([0-9][a-z0-9.+_-]*)\b/gi)) {
    add(match[1], match[1], match[3], match[2] === '>=' ? 'minimum' : 'exact');
  }
  return [...found.values()].slice(0, 16);
}

function analyzeConnectionFailure(log) {
  const text = String(log).slice(-MAX_LOG_CHARS);
  const reason = mismatchReason(text);
  const lines = text.split(/\r?\n/);
  const errorLine = lines.findIndex((line) => mismatchReason(line));
  const evidence = errorLine < 0 ? '' : lines.slice(errorLine).join('\n');
  const requirements = extractModRequirements(evidence);
  const namespaces = [...new Set([
    ...requirements.map((mod) => mod.id).filter((id) => !IGNORED_NAMESPACES.has(id)),
    ...extractNamespaces(evidence)
  ])].slice(0, 16);
  return {
    failure: Boolean(reason || requirements.length || isConnectionFailure(text)),
    modError: Boolean(reason || requirements.length),
    reason: reason || 'Falha de conexão sem mod identificado',
    requirements,
    namespaces,
    canRepair: Boolean((reason || requirements.length) && namespaces.length > 0)
  };
}

function redact(text) {
  return String(text)
    .replace(/(Bearer\s+)[a-z0-9._-]+/gi, '$1[oculto]')
    .replace(/((?:access|refresh|client)[_-]?token\s*[:=]\s*)[^\s,;]+/gi, '$1[oculto]');
}

async function saveConnectionDiagnostic(gamePath, server, log, analysis) {
  const directory = path.join(gamePath, 'logs', 'launcher-connection-errors');
  await fs.mkdir(directory, { recursive: true, mode: 0o700 });
  const timestamp = new Date().toISOString();
  const filename = `connection-${timestamp.replace(/[:.]/g, '-')}-${crypto.randomBytes(3).toString('hex')}.log`;
  const destination = path.join(directory, filename);
  const requirements = analysis.requirements.length
    ? analysis.requirements.map((mod) => `- ${mod.name} (${mod.id}): ${mod.rule === 'minimum' ? 'mínimo' : 'exata'} ${mod.requiredVersion}`)
    : ['- O erro não informou um nome e uma versão exatos.'];
  const contents = [
    'Cobblemon Legacy Launcher — diagnóstico de conexão',
    `Data: ${timestamp}`,
    `Servidor: ${server}`,
    `Tipo: ${analysis.modError ? 'incompatibilidade de mods' : 'falha de conexão'}`,
    `Motivo: ${analysis.reason}`,
    'Mods exigidos pelo erro:',
    ...requirements,
    `Namespaces encontrados: ${analysis.namespaces.join(', ') || 'nenhum'}`,
    '',
    '--- Log da tentativa de conexão ---',
    redact(String(log).slice(-MAX_LOG_CHARS)),
    ''
  ].join('\n');
  await fs.writeFile(destination, contents, { encoding: 'utf8', flag: 'wx', mode: 0o600 });
  return destination;
}

async function appendDiagnosticOutcome(filename, message) {
  if (!filename) return;
  await fs.appendFile(filename, `${new Date().toISOString()} — ${redact(message)}\n`, 'utf8');
}

async function selfTest() {
  const servers = configuredServerKeys([{ ip: 'enx-cirion-16.enx.host:10068' }]);
  if (!servers.has(parseConnectionTarget('[INFO]: Connecting to enx-cirion-16.enx.host, 10068'))
      || servers.has(parseConnectionTarget('[INFO]: Connecting to outro-servidor.com, 25565'))) {
    throw new Error('Falha ao restringir o diagnóstico ao servidor configurado.');
  }
  const registry = analyzeConnectionFailure('Received 4850 registry entries that are unknown to this client.\n'
    + 'The following registry entry namespaces may be related:\n\ncobblemonalphas\nfallingtrees\n');
  if (!registry.canRepair || !registry.namespaces.includes('cobblemonalphas')) {
    throw new Error('Falha ao extrair namespaces do erro de registro.');
  }
  const portugueseRegistry = analyzeConnectionFailure(
    '<log4j:Event logger="net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl">\n'
    + 'Registry entry (cobblesafari:hyperspace_crate) is missing from local registry (minecraft:block)\n'
    + '1057 entradas de registro recebidas desconhecidas pelo cliente. A causa é uma incompatibilidade entre os mods do cliente e servidor.\n'
    + 'Os seguintes namespaces de entrada do registro podem estar relacionados:\n\n'
    + 'cobblemon\ncobblemon_picnic\ncobblesafari\nmega_showdown\nsupplementaries\n');
  if (!portugueseRegistry.canRepair || !portugueseRegistry.namespaces.includes('cobblemon_picnic')
      || !portugueseRegistry.namespaces.includes('cobblesafari')
      || portugueseRegistry.namespaces.includes('log4j')) {
    throw new Error('Falha ao extrair namespaces do erro de registro em Português.');
  }
  const missing = analyzeConnectionFailure("Missing required mod 'Example Mod' (example_mod) version 1.2.3\n"
    + 'Required mods: second_mod@>=2.0.0');
  if (!missing.canRepair || missing.requirements.length !== 2
      || missing.requirements[0].requiredVersion !== '1.2.3'
      || missing.requirements[1].rule !== 'minimum') {
    throw new Error('Falha ao extrair nomes e versões exigidos pelo erro.');
  }
  const network = analyzeConnectionFailure('Client disconnected with reason: Connection timed out');
  if (!network.failure || network.modError || network.canRepair) {
    throw new Error('Erro de rede não pode disparar download de mod.');
  }
  const temporary = await fs.mkdtemp(path.join(os.tmpdir(), 'cobblemon-connection-test-'));
  try {
    const report = await saveConnectionDiagnostic(temporary, 'enx-cirion-16.enx.host:10068',
      'Missing required mod example_mod@1.2.3\naccessToken=segredo', missing);
    await appendDiagnosticOutcome(report, 'Mod identificado: Example Mod 1.2.3');
    const saved = await fs.readFile(report, 'utf8');
    if (!saved.includes('Example Mod (example_mod)') || !saved.includes('Mod identificado: Example Mod 1.2.3')
        || saved.includes('segredo')) throw new Error('Falha ao salvar o diagnóstico de conexão.');
  } finally {
    await fs.rm(temporary, { recursive: true, force: true });
  }
  process.stdout.write('SERVER-DIAGNOSTICS OK: conexão, mods, versões e log validados.\n');
}

if (require.main === module && process.argv.includes('--self-test')) {
  selfTest().catch((error) => {
    process.stderr.write(`${error.stack || error.message}\n`);
    process.exitCode = 1;
  });
}

module.exports = {
  MAX_LOG_CHARS,
  analyzeConnectionFailure,
  appendDiagnosticOutcome,
  configuredServerKeys,
  extractNamespaces,
  isConnectionFailure,
  isJoinComplete,
  mismatchReason,
  parseConnectionTarget,
  saveConnectionDiagnostic
};
