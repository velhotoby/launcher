const path = require('node:path');
const { spawn } = require('node:child_process');
const { Launcher } = require('eml-lib');

const MOD_MISMATCH_PATTERNS = [
  /mismatched mod set/i,
  /registry entr(?:y|ies).*unknown/i,
  /unknown.*registry entr(?:y|ies)/i,
  /incompatible mod set/i,
  /mod resolution encountered an incompatible/i,
  /missing required mods?/i,
  /requires version .+ of mod/i,
  /incomplete set of tags received from server/i,
  /failed to synchronize registries?/i
];

const IGNORED_NAMESPACES = new Set([
  'and', 'authlib', 'brigadier', 'c', 'client', 'com', 'fabric', 'forge', 'java',
  'minecraft', 'more', 'net', 'neoforge', 'org', 'registry', 'server', 'the'
]);

function mismatchReason(text) {
  return MOD_MISMATCH_PATTERNS.find((pattern) => pattern.test(text))?.source || null;
}

function extractNamespaces(text) {
  const found = new Set();
  const add = (value) => {
    const normalized = String(value).toLowerCase();
    if (/^[a-z][a-z0-9_-]{1,63}$/.test(normalized) && !IGNORED_NAMESPACES.has(normalized)) found.add(normalized);
  };
  for (const match of text.matchAll(/\b([a-z][a-z0-9_-]{1,63}):[a-z0-9_./-]+\b/gi)) add(match[1]);

  const marker = text.search(/namespaces? (?:may be )?related/i);
  if (marker >= 0) {
    const related = text.slice(marker).split(/\r?\n/).slice(1, 24);
    for (const line of related) {
      const cleaned = line.replace(/\[[^\]]+\]/g, ' ').replace(/[^a-z0-9_-]+/gi, ' ').trim();
      if (/^[a-z][a-z0-9_-]{1,63}$/i.test(cleaned)) add(cleaned);
    }
  }
  return [...found].slice(0, 16);
}

class AutoRepairLauncher extends Launcher {
  constructor(configuration, onRepairDetected = () => {}) {
    super(configuration);
    this.onRepairDetected_ = onRepairDetected;
    this.repairRequested = null;
  }

  resetRepairDetection() {
    this.repairRequested = null;
  }

  async run(javaPath, args) {
    this.launchArgs_ = args;
    return new Promise((resolve, reject) => {
      const minecraft = spawn(javaPath, args, { cwd: this.config.root, detached: false });
      let recentOutput = '';
      let forceTimer = null;
      let detectionTimer = null;
      let exited = false;

      const inspect = (data) => {
        const text = data.toString();
        this.emit('launch_data', text);
        recentOutput = `${recentOutput}${text}`.slice(-32768);
        const reason = mismatchReason(recentOutput);
        if (!reason || this.repairRequested || detectionTimer) return;

        // A lista de namespaces costuma chegar algumas linhas depois da mensagem principal.
        detectionTimer = setTimeout(() => {
          const excerpt = recentOutput.split(/\r?\n/).filter(Boolean).slice(-12).join(' ').slice(0, 1600);
          this.repairRequested = {
            reason,
            excerpt,
            namespaces: extractNamespaces(recentOutput),
            detectedAt: new Date().toISOString()
          };
          this.onRepairDetected_(this.repairRequested);
          minecraft.kill();
          forceTimer = setTimeout(() => {
            if (!exited) minecraft.kill('SIGKILL');
          }, 5000);
          forceTimer.unref();
        }, 750);
      };

      minecraft.stdout.on('data', inspect);
      minecraft.stderr.on('data', inspect);
      minecraft.on('error', reject);
      minecraft.on('exit', (code) => {
        exited = true;
        if (detectionTimer) clearTimeout(detectionTimer);
        if (forceTimer) clearTimeout(forceTimer);
        const exitCode = code ?? -1;
        this.emit('launch_close', exitCode);
        if (exitCode !== 0 && !this.repairRequested) {
          this.emit('launch_crash', {
            code: exitCode,
            date: new Date().toISOString(),
            javaPath,
            logsPath: path.join(this.config.root, 'logs', 'latest.log'),
            crashReportsDir: path.join(this.config.root, 'crash-reports')
          });
        }
        resolve();
      });
    });
  }
}

module.exports = { AutoRepairLauncher, mismatchReason, extractNamespaces };
