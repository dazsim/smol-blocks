"""Builds the precision pickaxe item textures from the vanilla pickaxe palettes, plus an enlarged preview."""
import sys
from PIL import Image

# Vanilla pickaxe layout with the right-hand end drawn as a narrow spike
ART = [
    "................",
    "................",
    "......aaaaa.....",
    ".....abcddcaef..",
    "......agggddhi..",
    "..........ecdg..",
    ".........efidcg.",
    "........ehi..dg.",
    ".......efi...dg.",
    "......ehi....cg.",
    ".....efi.....cg.",
    "....ehi......bg.",
    "...efi.......g..",
    "..ehi...........",
    "..ii............",
    "................",
]

HANDLE = {"e": (73, 54, 21), "f": (104, 78, 30), "h": (137, 103, 39), "i": (40, 30, 11)}
PALETTES = {
    "iron": {"a": (68, 68, 68), "b": (255, 255, 255), "c": (216, 216, 216), "d": (193, 193, 193), "g": (24, 24, 24)},
    "diamond": {"a": (14, 63, 54), "b": (51, 235, 203), "c": (43, 199, 172), "d": (39, 178, 154), "g": (8, 37, 32)},
}


def draw(palette):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    colours = {**HANDLE, **palette}
    for y, row in enumerate(ART):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), colours[ch] + (255,))
    return im


def main(out_dir, preview):
    images = []
    for tier, palette in PALETTES.items():
        im = draw(palette)
        im.save(f"{out_dir}/{tier}_precision_pickaxe.png")
        images.append(im)
    scale = 16
    sheet = Image.new("RGBA", (len(images) * 16 * scale + (len(images) + 1) * 32, 16 * scale + 64), (139, 139, 139, 255))
    for i, im in enumerate(images):
        sheet.paste(im.resize((16 * scale, 16 * scale), Image.NEAREST), (32 + i * (16 * scale + 32), 32), im.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.save(preview)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
