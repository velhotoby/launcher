const dns = require('node:dns');

const PRIMARY_MOJANG_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const ALTERNATE_MOJANG_MANIFEST = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json';
const BMCLAPI_ROOT = 'https://bmclapi2.bangbang93.com';
const BMCLAPI_MANIFEST = `${BMCLAPI_ROOT}/mc/game/version_manifest_v2.json`;
const RETRIES_PER_ADDRESS = 2;

function wait(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function isRetryableStatus(status) {
  return status === 408 || status === 425 || status === 429 || status >= 500;
}

function requestUrl(input) {
  if (typeof input === 'string') return input;
  if (input instanceof URL) return input.toString();
  return input?.url || '';
}

function requestMethod(input, init) {
  return String(init?.method || input?.method || 'GET').toUpperCase();
}

function candidateUrls(url) {
  if (url === PRIMARY_MOJANG_MANIFEST) {
    return [PRIMARY_MOJANG_MANIFEST, ALTERNATE_MOJANG_MANIFEST, BMCLAPI_MANIFEST];
  }
  if (url === ALTERNATE_MOJANG_MANIFEST) {
    return [ALTERNATE_MOJANG_MANIFEST, PRIMARY_MOJANG_MANIFEST, BMCLAPI_MANIFEST];
  }
  try {
    const parsed = new URL(url);
    if (parsed.hostname === 'piston-meta.mojang.com' || parsed.hostname === 'launchermeta.mojang.com') {
      return [url, `${BMCLAPI_ROOT}${parsed.pathname}${parsed.search}`];
    }
  } catch (_error) {
    return [url];
  }
  return [url];
}

function createRetryingFetch(originalFetch, onRetry = () => {}) {
  return async function reliableFetch(input, init) {
    if (requestMethod(input, init) !== 'GET') return originalFetch(input, init);

    const initialUrl = requestUrl(input);
    if (!initialUrl.startsWith('https://')) return originalFetch(input, init);

    const urls = candidateUrls(initialUrl);
    const totalAttempts = urls.length * RETRIES_PER_ADDRESS;
    let attempt = 0;
    let lastError;

    for (const url of urls) {
      for (let localAttempt = 0; localAttempt < RETRIES_PER_ADDRESS; localAttempt += 1) {
        attempt += 1;
        if (attempt > 1) {
          onRetry({ attempt, totalAttempts, hostname: new URL(url).hostname });
          await wait(Math.min(2200, 600 * (attempt - 1)));
        }

        try {
          const requestInput = url === initialUrl ? input : url;
          const response = await originalFetch(requestInput, init);
          if (!isRetryableStatus(response.status) || attempt === totalAttempts) return response;
          await response.body?.cancel();
        } catch (error) {
          lastError = error;
          if (attempt === totalAttempts) throw error;
        }
      }
    }

    throw lastError || new Error('Falha de rede sem detalhes.');
  };
}

function installReliableFetch(onRetry) {
  dns.setDefaultResultOrder('ipv4first');
  const originalFetch = globalThis.fetch;
  const reliableFetch = createRetryingFetch(originalFetch.bind(globalThis), onRetry);
  globalThis.fetch = reliableFetch;

  return () => {
    if (globalThis.fetch === reliableFetch) globalThis.fetch = originalFetch;
  };
}

module.exports = { createRetryingFetch, installReliableFetch };
