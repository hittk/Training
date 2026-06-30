#!/bin/bash
# Run from ~/Training. Decodes the fixed debug keystore and patches the build
# so every APK signs identically (in-place upgrades, no data loss).
set -e
cd "$HOME/Training"

# 1. Decode the keystore (expects debug_keystore_base64.txt alongside, e.g. in Downloads)
SRC="${1:-/sdcard/Download/debug_keystore_base64.txt}"
if [ ! -f "$SRC" ]; then echo "✗ base64 file not found at $SRC"; exit 1; fi
base64 -d "$SRC" > app/debug.keystore
echo "✓ debug.keystore written ($(wc -c < app/debug.keystore) bytes)"

# verify checksum
EXPECT="1bac210c89bb5ac52b99606e6b291836d8b42b478904032c141c4be5d658a3a3"
GOT=$(sha256sum app/debug.keystore | cut -d' ' -f1)
if [ "$GOT" != "$EXPECT" ]; then echo "✗ checksum mismatch! got $GOT"; exit 1; fi
echo "✓ checksum verified"

# 2. Patch build.gradle.kts (only if not already patched)
if ! grep -q 'signingConfigs {' app/build.gradle.kts; then
  python3 - <<'PY'
f="app/build.gradle.kts"; c=open(f).read()
c=c.replace(
'''    buildTypes {
        release {''',
'''    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {''')
c=c.replace('versionCode = 1','versionCode = 2')
c=c.replace('versionName = "0.1.0"','versionName = "0.2.0"')
open(f,"w").write(c)
print("  build.gradle.kts patched")
PY
else
  echo "  build.gradle.kts already has signingConfigs — skipping"
fi

# 3. Allow the debug keystore through .gitignore
if ! grep -q '!app/debug.keystore' .gitignore 2>/dev/null; then
  echo '!app/debug.keystore' >> .gitignore
  echo "  .gitignore: debug.keystore allowed"
fi

# 4. Commit & push
git add -A
git commit -m "build: fixed debug keystore for in-place upgrades"
git push
echo ""
echo "✓ Done. ONE more uninstall needed (old app used a different key), then"
echo "  install this build. All future installs will upgrade in place."
