"""Download Mozilla's released Firefox Translations (Bergamot) models from their public registry.
Usage: python3 -I fetch_models.py <registry.json> <outdir> en-zh zh-en"""
import json, sys, gzip, hashlib, pathlib, urllib.request
reg = json.load(open(sys.argv[1])); base = reg["baseUrl"]; out = pathlib.Path(sys.argv[2])
for pair in sys.argv[3:]:
    cands = reg["models"][pair]
    m = next((c for c in cands if (c.get("releaseStatus") or "").startswith("Release") and c["architecture"] == "base-memory"), None) \
        or next(c for c in cands if c.get("releaseStatus"))
    d = out / pair; d.mkdir(parents=True, exist_ok=True)
    print(pair, m["architecture"], m["releaseStatus"], m.get("metrics", {}).get("flores200-plus", {}).get("comet22"))
    for kind, f in m["files"].items():
        body = urllib.request.urlopen(urllib.request.Request(f"{base}/{f['path']}", headers={"User-Agent": "mt-test"}), timeout=600).read()
        name = f["path"].rsplit("/", 1)[1]
        if name.endswith(".gz"): body = gzip.decompress(body); name = name[:-3]
        if "uncompressedHash" in f: assert hashlib.sha256(body).hexdigest() == f["uncompressedHash"], name
        (d / name).write_bytes(body); print("  ", kind, name, len(body))
