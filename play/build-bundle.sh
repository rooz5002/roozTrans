#!/usr/bin/env bash
# Build the Google Play bundle and copy it here as play/release/roozTrans-<version>.aab
# Version comes from ../version.properties (bump it, or run ../deploy.sh first, so Play sees a new versionCode).
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f keystore.properties ] || { echo "Missing keystore.properties (upload key) - see keystore.properties.example"; exit 1; }
export JAVA_HOME=${JAVA_HOME:-$(ls -d ~/.local/opt/jdk-21* | head -1)}
./gradlew --no-daemon -q bundlePlayRelease
name=$(grep versionName version.properties | cut -d= -f2); code=$(grep versionCode version.properties | cut -d= -f2)
mkdir -p play/release
out=play/release/roozTrans-$name-code$code.aab
cp app/build/outputs/bundle/playRelease/app-play-release.aab "$out"
signer=$("$JAVA_HOME/bin/jarsigner" -verify -verbose:summary -certs "$out" 2>/dev/null | grep -m1 'Signed by' || true)
echo "Built $out ($(du -h "$out" | cut -f1))"
echo "  $signer"
case "$signer" in *"Android Debug"*) echo "  WARNING: signed with the debug key, not the upload key!"; exit 1;; esac
