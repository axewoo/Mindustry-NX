#!/usr/bin/env bash
# Build build/Mindustry/build-switch/Mindustry.nro with the SwitchGDX backend.
# Needs: JDK 17, git, unzip, rsync, cmake, ninja, python3, devkitPro (DEVKITPRO set) with
#   switch-dev switch-zlib switch-zziplib switch-sdl2 switch-sdl2_mixer switch-freetype
#   switch-glad switch-curl switch-mesa switch-libdrm_nouveau switch-libvorbis, plus libffi
#   (scripts/build-libffi.sh). Run scripts/setup-switch-deps.sh first.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

: "${DEVKITPRO:=/opt/devkitpro}"
export DEVKITPRO
JOBS="${JOBS:-$(nproc)}"
CLEARWING="${CLEARWING:-3.1.0}"
VERSION="$(sed -n 's/^buildversion=//p' gradle.properties)"

# Clearwing transpiler: JitPack does not serve the 3.1.x builds, so build the
# matching tag from source (its Gradle 7.4 wrapper needs a JDK 17, not newer).
CW_DIR="$ROOT/build/clearwing-$CLEARWING"
TRANSPILER_JAR="$CW_DIR/transpiler/build/libs/transpiler-$CLEARWING.jar"
if [ ! -f "$TRANSPILER_JAR" ]; then
    rm -rf "$CW_DIR"
    git clone -q --depth 1 -b "v$CLEARWING" https://github.com/TheLogicMaster/clearwing-vm "$CW_DIR"
    (cd "$CW_DIR" && JITPACK=1 bash ./gradlew --no-daemon --console=plain :transpiler:shadowJar)
fi

./gradlew --no-daemon --console=plain -PlocalRhino -PversionModifier=switch \
    --init-script switch/clearwing.gradle :tools:pack :switch:switchDeps

DEPS="$ROOT/switch/build/switch-deps"
OUT="$ROOT/build/Mindustry"         # directory name becomes the NRO name
DIST="$ROOT/build/dist"
rm -rf "$DIST"
mkdir -p "$DIST/src" "$DIST/romfs/classpath" "$OUT"

MODS=()
if [ -d switch/mods ]; then
    for d in switch/mods/*/; do [ -d "$d" ] && MODS+=("$d"); done
fi

echo "== Transpiling Java bytecode to C++"
java -jar "$TRANSPILER_JAR" \
    --input "$DEPS"/libs/*.jar "${MODS[@]}" "$DEPS/classes" "$DEPS/core" \
    --output "$DIST" \
    --source "$ROOT/switch/src/main/java" "$ROOT/core/src" \
    --config "$ROOT/switch/switch.json"

echo "== Collecting resources"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
for jar in "$DEPS"/libs/*.jar; do
    rm -rf "$TMP/x" && mkdir "$TMP/x"
    unzip -q -o "$jar" -d "$TMP/x" || true
    # Native code shipped inside jars
    (cd "$TMP/x" && find . -type f \( -name '*.cpp' -o -name '*.hpp' -o -name '*.h' -o -name '*.c' \) \
        -print0 | rsync -a --from0 --files-from=- ./ "$DIST/src/")
    # Everything else that is not class files goes on the romfs classpath
    (cd "$TMP/x" && find . -type f \
        -not -name '*.class' -not -name '*.gwt.xml' \
        -not -name '*.cpp' -not -name '*.hpp' -not -name '*.h' -not -name '*.c' \
        -not -path './META-INF/*' -not -path './switchgdx/*' -print0 \
        | rsync -a --from0 --files-from=- ./ "$DIST/romfs/classpath/")
done
for mod in "${MODS[@]}"; do
    rsync -a --exclude 'META-INF' --exclude '.github' --exclude '.git' --exclude '*.class' \
        --exclude 'classes.dex' "$mod" "$DIST/romfs/mods/$(basename "$mod")/"
done
cp -r core/assets/. "$DIST/romfs/"
rm -rf "$DIST/romfs/cache"
cp switch/icon.jpg "$DIST/icon.jpg"
# CMake project and the SwitchGDX native backend
cp -r switch/res/. "$DIST/"

# Without an nxlink host, send stdout/stderr (uncaught Java exceptions, game logs)
# to Mindustry.log next to the NRO so crashes can be diagnosed on hardware.
python3 - "$DIST/src/switchgdx/SwitchGDX.cpp" <<'PY'
import sys
p = sys.argv[1]
s = open(p).read()
old = "    socketInitializeDefault();\n    nxlinkStdio();\n"
new = ("    socketInitializeDefault();\n"
       "    if (nxlinkStdio() < 0) {\n"
       "        freopen(\"Mindustry.log\", \"w\", stdout);\n"
       "        freopen(\"Mindustry.log\", \"a\", stderr);\n"
       "        setvbuf(stdout, nullptr, _IONBF, 0);\n"
       "        setvbuf(stderr, nullptr, _IONBF, 0);\n"
       "    }\n")
if old not in s:
    sys.exit("SwitchGDX.cpp: userAppInit pattern not found")
open(p, "w").write(s.replace(old, new, 1))
PY

# Newer devkitPro freetype pulls in harfbuzz (and friends): take the static
# link line from pkg-config, and group the libs so link order doesn't matter.
PKGCONF="$DEVKITPRO/portlibs/switch/bin/aarch64-none-elf-pkg-config"
FT_LIBS="$("$PKGCONF" --static --libs freetype2 2>/dev/null || echo -lfreetype)"
echo "freetype link flags: $FT_LIBS"
sed -i \
    -e "s|-lglad |-Wl,--start-group -lglad |" \
    -e "s|-lfreetype|$FT_LIBS|" \
    -e "s|-lstdc++ -lm)|-lstdc++ -lm -Wl,--end-group)|" \
    "$DIST/switch.cmake"
grep -q "start-group" "$DIST/switch.cmake" || { echo "switch.cmake patch failed"; exit 1; }

# Sync into the stable build dir so unchanged files keep their timestamps
rsync -crh --delete --checksum --exclude '/build-*' "$DIST/" "$OUT/"

echo "== Building NRO"
cd "$OUT"
export APP_TITLE="Mindustry" APP_AUTHOR="Anuken, TheLogicMaster, axewoo" APP_VERSION="$VERSION"
export CMAKE_POLICY_VERSION_MINIMUM=3.5
cmake --toolchain DevkitA64Libnx.cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -B build-switch .
cmake --build build-switch -j"$JOBS"
ls -la build-switch/*.nro
