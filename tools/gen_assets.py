"""Texturas y JSON del mod Misiones Diarias.  Uso: python tools/gen_assets.py"""
import json
import os
import random

from PIL import Image, ImageDraw

NS = "misiones"
RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def hexc(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
os.makedirs(os.path.join(A, "textures", "item"), exist_ok=True)

# ---------------------------------------------------------------- texturas
rnd = random.Random(2)
img = Image.new("RGBA", (32, 32))
for y in range(32):
    for x in range(32):
        v = rnd.randint(-14, 14)
        b = hexc("b5813f")
        img.putpixel((x, y), (b[0] + v, b[1] + v, b[2] + v // 2, 255))
d = ImageDraw.Draw(img)
d.rectangle((0, 0, 31, 31), outline=hexc("5a3a22"))
d.rectangle((1, 1, 30, 30), outline=hexc("7a522a"))
notas = [((3, 3, 13, 14), "fff3a0"), ((16, 4, 28, 13), "ffffff"), ((5, 17, 15, 28), "c8f0ff"), ((18, 16, 28, 28), "ffd0d0")]
for (x0, y0, x1, y1), col in notas:
    d.rectangle((x0, y0, x1, y1), fill=hexc(col))
    for yy in range(y0 + 3, y1 - 1, 2):
        d.line((x0 + 2, yy, x1 - 2 - rnd.randint(0, 3), yy), fill=hexc("8a8a8a"))
    d.rectangle(((x0 + x1) // 2, y0 - 1, (x0 + x1) // 2 + 1, y0), fill=hexc("c0392b"))  # chinche
d.text((0, 0), "", fill=0)
img.save(os.path.join(A, "textures", "block", "tablon.png"))
img = Image.new("RGBA", (16, 16), hexc("5a3a22"))
img.save(os.path.join(A, "textures", "block", "tablon_marco.png"))

rows = [
    "................",
    "...kkkkkkkkkk...",
    "...kBBBBBBBBkk..",
    "...kBWWWWWWBkWk.",
    "...kBWggggWBkWk.",
    "...kBWWWWWWBkWk.",
    "...kBWggWWWBkWk.",
    "...kBWWWWWWBkWk.",
    "...kBWgggWWBkWk.",
    "...kBWWWWWWBkWk.",
    "...kBWYYWWWBkWk.",
    "...kBWYYWWWBkWk.",
    "...kBBBBBBBBkWk.",
    "...kkkkkkkkkkWk.",
    "....kWWWWWWWWWk.",
    "....kkkkkkkkkkk.",
]
pal = {".": (0, 0, 0, 0), "k": hexc("2b1d14"), "B": hexc("2b6cb0"), "W": hexc("fbf8ec"), "g": hexc("8a8a8a"), "Y": hexc("f5c400")}
img = Image.new("RGBA", (16, 16))
for y, r in enumerate(rows):
    for x, ch in enumerate(r):
        img.putpixel((x, y), pal[ch])
img.save(os.path.join(A, "textures", "item", "libreta_de_misiones.png"))

icon = Image.new("RGBA", (64, 64), hexc("2b6cb0"))
d = ImageDraw.Draw(icon)
d.rectangle((10, 8, 54, 56), fill=hexc("fbf8ec"))
for i, y in enumerate((16, 30, 44)):
    d.rectangle((15, y, 23, y + 8), outline=hexc("2b1d14"))
    if i < 2:
        d.line((16, y + 4, 19, y + 7), fill=hexc("2e7d32"), width=2)
        d.line((19, y + 7, 23, y), fill=hexc("2e7d32"), width=2)
    d.rectangle((27, y + 3, 50, y + 5), fill=hexc("8a8a8a"))
icon.save(os.path.join(A, "icon.png"))

# ---------------------------------------------------------------- modelos
write(os.path.join(A, "models", "block", "tablon_de_misiones.json"), {
    "textures": {"t": f"{NS}:block/tablon", "m": f"{NS}:block/tablon_marco", "particle": f"{NS}:block/tablon"},
    "elements": [
        {"from": [0, 0, 14], "to": [16, 16, 16], "faces": {
            "north": {"texture": "#t", "uv": [0, 0, 16, 16]}, "south": {"texture": "#m"}, "east": {"texture": "#m"},
            "west": {"texture": "#m"}, "up": {"texture": "#m"}, "down": {"texture": "#m"}}},
        {"from": [0, 15, 13], "to": [16, 16, 14], "faces": {f: {"texture": "#m"} for f in ("north", "south", "east", "west", "up", "down")}},
    ]})
rot = {"north": 0, "east": 90, "south": 180, "west": 270}
write(os.path.join(A, "blockstates", "tablon_de_misiones.json"), {"variants": {
    f"facing={f}": ({"model": f"{NS}:block/tablon_de_misiones", "y": r} if r else {"model": f"{NS}:block/tablon_de_misiones"})
    for f, r in rot.items()}})
write(os.path.join(A, "items", "tablon_de_misiones.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/tablon_de_misiones"}})
write(os.path.join(A, "models", "item", "libreta_de_misiones.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/libreta_de_misiones"}})
write(os.path.join(A, "items", "libreta_de_misiones.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/libreta_de_misiones"}})

# ---------------------------------------------------------------- data
write(os.path.join(D, "loot_table", "blocks", "tablon_de_misiones.json"), {
    "type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                                          "entries": [{"type": "minecraft:item", "name": f"{NS}:tablon_de_misiones"}]}]})
write(os.path.join(D, "recipe", "tablon_de_misiones.json"), {
    "type": "minecraft:crafting_shaped", "pattern": ["PPP", "PBP", "PPP"],
    "key": {"P": "#minecraft:planks", "B": "minecraft:book"}, "result": {"id": f"{NS}:tablon_de_misiones"}})
write(os.path.join(D, "recipe", "libreta_de_misiones.json"), {
    "type": "minecraft:crafting_shapeless", "ingredients": ["minecraft:book", "minecraft:feather", "minecraft:compass"],
    "result": {"id": f"{NS}:libreta_de_misiones"}})
write(os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "axe.json"), {"values": [f"{NS}:tablon_de_misiones"]})

names = {"block.misiones.tablon_de_misiones": "Tablón de Misiones", "item.misiones.libreta_de_misiones": "Libreta de Misiones",
         "key.misiones.abrir": "Abrir misiones", "key.category.misiones.misiones": "Misiones Diarias"}
for lang in ("en_us", "es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve"):
    write(os.path.join(A, "lang", lang + ".json"), names)
print("Assets de misiones generados")
