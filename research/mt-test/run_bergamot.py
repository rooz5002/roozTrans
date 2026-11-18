"""Translate sentences.tsv with Mozilla's Firefox Translations (Bergamot) models. Writes out/bergamot.tsv"""
import pathlib, time, bergamot
M = pathlib.Path("models")
def cfg(pair):
    d = M / pair; f = {p.name: str(p) for p in d.iterdir()}
    model = next(v for k, v in f.items() if k.startswith("model."))
    vocabs = [v for k, v in sorted(f.items()) if k.endswith(".spm")]
    if len(vocabs) == 1: vocabs *= 2
    else: vocabs = [next(v for k, v in f.items() if k.startswith("srcvocab")), next(v for k, v in f.items() if k.startswith("trgvocab"))]
    lex = next(v for k, v in f.items() if k.startswith("lex."))
    return f"""models: [{model}]
vocabs: [{vocabs[0]}, {vocabs[1]}]
shortlist: [{lex}, false]
beam-size: 1
normalize: 1.0
word-penalty: 0
max-length-break: 128
mini-batch-words: 1024
max-length-factor: 2.0
skip-cost: true
cpu-threads: 0
quiet: true
quiet-translation: true
gemm-precision: int8shiftAlphaAll
alignment: soft
"""
svc = bergamot.Service(bergamot.ServiceConfig(numWorkers=1))
models = {p: svc.modelFromConfig(cfg(p)) for p in ("en-zh", "zh-en")}
rows = [l.rstrip("\n").split("\t") for l in open("sentences.tsv", encoding="utf-8") if l.strip()]
out = pathlib.Path("out"); out.mkdir(exist_ok=True)
t0 = time.time()
with open(out / "bergamot.tsv", "w", encoding="utf-8") as w:
    for lang, s in rows:
        m = models["zh-en" if lang == "zh" else "en-zh"]
        r = svc.translate(m, bergamot.VectorString([s]), bergamot.ResponseOptions(alignment=False, qualityScores=False, HTML=False))
        w.write(f"{lang}\t{s}\t{r[0].target.text}\n")
print(f"{len(rows)} sentences in {time.time()-t0:.1f}s")
