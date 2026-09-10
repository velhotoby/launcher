const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const BUNDLED_FILES = [
  {
    name: 'client-1.12.xml',
    sha1: 'bd65e7d2e3c237be76cfbef4c2405033d7f91521',
    contents: 'PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPENvbmZpZ3VyYXRpb24gc3RhdHVzPSJXQVJOIj4KICAgIDxBcHBlbmRlcnM+CiAgICAgICAgPENvbnNvbGUgbmFtZT0iU3lzT3V0IiB0YXJnZXQ9IlNZU1RFTV9PVVQiPgogICAgICAgICAgICA8TGVnYWN5WE1MTGF5b3V0IC8+CiAgICAgICAgPC9Db25zb2xlPgogICAgICAgIDxSb2xsaW5nUmFuZG9tQWNjZXNzRmlsZSBuYW1lPSJGaWxlIiBmaWxlTmFtZT0ibG9ncy9sYXRlc3QubG9nIiBmaWxlUGF0dGVybj0ibG9ncy8lZHt5eXl5LU1NLWRkfS0laS5sb2cuZ3oiPgogICAgICAgICAgICA8UGF0dGVybkxheW91dCBwYXR0ZXJuPSJbJWR7SEg6bW06c3N9XSBbJXQvJWxldmVsXTogJW1zZ3tub2xvb2t1cHN9JW4iIC8+CiAgICAgICAgICAgIDxQb2xpY2llcz4KICAgICAgICAgICAgICAgIDxUaW1lQmFzZWRUcmlnZ2VyaW5nUG9saWN5IC8+CiAgICAgICAgICAgICAgICA8T25TdGFydHVwVHJpZ2dlcmluZ1BvbGljeSAvPgogICAgICAgICAgICA8L1BvbGljaWVzPgogICAgICAgIDwvUm9sbGluZ1JhbmRvbUFjY2Vzc0ZpbGU+CiAgICA8L0FwcGVuZGVycz4KICAgIDxMb2dnZXJzPgogICAgICAgIDxSb290IGxldmVsPSJpbmZvIj4KICAgICAgICAgICAgPGZpbHRlcnM+CiAgICAgICAgICAgICAgICA8TWFya2VyRmlsdGVyIG1hcmtlcj0iTkVUV09SS19QQUNLRVRTIiBvbk1hdGNoPSJERU5ZIiBvbk1pc21hdGNoPSJORVVUUkFMIiAvPgogICAgICAgICAgICA8L2ZpbHRlcnM+CiAgICAgICAgICAgIDxBcHBlbmRlclJlZiByZWY9IlN5c091dCIvPgogICAgICAgICAgICA8QXBwZW5kZXJSZWYgcmVmPSJGaWxlIi8+CiAgICAgICAgPC9Sb290PgogICAgPC9Mb2dnZXJzPgo8L0NvbmZpZ3VyYXRpb24+'
  }
];

function sha1(contents) {
  return crypto.createHash('sha1').update(contents).digest('hex');
}

async function ensureBundledMinecraftFiles(instanceRoot) {
  await fs.promises.mkdir(instanceRoot, { recursive: true });
  let installed = 0;

  for (const file of BUNDLED_FILES) {
    const targetPath = path.join(instanceRoot, file.name);
    const sourceContents = Buffer.from(file.contents, 'base64');
    if (sha1(sourceContents) !== file.sha1) {
      throw new Error(`O arquivo de fallback ${file.name} não passou na verificação de integridade.`);
    }

    try {
      const currentContents = await fs.promises.readFile(targetPath);
      if (sha1(currentContents) === file.sha1) continue;
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }

    const temporaryPath = `${targetPath}.launcher-part`;
    await fs.promises.writeFile(temporaryPath, sourceContents, { mode: 0o644 });
    await fs.promises.rename(temporaryPath, targetPath);
    installed += 1;
  }

  return installed;
}

module.exports = { ensureBundledMinecraftFiles };
