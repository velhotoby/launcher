const path = require('node:path');
const { spawn } = require('node:child_process');
const { Launcher } = require('eml-lib');
const {
  MAX_LOG_CHARS,
  analyzeConnectionFailure,
  configuredServerKeys,
  extractNamespaces,
  isConnectionFailure,
  isJoinComplete,
  mismatchReason,
  parseConnectionTarget,
  saveConnectionDiagnostic
} = require('./server-error-diagnostics');

class AutoRepairLauncher extends Launcher {
  constructor(configuration, { onRepairDetected = () => {}, onDiagnostic = () => {}, servers = [] } = {}) {
    super(configuration);
    this.onRepairDetected_ = onRepairDetected;
    this.onDiagnostic_ = onDiagnostic;
    this.serverKeys_ = configuredServerKeys(servers);
    this.repairRequested = null;
    this.connectionDiagnostic = null;
  }

  resetRepairDetection() {
    this.repairRequested = null;
    this.connectionDiagnostic = null;
  }

  async run(javaPath, args) {
    this.launchArgs_ = args;
    return new Promise((resolve, reject) => {
      const minecraft = spawn(javaPath, args, { cwd: this.config.root, detached: false });
      const pending = { stdout: '', stderr: '' };
      let connection = null;
      let forceTimer = null;
      let detectionTimer = null;
      let diagnosticPromise = null;
      let exited = false;

      const finalizeDiagnostic = () => {
        if (diagnosticPromise || !connection || connection.joined) return diagnosticPromise || Promise.resolve();
        const snapshot = connection;
        const analysis = analyzeConnectionFailure(snapshot.log);
        if (!analysis.failure) return Promise.resolve();
        diagnosticPromise = (async () => {
          let logPath;
          try {
            logPath = await saveConnectionDiagnostic(this.config.root, snapshot.server, snapshot.log, analysis);
          } catch (error) {
            this.connectionDiagnostic = { failure: true, error: `Não foi possível salvar o log: ${error.message}` };
            this.onDiagnostic_(this.connectionDiagnostic);
            return;
          }
          const report = { ...analysis, server: snapshot.server, logPath, detectedAt: new Date().toISOString() };
          this.connectionDiagnostic = report;
          this.onDiagnostic_(report);
          if (!analysis.canRepair || connection !== snapshot) return;
          this.repairRequested = report;
          this.onRepairDetected_(report);
          if (exited) return;
          minecraft.kill();
          forceTimer = setTimeout(() => {
            if (!exited) minecraft.kill('SIGKILL');
          }, 5000);
          forceTimer.unref();
        })();
        return diagnosticPromise;
      };

      const processLine = (line) => {
        const target = parseConnectionTarget(line);
        if (target) {
          connection = this.serverKeys_.has(target) ? { server: target, log: `${line}\n`, joined: false } : null;
          diagnosticPromise = null;
          if (detectionTimer) clearTimeout(detectionTimer);
          detectionTimer = null;
          return;
        }
        if (!connection || connection.joined || diagnosticPromise) return;
        connection.log = `${connection.log}${line}\n`.slice(-MAX_LOG_CHARS);
        if (isJoinComplete(line)) {
          connection.joined = true;
          if (detectionTimer) clearTimeout(detectionTimer);
          detectionTimer = null;
          return;
        }
        if ((isConnectionFailure(line) || mismatchReason(connection.log)) && !detectionTimer) {
          // O texto do erro e os namespaces podem chegar em linhas seguintes.
          detectionTimer = setTimeout(() => {
            detectionTimer = null;
            finalizeDiagnostic().catch((error) =>
              this.onDiagnostic_({ failure: true, error: `Falha ao analisar o log: ${error.message}` }));
          }, 1200);
        }
      };

      const inspect = (data, stream) => {
        const text = data.toString();
        this.emit('launch_data', text);
        const lines = `${pending[stream]}${text}`.split(/\r?\n/);
        pending[stream] = lines.pop().slice(-MAX_LOG_CHARS);
        for (const line of lines) processLine(line);
      };

      minecraft.stdout.on('data', (data) => inspect(data, 'stdout'));
      minecraft.stderr.on('data', (data) => inspect(data, 'stderr'));
      minecraft.on('error', reject);
      minecraft.on('exit', async (code) => {
        exited = true;
        if (detectionTimer) clearTimeout(detectionTimer);
        if (forceTimer) clearTimeout(forceTimer);
        for (const line of Object.values(pending)) if (line) processLine(line);
        if (detectionTimer) clearTimeout(detectionTimer);
        try { await finalizeDiagnostic(); }
        catch (error) { this.onDiagnostic_({ failure: true, error: `Falha ao analisar o log: ${error.message}` }); }
        const exitCode = code ?? -1;
        this.emit('launch_close', exitCode);
        if (exitCode !== 0 && !this.repairRequested && !this.connectionDiagnostic) {
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
