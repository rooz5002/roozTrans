#!/usr/bin/env python3
"""Render TERMS.md / PRIVACY.md to standalone HTML pages (server/legal/) for the website.
Handles the small Markdown subset those files use. Run: python3 server/build-legal.py"""
import html, re, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
LINKS = {"PRIVACY.md": "privacy.html", "TERMS.md": "terms.html", "LICENSE": "https://github.com/rooz5002/roozTrans/blob/main/LICENSE"}

def inline(s):
    s = html.escape(s, quote=False)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", s)
    s = re.sub(r"(?<![\w*])_(.+?)_(?![\w*])", r"<em>\1</em>", s)
    s = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", lambda m: f'<a href="{LINKS.get(m[2], m[2])}">{m[1]}</a>', s)
    s = re.sub(r"&lt;(https?://[^&]+)&gt;", r'<a href="\1">\1</a>', s)
    return s

def render(md):
    out, lst, para = [], None, []
    lines = md.splitlines()
    def flush():
        nonlocal lst, para
        if para: out.append("<p>" + " ".join(inline(p) for p in para) + "</p>"); para = []
        if lst: out.append(f"</{lst}>"); lst = None
    i = 0
    while i < len(lines):
        l = lines[i].rstrip()
        if l.startswith("|"):
            flush(); rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")]); i += 1
            out.append("<div class=t><table><tr>" + "".join(f"<th>{inline(c)}</th>" for c in rows[0]) + "</tr>"
                       + "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in rows[2:]) + "</table></div>")
            continue
        m = re.match(r"(#+) (.*)", l)
        if m: flush(); n = len(m[1]); out.append(f"<h{n}>{inline(m[2])}</h{n}>")
        elif re.match(r"[-•] ", l) or re.match(r"\d+\. ", l):
            kind = "ol" if l[0].isdigit() else "ul"
            if para: flush()
            if lst != kind:
                if lst: out.append(f"</{lst}>")
                out.append(f"<{kind}>"); lst = kind
            out.append("<li>" + inline(re.sub(r"^([-•]|\d+\.) ", "", l)) + "</li>")
        elif not l: flush()
        else:
            if lst: flush()
            para.append(l.rstrip("\\").rstrip())
        i += 1
    flush(); return "\n".join(out)

CSS = """:root{--bg:#FAF9F5;--fg:#1F1E1D;--mut:#6b6862;--acc:#C96442;--line:#E5E2D9}
@media (prefers-color-scheme:dark){:root{--bg:#1A1A1E;--fg:#ECECEC;--mut:#a3a3aa;--acc:#E3A857;--line:#34343B}}
body{margin:0;background:var(--bg);color:var(--fg);font:16px/1.6 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:760px;margin:0 auto;padding:24px 16px 64px}h1{font-size:1.7em;line-height:1.25}h2{margin-top:1.6em;font-size:1.2em}
a{color:var(--acc)}em{color:var(--mut)}code{font-size:.9em}.t{overflow-x:auto}table{border-collapse:collapse;width:100%;font-size:.92em}
th,td{border:1px solid var(--line);padding:8px;text-align:left;vertical-align:top}nav{margin-bottom:8px;font-size:.92em}"""

for src, dst, title in [("TERMS.md", "terms.html", "roozTrans Terms of Use"), ("PRIVACY.md", "privacy.html", "roozTrans Privacy Policy")]:
    body = render((root / src).read_text())
    page = f"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title><style>{CSS}</style></head><body><main>
<nav><a href="terms.html">Terms of use</a> · <a href="privacy.html">Privacy policy</a> · <a href="https://github.com/rooz5002/roozTrans">Source</a></nav>
{body}
</main></body></html>
"""
    (root / "server/legal" / dst).write_text(page)
    print("wrote server/legal/" + dst)
