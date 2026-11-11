#!/usr/bin/env bash
# Regenerate terms.html / privacy.html from TERMS.md / PRIVACY.md and upload them next to update.php.
# Uses host / owner / updateUrl from deploy.properties (untracked).
set -euo pipefail
cd "$(dirname "$0")/.."
cfg() { grep "^$1=" deploy.properties | cut -d= -f2-; }
HOST=$(cfg host); OWNER=$(cfg owner); URL=$(cfg updateUrl)
WEB=$(cfg webdir)   # e.g. /home/site/public_html/rooztrans
python3 -I server/build-legal.py
scp -q server/legal/terms.html server/legal/privacy.html "$HOST:$WEB/"
ssh "$HOST" "cd '$WEB' && chown $OWNER terms.html privacy.html && chmod 644 terms.html privacy.html"
base=${URL%/*}
for f in terms privacy; do printf '%s → HTTP %s\n' "$base/$f.html" "$(curl -s -o /dev/null -w '%{http_code}' "$base/$f.html")"; done
