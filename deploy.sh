#!/usr/bin/env bash
# Build the "direct" roozTrans APK and publish it to your own server. Phones pick it up automatically.
# Usage: ./deploy.sh [versionName]      (versionCode is bumped automatically)
set -euo pipefail
cd "$(dirname "$0")"
[ -f deploy.properties ] || { echo "Missing deploy.properties (copy deploy.properties.example)"; exit 1; }
cfg() { grep "^$1=" deploy.properties | cut -d= -f2-; }
HOST=$(cfg host); DIR=$(cfg dir); OWNER=$(cfg owner)

code=$(( $(grep versionCode version.properties | cut -d= -f2) + 1 ))
name=${1:-$(grep versionName version.properties | cut -d= -f2 | awk -F. '{print $1"."$2+1}')}
cp version.properties /tmp/rooztrans-version.bak
trap 'echo "Deploy FAILED - version left unchanged"; mv /tmp/rooztrans-version.bak version.properties' ERR
printf 'versionCode=%s\nversionName=%s\n' "$code" "$name" > version.properties

export JAVA_HOME=$(ls -d ~/.local/opt/jdk-21* | head -1)
./gradlew --no-daemon -q assembleDirectRelease
APK=app/build/outputs/apk/direct/release/app-direct-release.apk
sha=$(sha256sum "$APK" | cut -d' ' -f1)
size=$(stat -c %s "$APK")
printf '{"versionCode":%s,"versionName":"%s","sha256":"%s","size":%s}\n' "$code" "$name" "$sha" "$size" > /tmp/rooztrans-version.json

# upload under temp names, then swap in atomically (APK first, then the version that points at it)
scp -q "$APK" "$HOST:$DIR/roozTrans.apk.new"
scp -q /tmp/rooztrans-version.json "$HOST:$DIR/version.json.new"
ssh "$HOST" "cd $DIR && chown $OWNER *.new && chmod 640 *.new && mv roozTrans.apk.new roozTrans.apk && mv version.json.new version.json"
rm -f /tmp/rooztrans-version.json
trap - ERR; rm -f /tmp/rooztrans-version.bak
echo "Published roozTrans v$name (code $code, $((size/1048576)) MB)"
