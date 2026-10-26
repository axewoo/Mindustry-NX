#!/usr/bin/env bash
# Prepare the source dependencies the Switch build compiles locally, next to this repo:
#   ../Arc    Anuken/Arc at the archash from gradle.properties, with switch/arc-patches applied
#             (they add the SwitchGDX backend)
#   ../rhino  Anuken/rhino at the rhinoVersion from build.gradle (JitPack is unreliable)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PARENT="$(dirname "$ROOT")"
ARC_HASH="$(sed -n 's/^archash=//p' "$ROOT/gradle.properties")"
RHINO_HASH="$(sed -n "s/^\s*rhinoVersion = '\(.*\)'/\1/p" "$ROOT/build.gradle")"

# checkout <dir> <repo url> <commit>: clone once, then reset to <commit>
checkout(){
    [ -d "$1/.git" ] || git clone -q "$2" "$1"
    git -C "$1" cat-file -e "$3^{commit}" 2>/dev/null || git -C "$1" fetch -q origin
    git -C "$1" checkout -q -f --detach "$3"
    git -C "$1" clean -qfdx
}

echo "== Arc $ARC_HASH"
checkout "$PARENT/Arc" https://github.com/Anuken/Arc.git "$ARC_HASH"
git -C "$PARENT/Arc" -c user.name=switch-build -c user.email=switch-build@localhost \
    am -q --whitespace=nowarn "$ROOT"/switch/arc-patches/*.patch
git -C "$PARENT/Arc" log --oneline -1

echo "== rhino $RHINO_HASH"
checkout "$PARENT/rhino" https://github.com/Anuken/rhino.git "$RHINO_HASH"
git -C "$PARENT/rhino" log --oneline -1
