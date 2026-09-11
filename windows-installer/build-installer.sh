#!/usr/bin/env bash
set -euo pipefail

INSTALLER_DIR="$(cd "$(dirname "$0")" && pwd)"
INNO_COMPILER="$INSTALLER_DIR/tools/inno/ISCC.exe"
WINE_PREFIX="$INSTALLER_DIR/tools/wine-prefix"

if [[ ! -f "$INSTALLER_DIR/payload/runtime/bin/javaw.exe" ]]; then
  echo "Runtime Java 21 para Windows não encontrado em payload/runtime." >&2
  exit 1
fi
if [[ ! -f "$INNO_COMPILER" ]]; then
  echo "Inno Setup não encontrado em tools/inno." >&2
  exit 1
fi

llvm-rc /fo "$INSTALLER_DIR/payload/launcher.res" "$INSTALLER_DIR/src/launcher.rc"
clang --target=x86_64-windows-msvc -std=c11 -Oz -ffreestanding -fno-stack-protector \
  -c "$INSTALLER_DIR/src/launcher.c" -o "$INSTALLER_DIR/payload/launcher.obj"
lld-link /subsystem:windows /entry:wWinMainCRTStartup /nodefaultlib /opt:ref \
  /out:"$INSTALLER_DIR/payload/CobblemonLegacyLauncher.exe" \
  "$INSTALLER_DIR/payload/launcher.obj" "$INSTALLER_DIR/payload/launcher.res" \
  /usr/lib/wine/x86_64-windows/libkernel32.a \
  /usr/lib/wine/x86_64-windows/libuser32.a \
  /usr/lib/wine/x86_64-windows/libshell32.a

INNO_SCRIPT="$(WINEPREFIX="$WINE_PREFIX" winepath -w "$INSTALLER_DIR/installer.iss")"
WINEPREFIX="$WINE_PREFIX" WINEDEBUG=-all wine "$INNO_COMPILER" "$INNO_SCRIPT"

echo "$INSTALLER_DIR/dist/Cobblemon-Legacy-Launcher-Installer.exe"
