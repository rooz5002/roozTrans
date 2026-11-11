#!/usr/bin/env bash
# Run ON the server to swap the download page's password hash:
#   ssh user@server bash -s -- /path/to/public_html/rooztrans/index.php '<bcrypt hash>' < set-password.sh
# Make a hash with:  php -r 'echo password_hash("new-password", PASSWORD_DEFAULT), "\n";'
set -e
F=${1:?index.php path}; H=${2:?bcrypt hash}
cp -p "$F" "$F.bak"
awk -v h="$H" -v q="'" 'NR==3 && /^\$HASH = /{print "$HASH = " q h q ";"; next} 1' "$F" > "$F.new"
chown --reference="$F" "$F.new"; chmod --reference="$F" "$F.new"; mv "$F.new" "$F"
sed -n 3p "$F"
