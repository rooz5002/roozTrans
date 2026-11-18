"""Translate sentences.tsv with ML Kit offline, through the roozTrans app on a connected emulator. -> out/mlkit.tsv"""
import re, shlex, subprocess, time, html, pathlib, os
ADB = os.path.expanduser("~/Android/Sdk/platform-tools/adb")
def sh(cmd): return subprocess.run([ADB, "shell", cmd], capture_output=True, text=True).stdout
rows = [l.rstrip("\n").split("\t") for l in open("sentences.tsv", encoding="utf-8") if l.strip()]
out = pathlib.Path("out"); out.mkdir(exist_ok=True)
with open(out / "mlkit.tsv", "w", encoding="utf-8") as w:
    for lang, s in rows:
        sh("am force-stop com.rooz.trans")
        sh("am start -n com.rooz.trans/com.rooz.translator.MainActivity -a android.intent.action.SEND -t text/plain "
           "--es android.intent.extra.TEXT " + shlex.quote(s))
        res = "?"
        for _ in range(10):
            time.sleep(1.5)
            sh("uiautomator dump /sdcard/u.xml")
            texts = [html.unescape(t) for t in re.findall(r'text="([^"]*)"', sh("cat /sdcard/u.xml"))]
            if s in texts:
                i = texts.index(s); after = [t for t in texts[i+1:] if t]
                if after and after[0] != "…": res = after[0]; break
        w.write(f"{lang}\t{s}\t{res.replace(chr(10), ' ')}\n"); print(res)
