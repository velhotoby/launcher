#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
V3_DIR="$PROJECT_DIR/java-launcher-v3"
BUILD_DIR="$V3_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
BACKEND_DIR="$CLASSES_DIR/backend"
DIST_DIR="$PROJECT_DIR/dist"
JAVA_HOME_COBBLEMON="${JAVA_HOME_COBBLEMON:-${JAVA_HOME:-$HOME/.cobblemon_legacy_launcher/build-jdk-21}}"
JAVAC="$JAVA_HOME_COBBLEMON/bin/javac"
JAR="$JAVA_HOME_COBBLEMON/bin/jar"

if [[ ! -x "$JAVAC" || ! -x "$JAR" ]]; then
  echo "JDK 21 não encontrado em $JAVA_HOME_COBBLEMON" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$BACKEND_DIR/node_modules" "$DIST_DIR"

"$JAVAC" --release 17 -encoding UTF-8 -cp "$V3_DIR/lib/*" -d "$CLASSES_DIR" \
  "$V3_DIR"/src/main/java/com/cobblemonlegacy/v3/*.java

for dependency in "$V3_DIR"/lib/*.jar; do
  unzip -q -o "$dependency" -d "$CLASSES_DIR"
done
rm -f "$CLASSES_DIR"/META-INF/*.SF "$CLASSES_DIR"/META-INF/*.RSA "$CLASSES_DIR"/META-INF/*.DSA "$CLASSES_DIR"/META-INF/MANIFEST.MF

cp "$V3_DIR/src/main/resources/backend/v3-backend.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/auto-repair-launcher.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/server-error-diagnostics.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/trusted-mod-sync.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/trusted-mod-discovery.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/performance-profile.js" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/trusted-mod-catalog.json" "$BACKEND_DIR/"
cp "$V3_DIR/src/main/resources/backend/launcher-config.json" "$BACKEND_DIR/"
cp -a "$V3_DIR/src/main/resources/ui" "$CLASSES_DIR/"
for resource in fetch-retry.js keybinds.js minecraft-fallback.js server-list.js; do
  cp "$PROJECT_DIR/$resource" "$BACKEND_DIR/$resource"
done

modules=(
  eml-lib tslib yauzl yazl buffer-crc32 pend prismarine-nbt protodef lodash.reduce
  protodef-validator abort-controller event-target-shim buffer base64-js ieee754 events process
  uri-js punycode fast-deep-equal fast-json-stable-stringify
)
for module in "${modules[@]}"; do
  cp -a "$PROJECT_DIR/node_modules/$module" "$BACKEND_DIR/node_modules/$module"
done

cat > "$BUILD_DIR/MANIFEST.MF" <<'EOF'
Manifest-Version: 1.0
Main-Class: com.cobblemonlegacy.v3.LauncherApp
Implementation-Title: Cobblemon Legacy Launcher
Implementation-Version: 3.4.15
Created-By: Cobblemon Legacy Community

EOF

OUTPUT="$DIST_DIR/Cobblemon-Legacy-Launcher-3.4.15.jar"
"$JAR" --create --file "$OUTPUT" --manifest "$BUILD_DIR/MANIFEST.MF" -C "$CLASSES_DIR" .
echo "$OUTPUT"
