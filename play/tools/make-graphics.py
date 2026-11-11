#!/usr/bin/env python3
"""Make the Play Store icon (512x512) and feature graphic (1024x500) from the app's launcher icon.
Run: python3 play/tools/make-graphics.py"""
import re, pathlib
from PIL import Image, ImageDraw, ImageFont, ImageChops

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "play/graphics"
RED = (198, 40, 40)          # @color/launcher_bg #C62828
xml = (ROOT / "app/src/main/res/drawable/ic_launcher_fg.xml").read_text()
PATH = re.search(r'pathData="([^"]+)"', xml)[1]
CJK = "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
CJK_R = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"

def subpaths(d):
    """Parse the SVG path subset used by the icon into lists of points (beziers flattened)."""
    toks = re.findall(r"[MmLlHhVvCcZz]|-?\d*\.?\d+", d)
    i, cmd, x, y, sx, sy, cur, out = 0, None, 0.0, 0.0, 0.0, 0.0, [], []
    num = lambda: float(toks[i])
    while i < len(toks):
        t = toks[i]
        if re.match(r"[A-Za-z]", t): cmd = t; i += 1
        if cmd in "Zz":
            if cur: out.append(cur); cur = []
            x, y = sx, sy; continue
        if cmd in "Mm":
            if cur: out.append(cur)
            dx, dy = float(toks[i]), float(toks[i+1]); i += 2
            x, y = (x+dx, y+dy) if cmd == "m" else (dx, dy); sx, sy = x, y; cur = [(x, y)]
            cmd = "l" if cmd == "m" else "L"
        elif cmd in "Ll":
            dx, dy = float(toks[i]), float(toks[i+1]); i += 2
            x, y = (x+dx, y+dy) if cmd == "l" else (dx, dy); cur.append((x, y))
        elif cmd in "Hh":
            v = float(toks[i]); i += 1; x = x+v if cmd == "h" else v; cur.append((x, y))
        elif cmd in "Vv":
            v = float(toks[i]); i += 1; y = y+v if cmd == "v" else v; cur.append((x, y))
        elif cmd in "Cc":
            p = [float(v) for v in toks[i:i+6]]; i += 6
            if cmd == "c": p = [p[0]+x, p[1]+y, p[2]+x, p[3]+y, p[4]+x, p[5]+y]
            x0, y0 = x, y
            for k in range(1, 13):
                s = k/12; a = (1-s)**3; b = 3*(1-s)**2*s; c = 3*(1-s)*s*s; e = s**3
                cur.append((a*x0+b*p[0]+c*p[2]+e*p[4], a*y0+b*p[1]+c*p[3]+e*p[5]))
            x, y = p[4], p[5]
    if cur: out.append(cur)
    return out

def glyph(size, color=(255, 255, 255)):
    """The translate glyph (24x24 viewport) rendered at size px, even-odd fill, 4x supersampled."""
    S = size * 4; mask = Image.new("1", (S, S), 0)
    for sp in subpaths(PATH):
        m = Image.new("1", (S, S), 0)
        ImageDraw.Draw(m).polygon([(px*S/24, py*S/24) for px, py in sp], fill=1)
        mask = ImageChops.logical_xor(mask, m)
    alpha = mask.convert("L").resize((size, size), Image.LANCZOS)
    g = Image.new("RGBA", (size, size), color + (0,)); g.putalpha(alpha); return g

# ---- 512x512 icon: full-bleed square (Play applies the rounded mask itself) ----
icon = Image.new("RGB", (512, 512), RED)
g = glyph(300); icon.paste(g, (106, 106), g)
icon.save(OUT / "icon-512.png")

# ---- 1024x500 feature graphic (no alpha) ----
W, H = 1024, 500
fg = Image.new("RGB", (W, H), RED); d = ImageDraw.Draw(fg)
# soft lighter circle behind the glyph
circ = Image.new("L", (W, H), 0); ImageDraw.Draw(circ).ellipse((70, 90, 390, 410), fill=40)
fg.paste((255, 255, 255), (0, 0), circ)
g = glyph(230); fg.paste(g, (115, 135), g)
f_title = ImageFont.truetype(CJK, 92, index=0)
f_sub = ImageFont.truetype(CJK, 46, index=0)       # index 0 = SC in the .ttc
f_small = ImageFont.truetype(CJK_R, 30, index=0)
d.text((440, 110), "roozTrans", font=f_title, fill="white")
d.text((444, 232), "English ⇄ 中文", font=f_sub, fill="white")
d.text((446, 310), "Text · photos · your screen", font=f_small, fill=(255, 225, 220))
d.text((446, 352), "Works offline · no ads", font=f_small, fill=(255, 225, 220))
fg.save(OUT / "feature-1024x500.png")
print("wrote", OUT / "icon-512.png", "and", OUT / "feature-1024x500.png")
