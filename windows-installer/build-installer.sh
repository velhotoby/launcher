#!/usr/bin/env bash
set -euo pipefail

INSTALLER_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$INSTALLER_DIR/.." && pwd)"
USER_DIRECTORY="$(getent passwd "$(id -u)" | cut -d: -f6)"
JAVAC_BIN="${JAVAC_BIN:-$USER_DIRECTORY/.cobblemon_legacy_launcher/build-jdk-21/bin/javac}"
JAR_BIN="${JAR_BIN:-$USER_DIRECTORY/.cobblemon_legacy_launcher/build-jdk-21/bin/jar}"
LAUNCHER_JAR="$PROJECT_DIR/dist/Cobblemon-Legacy-Launcher-3.4.32.jar"
BOOTSTRAP_CLASSES="$(mktemp -d)"
trap 'rm -rf "$BOOTSTRAP_CLASSES"' EXIT

if [[ ! -f "$INSTALLER_DIR/payload/runtime/bin/javaw.exe" ]]; then
  echo "Runtime Java 21 para Windows não encontrado em payload/runtime." >&2
  exit 1
fi
if [[ ! -x "$JAVAC_BIN" || ! -x "$JAR_BIN" ]]; then
  echo "JDK de compilação não encontrado. Defina JAVAC_BIN e JAR_BIN." >&2
  exit 1
fi
if [[ ! -f "$LAUNCHER_JAR" ]]; then
  echo "Compile primeiro o JAR 3.4.32 com java-launcher-v3/build.sh." >&2
  exit 1
fi

"$JAVAC_BIN" --release 17 -encoding UTF-8 -d "$BOOTSTRAP_CLASSES" \
  "$INSTALLER_DIR/src/WindowsBootstrap.java"
"$JAR_BIN" --create --file "$INSTALLER_DIR/payload/Cobblemon-Legacy-Bootstrap.jar" \
  --main-class com.cobblemonlegacy.windows.WindowsBootstrap -C "$BOOTSTRAP_CLASSES" .
cp "$LAUNCHER_JAR" "$INSTALLER_DIR/payload/Cobblemon-Legacy-Launcher-Windows.jar"

(
  cd "$INSTALLER_DIR/payload"
  zip -qrFS runtime-bundle.zip runtime
)

llvm-rc /fo "$INSTALLER_DIR/payload/windows-installer.res" \
  "$INSTALLER_DIR/src/windows-installer.rc"
clang --target=x86_64-windows-msvc -std=c11 -Oz -ffreestanding -fno-stack-protector \
  -c "$INSTALLER_DIR/src/windows-installer.c" \
  -o "$INSTALLER_DIR/payload/windows-installer.obj"
lld-link /subsystem:windows /entry:wWinMainCRTStartup /nodefaultlib /opt:ref \
  /machine:x64 /out:"$INSTALLER_DIR/dist/Cobblemon-Legacy-Launcher-Installer.exe" \
  "$INSTALLER_DIR/payload/windows-installer.obj" \
  "$INSTALLER_DIR/payload/windows-installer.res" \
  /usr/lib/wine/x86_64-windows/libkernel32.a \
  /usr/lib/wine/x86_64-windows/libuser32.a \
  /usr/lib/wine/x86_64-windows/libshell32.a \
  /usr/lib/wine/x86_64-windows/libole32.a \
  /usr/lib/wine/x86_64-windows/libcomctl32.a \
  /usr/lib/wine/x86_64-windows/liburlmon.a \
  /usr/lib/wine/x86_64-windows/libadvapi32.a

echo "$INSTALLER_DIR/dist/Cobblemon-Legacy-Launcher-Installer.exe"
