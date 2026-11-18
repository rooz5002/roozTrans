"""Translate sentences.tsv with a local Ollama model. Usage: python3 -I run_llm.py <model> -> out/<model>.tsv"""
import json, sys, time, pathlib, urllib.request
model = sys.argv[1]
SYS = {"zh": "Translate the user's Chinese text into natural English. It is a sign, notice, menu or message seen by a foreign traveller in China. Output only the translation.",
       "en": "Translate the user's English text into natural Simplified Chinese, as a traveller would say it in China. Output only the translation."}
def ask(lang, s):
    body = json.dumps({"model": model, "stream": False, "think": False, "options": {"temperature": 0},
                       "messages": [{"role": "system", "content": SYS[lang]}, {"role": "user", "content": s}]}).encode()
    r = urllib.request.urlopen(urllib.request.Request("http://127.0.0.1:11434/api/chat", data=body, headers={"Content-Type": "application/json"}), timeout=300)
    return json.loads(r.read())["message"]["content"].strip().replace("\n", " ")
rows = [l.rstrip("\n").split("\t") for l in open("sentences.tsv", encoding="utf-8") if l.strip()]
out = pathlib.Path("out"); out.mkdir(exist_ok=True); t0 = time.time()
with open(out / f"{model.replace(':', '_')}.tsv", "w", encoding="utf-8") as w:
    for lang, s in rows: w.write(f"{lang}\t{s}\t{ask(lang, s)}\n")
print(model, f"{time.time()-t0:.1f}s")
