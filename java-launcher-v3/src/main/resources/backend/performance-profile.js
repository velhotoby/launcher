const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');

const PROFILE_VERSION = 1;
const STATE_FILENAME = '.launcher-performance-v1.json';
const MIB = 1024 * 1024;

const SETTINGS = {
  low: {
    graphicsMode: '0',
    renderDistance: '6',
    simulationDistance: '4',
    entityDistanceScaling: '0.5',
    particles: '2',
    biomeBlendRadius: '0',
    mipmapLevels: '2',
    maxFps: '60',
    enableVsync: 'true',
    entityShadows: 'false',
    ao: 'false'
  },
  balanced: {
    graphicsMode: '1',
    renderDistance: '10',
    simulationDistance: '6',
    entityDistanceScaling: '0.75',
    particles: '1',
    biomeBlendRadius: '2',
    mipmapLevels: '3',
    maxFps: '90',
    enableVsync: 'true',
    entityShadows: 'true',
    ao: 'true'
  },
  high: {
    graphicsMode: '1',
    renderDistance: '12',
    simulationDistance: '8',
    entityDistanceScaling: '1.0',
    particles: '0',
    biomeBlendRadius: '4',
    mipmapLevels: '4',
    maxFps: '120',
    enableVsync: 'true',
    entityShadows: 'true',
    ao: 'true'
  }
};

const LABELS = {
  low: 'econômico',
  balanced: 'equilibrado',
  high: 'alto desempenho'
};

function memoryAllocation(totalMemoryMB) {
  if (totalMemoryMB <= 4096) return { min: 512, max: 2048 };
  if (totalMemoryMB <= 6144) return { min: 768, max: 2560 };
  if (totalMemoryMB <= 8192) return { min: 1024, max: 3584 };
  if (totalMemoryMB <= 12288) return { min: 1024, max: 4608 };
  if (totalMemoryMB <= 16384) return { min: 1024, max: 5632 };
  return { min: 1024, max: 6144 };
}

function selectPerformanceProfile(totalMemoryMB, logicalCpuCount, override = '') {
  const memory = Math.max(1024, Math.floor(Number(totalMemoryMB) || 0));
  const cpus = Math.max(1, Math.floor(Number(logicalCpuCount) || 1));
  const requested = String(override || '').trim().toLowerCase();
  let id;
  if (['low', 'balanced', 'high'].includes(requested)) id = requested;
  else if (memory < 7168 || cpus <= 4) id = 'low';
  else if (memory < 14336 || cpus <= 8) id = 'balanced';
  else id = 'high';

  return {
    profileVersion: PROFILE_VERSION,
    id,
    label: LABELS[id],
    detectedMemoryMB: memory,
    logicalCpuCount: cpus,
    memory: memoryAllocation(memory),
    javaArgs: [
      '-XX:+UseG1GC',
      '-XX:+ParallelRefProcEnabled',
      '-XX:MaxGCPauseMillis=100',
      '-XX:+UseStringDeduplication',
      '-XX:+DisableExplicitGC'
    ],
    settings: SETTINGS[id]
  };
}

function detectPerformanceProfile() {
  return selectPerformanceProfile(
    Math.floor(os.totalmem() / MIB),
    Math.max(1, os.cpus()?.length || os.availableParallelism?.() || 1),
    process.env.COBBLEMON_PERFORMANCE_PROFILE
  );
}

async function readJson(filename) {
  try { return JSON.parse(await fs.readFile(filename, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT' || error instanceof SyntaxError) return null; throw error; }
}

async function applyPerformanceProfile(gamePath, profile) {
  const optionsPath = path.join(gamePath, 'options.txt');
  const statePath = path.join(gamePath, STATE_FILENAME);
  const savedState = await readJson(statePath);
  let optionsExist = true;
  try { await fs.access(optionsPath); }
  catch (error) { if (error.code === 'ENOENT') optionsExist = false; else throw error; }

  if (optionsExist && savedState?.profileVersion === PROFILE_VERSION && savedState?.profile === profile.id) {
    return { changed: false, count: 0, profile };
  }

  let lines;
  try { lines = (await fs.readFile(optionsPath, 'utf8')).split(/\r?\n/).filter(Boolean); }
  catch (error) { if (error.code !== 'ENOENT') throw error; lines = ['version:3955', 'lang:pt_br']; }

  const desired = new Map(Object.entries(profile.settings));
  const applied = new Set();
  const output = lines.map((line) => {
    const separator = line.indexOf(':');
    if (separator < 0) return line;
    const key = line.slice(0, separator);
    if (!desired.has(key)) return line;
    applied.add(key);
    return `${key}:${desired.get(key)}`;
  });
  for (const [key, value] of desired) if (!applied.has(key)) output.push(`${key}:${value}`);

  await fs.mkdir(gamePath, { recursive: true });
  const temporaryOptions = `${optionsPath}.performance.tmp`;
  await fs.writeFile(temporaryOptions, `${output.join('\n')}\n`, 'utf8');
  await fs.rename(temporaryOptions, optionsPath);

  const nextState = {
    profileVersion: PROFILE_VERSION,
    profile: profile.id,
    detectedMemoryMB: profile.detectedMemoryMB,
    logicalCpuCount: profile.logicalCpuCount,
    allocatedMemoryMB: profile.memory.max,
    appliedAt: new Date().toISOString()
  };
  const temporaryState = `${statePath}.tmp`;
  await fs.writeFile(temporaryState, `${JSON.stringify(nextState, null, 2)}\n`, 'utf8');
  await fs.rename(temporaryState, statePath);
  return { changed: true, count: desired.size, profile };
}

async function selfTest() {
  const low = selectPerformanceProfile(4096, 4);
  const balanced = selectPerformanceProfile(8192, 8);
  const high = selectPerformanceProfile(32768, 16);
  if (low.id !== 'low' || low.memory.max !== 2048 || low.settings.renderDistance !== '6') {
    throw new Error('Falha no perfil econômico.');
  }
  if (balanced.id !== 'balanced' || balanced.memory.max !== 3584 || balanced.settings.simulationDistance !== '6') {
    throw new Error('Falha no perfil equilibrado.');
  }
  if (high.id !== 'high' || high.memory.max !== 6144 || high.settings.simulationDistance !== '8') {
    throw new Error('Falha no perfil de alto desempenho.');
  }
  if (selectPerformanceProfile(4096, 2, 'high').id !== 'high') {
    throw new Error('Falha na substituição manual do perfil.');
  }
  const temporary = await fs.mkdtemp(path.join(os.tmpdir(), 'cobblemon-performance-test-'));
  try {
    await fs.writeFile(path.join(temporary, 'options.txt'),
      'version:3955\nlang:pt_br\nrenderDistance:20\nkey_key.forward:key.keyboard.w\n', 'utf8');
    const first = await applyPerformanceProfile(temporary, low);
    const options = await fs.readFile(path.join(temporary, 'options.txt'), 'utf8');
    const second = await applyPerformanceProfile(temporary, low);
    if (!first.changed || first.count !== Object.keys(low.settings).length
        || !options.includes('renderDistance:6')
        || !options.includes('simulationDistance:4')
        || !options.includes('key_key.forward:key.keyboard.w')
        || second.changed) {
      throw new Error('Falha ao aplicar ou preservar o perfil no options.txt.');
    }
  } finally {
    await fs.rm(temporary, { recursive: true, force: true });
  }
  process.stdout.write('PERFORMANCE-PROFILE OK: perfis e memória validados.\n');
}

if (require.main === module && process.argv.includes('--self-test')) {
  selfTest().catch((error) => {
    process.stderr.write(`${error.stack || error.message}\n`);
    process.exitCode = 1;
  });
}

module.exports = {
  PROFILE_VERSION,
  SETTINGS,
  applyPerformanceProfile,
  detectPerformanceProfile,
  memoryAllocation,
  selectPerformanceProfile
};
