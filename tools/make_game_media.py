"""Prepare the illustrated specimen plates, launcher icon, and offline audio."""
import argparse
import json
import math
from pathlib import Path
import subprocess
import tempfile
import wave

import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main"
STORE = ROOT / "play-store"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
SPECIMENS = json.loads((APP / "assets/riddles.json").read_text(encoding="utf-8"))


def build_art(sheet):
    if sheet:
        source = Image.open(sheet).convert("RGB")
        out = APP / "assets/fish"
        out.mkdir(parents=True, exist_ok=True)
        for index, fish in enumerate(SPECIMENS):
            row, col = divmod(index, 4)
            tile = source.crop((
                round(source.width * col / 4), round(source.height * row / 4),
                round(source.width * (col + 1) / 4), round(source.height * (row + 1) / 4),
            ))
            rgb = np.asarray(tile, dtype=np.float32)
            reference = np.median(rgb[:, [0, 1, -2, -1], :], axis=(0, 1))
            distance = np.linalg.norm(rgb - reference, axis=2)
            opaque = Image.fromarray(np.uint8(distance >= 18) * 255)
            mask = Image.new("L", (tile.width + 2, tile.height + 2))
            mask.paste(opaque, (1, 1))
            ImageDraw.floodfill(mask, (0, 0), 128)
            inside = np.array(mask)[1:-1, 1:-1] != 128
            alpha = np.where(inside, 1.0, np.clip((distance - 6) / 12, 0, 1))
            unmatted = (rgb - reference * (1 - alpha[:, :, None])) / np.maximum(alpha[:, :, None], 0.08)
            pixels = np.dstack((np.clip(unmatted, 0, 255), alpha * 255)).astype(np.uint8)
            art = Image.fromarray(pixels)
            art = art.crop(art.getchannel("A").getbbox())
            art.save(out / f"{fish['id']}.webp", quality=91, method=6)
    build_icon()


def icon_mark(width, proportion=1):
    canvas = Image.new("RGBA", (width, width))
    draw = ImageDraw.Draw(canvas)
    scale = width / 512 * proportion
    offset = (width - scale * 512) / 2

    def point(x, y):
        return (round(offset + x * scale), round(offset + y * scale))

    def polygon(points, color):
        draw.polygon([point(x, y) for x, y in points], fill=color)

    def oval(bounds, color):
        draw.ellipse([point(*bounds[:2]), point(*bounds[2:])], fill=color)

    def line(points, color, thickness):
        draw.line([point(x, y) for x, y in points], fill=color, width=round(thickness * scale), joint="curve")

    # A simplified anglerfish stamp. Curves use sampled quadratic segments.
    def curve(a, b, c):
        return [((1-t)**2*a[0] + 2*(1-t)*t*b[0] + t*t*c[0],
                 (1-t)**2*a[1] + 2*(1-t)*t*b[1] + t*t*c[1]) for t in np.linspace(0, 1, 35)]

    mint, moss, light, deep = "#aed9c3", "#58928b", "#f4c477", "#09232b"
    oval((57, 57, 455, 455), "#12363e")
    draw.ellipse([point(57, 57), point(455, 455)], outline="#376069", width=max(1, round(3 * scale)))
    polygon([(188, 289), (102, 235), (114, 288), (99, 352), (195, 330)], moss)
    line(curve((277, 242), (312, 115), (370, 166)), light, 9)
    oval((358, 159, 385, 186), light)
    polygon(
        curve((158, 284), (216, 214), (294, 230))
        + curve((294, 230), (368, 230), (385, 278))
        + [(317, 298), (383, 317)]
        + curve((383, 317), (327, 391), (231, 356))
        + curve((231, 356), (183, 341), (158, 315)),
        mint,
    )
    polygon([(321, 296), (340, 289), (338, 304)], "#fff0c9")
    polygon([(350, 289), (371, 283), (362, 301)], "#fff0c9")
    oval((318, 249, 339, 270), deep)
    oval((324, 252, 330, 258), "#fff0c9")
    line(curve((236, 261), (203, 292), (231, 330)), moss, 6)
    line(curve((250, 266), (220, 297), (242, 325)), moss, 4)
    return canvas


def build_icon():
    STORE.mkdir(parents=True, exist_ok=True)
    large = Image.new("RGBA", (1024, 1024), "#09232b")
    large.alpha_composite(icon_mark(1024))
    icon = large.resize((512, 512), Image.Resampling.LANCZOS)
    icon.save(STORE / "app-icon.png", optimize=True)
    foreground = APP / "res/drawable-nodpi"
    foreground.mkdir(parents=True, exist_ok=True)
    icon_mark(864, 0.70).resize((432, 432), Image.Resampling.LANCZOS).save(
        foreground / "launcher_foreground.png", optimize=True,
    )


def write_wave(path, signal, rate):
    pcm = np.clip(signal, -1, 1)
    with wave.open(str(path), "wb") as file:
        file.setnchannels(1 if pcm.ndim == 1 else pcm.shape[1])
        file.setsampwidth(2)
        file.setframerate(rate)
        file.writeframes((pcm * 32767).astype("<i2").tobytes())


def compose_music():
    rate = 44100
    beat = 60 / 106
    length = round(16 * 4 * beat * rate)
    signal = np.zeros((length, 2))
    noise = np.random.default_rng(8)

    def add(waveform, start, pan):
        indices = (np.arange(len(waveform)) + round(start * rate)) % length
        signal[indices, 0] += waveform * math.sqrt((1 - pan) / 2)
        signal[indices, 1] += waveform * math.sqrt((1 + pan) / 2)

    def note(midi, start, duration, volume, pan, timbre):
        t = np.arange(round(duration * rate)) / rate
        phase = 2 * np.pi * (440 * 2 ** ((midi - 69) / 12)) * t
        if timbre == "bass":
            sound = np.sin(phase) + 0.17 * np.sin(2 * phase)
            envelope = np.minimum(t / 0.024, 1) * np.exp(-t * 4.5)
        else:
            sound = np.sin(phase) + 0.30 * np.sin(2.007 * phase) * np.exp(-t * 9)
            sound += 0.10 * np.sin(3.985 * phase) * np.exp(-t * 17)
            envelope = np.minimum(t / 0.009, 1) * np.exp(-t * 5)
        taper = np.minimum((duration - t) / 0.04, 1)
        add(volume * sound * envelope * taper, start, pan)

    chords = [(48, 60, 64, 67, 71), (45, 57, 60, 64, 67),
              (41, 57, 60, 64, 67), (43, 55, 59, 62, 65)]
    melodies = [
        [(0.5, 76), (1.25, 79), (2.5, 83), (3.25, 81)],
        [(0, 79), (1.5, 76), (2.75, 72)],
        [(0.5, 74), (1.25, 76), (2.5, 79), (3.5, 84)],
        [(0, 83), (1.75, 79), (2.75, 74), (3.5, 71)],
        [(0.25, 72), (1, 76), (2.5, 79)],
        [(0.5, 81), (1.25, 79), (2.0, 76), (3.25, 72)],
        [(0.75, 76), (1.5, 74), (2.75, 72)],
        [(0, 71), (1.25, 74), (2.5, 67)],
    ]
    for bar in range(16):
        bass, *harmony = chords[bar % len(chords)]
        base = bar * 4
        for offset, pitch in [(0, bass), (1.75, bass + 7), (2.5, bass + 12)]:
            note(pitch, (base + offset) * beat, 0.55, 0.18, -0.1, "bass")
        for tick, pitch in zip((0.75, 1.5, 2.75, 3.5), harmony, strict=True):
            note(pitch, (base + tick) * beat, 0.58, 0.085, -0.4, "mallet")
        for tick, pitch in melodies[bar % len(melodies)]:
            note(pitch, (base + tick) * beat, 0.72, 0.23, 0.22, "mallet")
            note(pitch, (base + tick) * beat + 0.14, 0.42, 0.035, -0.37, "mallet")
        for tick in (0.5, 1.5, 2.5, 3.5):
            t = np.arange(int(rate * 0.11)) / rate
            grains = np.diff(noise.normal(0, 1, len(t) + 1))
            add(0.021 * grains * np.exp(-t * 48) * np.minimum(t / 0.004, 1), (base + tick) * beat, 0.40)
        for tick in (0, 2):
            t = np.arange(int(rate * 0.20)) / rate
            kick = np.sin(2 * np.pi * (45 * t + 2.6 * (1 - np.exp(-t * 26))))
            add(0.095 * kick * np.exp(-t * 29) * np.minimum(t / 0.005, 1), (base + tick) * beat, 0.0)
    return np.tanh(signal * 1.40), rate


def encode_audio(wav, destination, bitrate):
    subprocess.run([
        FFMPEG, "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav),
        "-c:a", "libvorbis", "-b:a", bitrate, str(destination),
    ], check=True)


def build_audio(args):
    out = APP / "assets/audio"
    out.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "media.wav"
        if args.music:
            write_wave(wav, *compose_music())
            encode_audio(wav, out / "tidal_notebook.ogg", "96k")
        if args.model:
            from kokoro_onnx import Kokoro

            import onnxruntime

            options = onnxruntime.SessionOptions()
            options.intra_op_num_threads = 2
            options.inter_op_num_threads = 1
            options.add_session_config_entry("session.intra_op.allow_spinning", "0")
            session = onnxruntime.InferenceSession(str(args.model), options, providers=["CPUExecutionProvider"])
            voice = Kokoro.from_session(session, str(args.voices))
            for fish in SPECIMENS:
                scripts = {"clue": fish["question"], "fact": f"{fish['name']}. {fish['fact']}"}
                for kind, words in scripts.items():
                    destination = out / f"{fish['id']}_{kind}.ogg"
                    if destination.is_file() and not args.replace:
                        continue
                    signal, rate = voice.create(words, voice="af_heart", speed=0.94, lang="en-us")
                    signal *= 0.88 / max(np.max(np.abs(signal)), 0.01)
                    write_wave(wav, signal, rate)
                    encode_audio(wav, destination, "44k")
                    print(destination.name, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sheet", type=Path)
    parser.add_argument("--art", action="store_true")
    parser.add_argument("--music", action="store_true")
    parser.add_argument("--model", type=Path)
    parser.add_argument("--voices", type=Path)
    parser.add_argument("--replace", action="store_true")
    args = parser.parse_args()
    if args.model and not args.voices:
        parser.error("--model also needs --voices")
    if args.art or args.sheet:
        build_art(args.sheet)
    if args.music or args.model:
        build_audio(args)


if __name__ == "__main__":
    main()
