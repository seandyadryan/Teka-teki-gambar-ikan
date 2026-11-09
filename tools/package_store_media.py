"""Compose Play Store artwork and a narrated trailer from real emulator captures."""
from pathlib import Path
import math
import os
import subprocess

import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT / "play-store"
APP = ROOT / "app/src/main"
RAW = ROOT / ".capture"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
SERIF = APP / "res/font/field_serif.ttf"
WINDOWS_FONTS = Path(os.getenv("WINDIR", r"C:\Windows")) / "Fonts"
SANS = WINDOWS_FONTS / "segoeui.ttf"
BOLD = WINDOWS_FONTS / "segoeuib.ttf"
INK = "#102e36"
DEEP = "#09232b"
PAPER = "#f8efdb"
GOLD = "#f0bd72"
MINT = "#acd5c5"
MIST = "#b5cdcc"
CREAM_LINE = "#d9c6a7"
DARK_LINE = "#376069"


def font(size, face=SERIF):
    return ImageFont.truetype(str(face), size)


def text(draw, point, words, size, color, face=SERIF, spacing=4):
    x, y = point
    face = font(size, face)
    for line in words.splitlines():
        draw.text((x, y), line, font=face, fill=color, anchor="lt")
        y += round(size * 1.08) + spacing


def label(draw, point, words, size, color, tracking=3):
    x, y = point
    lettering = font(size, BOLD)
    for letter in words:
        draw.text((x, y), letter, font=lettering, fill=color, anchor="lt")
        x += draw.textlength(letter, font=lettering) + tracking


def contour(draw, width, height, dark):
    color = "#1b4149" if dark else "#ead9bb"
    for i in range(14):
        y = height * (0.24 + i * 0.058)
        points = [
            (x, y + height * (0.031 * math.sin(x / width * math.pi * 2 + i * 0.12)
                              + 0.022 * math.sin(x / width * math.pi * 3)))
            for x in range(0, width + 11, 10)
        ]
        draw.line(points, fill=color, width=max(1, width // 720))


def stock(width, height, dark=True):
    base = (16, 46, 54) if dark else (248, 239, 219)
    rng = np.random.default_rng(28)
    grit = rng.normal(0, 1.02, (height, width, 1))
    grid = np.clip(np.asarray(base) + grit, 0, 255).astype(np.uint8)
    canvas = Image.fromarray(grid).convert("RGBA")
    contour(ImageDraw.Draw(canvas), width, height, dark)
    return canvas


def specimen(canvas, fish, center, size):
    art = Image.open(APP / f"assets/fish/{fish}.webp").convert("RGBA")
    art.thumbnail(size, Image.Resampling.LANCZOS)
    canvas.alpha_composite(art, (int(center[0] - art.width / 2), int(center[1] - art.height / 2)))


def save(canvas, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(destination, optimize=True)


def feature():
    canvas = stock(1024, 500)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((31, 28, 993, 471), radius=11, outline=DARK_LINE, width=2)
    draw.line((87, 103, 453, 103), fill=DARK_LINE, width=2)
    label(draw, (88, 119), "THE OCEAN FIELD CLUB", 14, MINT)
    text(draw, (83, 160), "Fish Riddle\nClub", 72, PAPER, spacing=2)
    text(draw, (88, 363), "Illustrated ocean riddles", 24, GOLD, SANS)
    draw.ellipse((553, 64, 924, 433), outline=DARK_LINE, width=2)
    draw.ellipse((575, 86, 902, 411), outline="#2c545c", width=1)
    specimen(canvas, "parrotfish", (730, 241), (385, 290))
    specimen(canvas, "seahorse", (894, 311), (105, 140))
    draw = ImageDraw.Draw(canvas)
    for x, y in [(516, 141), (859, 106), (920, 176), (614, 398)]:
        draw.ellipse((x-3, y-3, x+3, y+3), fill=GOLD)
    save(canvas, STORE / "feature-graphic.png")


def framed_phone(canvas, screenshot, x, y, width, height):
    bezel = 15
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((x-18, y+6, x+width+19, y+height+32), radius=59, fill="#122528")
    draw.rounded_rectangle((x-9, y-8, x+width+9, y+height+8), radius=51, fill="#597476")
    draw.rounded_rectangle((x-7, y-7, x+width+7, y+height+7), radius=49, fill="#07171c")
    image = screenshot.convert("RGB").resize((width-2*bezel, height-2*bezel), Image.Resampling.LANCZOS)
    mask = Image.new("L", image.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width-1, image.height-1), radius=33, fill=255)
    canvas.paste(image, (x+bezel, y+bezel), mask)


PHONE = [
    ("01-clue", "01-picture-riddles", "NAME THE SPECIMEN", "Which fish\nhas a lamp?", "Study the illustration and follow the clue."),
    ("02-fact", "02-fish-facts", "OPEN THE FIELD NOTES", "A fish fact\nin every reveal", "Discover how each fish lives, hunts or hides."),
    ("03-next", "03-narrated-clues", "LISTEN CLOSELY", "Hear the clue", "Play every riddle and fish fact in English."),
    ("04-guide", "04-field-guide", "SAVE YOUR FINDS", "Build your\nfield guide", "Collect the fish you identify on each dive."),
    ("00-cover", "05-expeditions", "CHOOSE A ROUTE", "Four ocean\nexpeditions", "Meet 16 fish from reefs and the deep sea."),
]


def screenshots():
    for index, (frame, filename, eyebrow, headline, subtitle) in enumerate(PHONE):
        dark = index % 2 == 0
        canvas = stock(1080, 1920, dark)
        draw = ImageDraw.Draw(canvas)
        primary, accent, secondary = (PAPER, GOLD, MIST) if dark else (INK, "#854638", "#46616a")
        rule = DARK_LINE if dark else CREAM_LINE
        draw.line((70, 87, 1010, 87), fill=rule, width=3)
        label(draw, (72, 52), "FISH RIDDLE CLUB", 18, secondary)
        label(draw, (874, 52), f"PL. {index + 1:02}", 18, secondary)
        label(draw, (72, 117), eyebrow, 24, accent, 4)
        text(draw, (68, 173), headline, 83, primary, spacing=-2)
        subtitle_y = 365 if "\n" in headline else 285
        text(draw, (74, subtitle_y), subtitle, 31, secondary, SANS)
        y = 492 if "\n" in headline else 421
        width, height = 743, 1298
        with Image.open(RAW / "phone" / (frame + ".png")) as screenshot:
            framed_phone(canvas, screenshot, (1080-width)//2, y, width, height)
        label(draw, (74, 1859), "ILLUSTRATED OCEAN RIDDLES", 17, secondary)
        save(canvas, STORE / "phone" / (filename + ".png"))

    for profile in ("tablet-7-inch", "tablet-10-inch", "desktop"):
        for frame, filename, *_ in PHONE[:4]:
            with Image.open(RAW / profile / (frame + ".png")) as screenshot:
                save(screenshot, STORE / profile / (filename + ".png"))


def trailer_plate(title, eyebrow, sub, stage, color, screenshot=None):
    canvas = stock(1920, 1080)
    draw = ImageDraw.Draw(canvas)
    draw.line((84, 87, 1836, 87), fill=DARK_LINE, width=2)
    label(draw, (85, 50), "FISH RIDDLE CLUB", 22, MINT)
    label(draw, (1560, 50), "OCEAN FIELD CLUB", 22, MINT)
    label(draw, (95, 261), eyebrow, 27, GOLD, 5)
    text(draw, (88, 332), title, 100, PAPER, spacing=12)
    text(draw, (95, 637), sub, 35, MIST, SANS, spacing=12)
    draw.line((94, 896, 797, 896), fill=DARK_LINE, width=2)
    label(draw, (95, 920), stage, 19, color)
    if screenshot:
        with Image.open(screenshot) as picture:
            framed_phone(canvas, picture, 1176, 119, 507, 881)
    else:
        draw.ellipse((1067, 203, 1735, 872), outline=DARK_LINE, width=3)
        draw.ellipse((1104, 240, 1698, 835), outline=DARK_LINE, width=2)
        specimen(canvas, "anglerfish", (1415, 532), (620, 564))
    return canvas


def run_ffmpeg(*args):
    subprocess.run([FFMPEG, "-hide_banner", "-loglevel", "error", "-y", *map(str, args)], check=True)


def trailer():
    audio = APP / "assets/audio"
    media = RAW / "phone"
    stage = RAW / "trailer"
    stage.mkdir(exist_ok=True)
    plates = [
        ("Fish Riddle\nClub", "THE OCEAN FIELD CLUB", "An illustrated fish quiz", "DIVE IN", GOLD, None),
        ("Follow the\nclue", "01   THE RIDDLE", "Look at the fish.\nListen to the question.", "PICTURE RIDDLES  /  SPOKEN CLUES", GOLD, media / "01-clue.png"),
        ("Name the\nfish", "02   THE REVEAL", "Get it right to unlock\na narrated fish fact.", "SPECIMEN IDENTIFIED", GOLD, media / "02-fact.png"),
        ("Keep your\ndiscoveries", "03   THE FIELD GUIDE", "Fill your guide as\nyou solve the riddles.", "16 FISH  /  FOUR EXPEDITIONS", GOLD, media / "04-guide.png"),
        ("Fish Riddle\nClub", "THE OCEAN FIELD CLUB", "PT Deploy Dulu Pulang Nanti", "PLAY OFFLINE", GOLD, None),
    ]
    for i, plate in enumerate(plates):
        save(trailer_plate(*plate), stage / f"plate{i}.png")

    gameplay = RAW / "phone-gameplay.mp4"
    inputs = []
    for i in range(5):
        inputs += ["-loop", "1", "-framerate", "30", "-i", stage / f"plate{i}.png"]
    inputs += ["-stream_loop", "-1", "-i", audio / "tidal_notebook.ogg",
               "-i", audio / "anglerfish_clue.ogg", "-i", audio / "anglerfish_fact.ogg"]
    live = gameplay.is_file()
    if live:
        inputs += ["-i", gameplay]
    durations = (3, 7, 9, 5, 4)
    filters = [
        f"[{i}:v]trim=duration={duration},setpts=PTS-STARTPTS,format=yuv420p,setsar=1[p{i}]"
        for i, duration in enumerate(durations)
    ]
    if live:
        # The source phone playback uses the same layout and aspect ratio as the framed stills.
        filters += [
            "[8:v]trim=start=5:end=12,setpts=PTS-STARTPTS,fps=30,scale=477:851,setsar=1[v1]",
            "[8:v]trim=start=12:end=21,setpts=PTS-STARTPTS,fps=30,scale=477:851,setsar=1[v2]",
            "[p1][v1]overlay=1191:134[p1live]",
            "[p2][v2]overlay=1191:134[p2live]",
        ]
    inner = "[p1live][p2live]" if live else "[p1][p2]"
    filters += [
        f"[p0]{inner}[p3][p4]concat=n=5:v=1:a=0[v]",
        "[5:a]volume=0.52,atrim=duration=28,afade=t=out:st=26:d=2[m]",
        "[6:a]adelay=4500:all=1,volume=0.85[q]",
        "[7:a]adelay=11300:all=1,volume=0.85[a]",
        "[m][q][a]amix=inputs=3:normalize=0,alimiter=limit=0.94[mix]",
    ]
    run_ffmpeg(
        "-filter_complex_threads", "2", *inputs, "-filter_complex", ";".join(filters),
        "-map", "[v]", "-map", "[mix]", "-t", "28", "-c:v", "libx264",
        "-preset", "veryfast", "-threads", "4", "-crf", "19", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "160k",
        "-movflags", "+faststart", STORE / "fish-riddle-club-preview.mp4",
    )


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--feature", action="store_true")
    parser.add_argument("--screenshots", action="store_true")
    parser.add_argument("--trailer", action="store_true")
    args = parser.parse_args()
    if args.feature:
        feature()
    if args.screenshots:
        screenshots()
    if args.trailer:
        trailer()
