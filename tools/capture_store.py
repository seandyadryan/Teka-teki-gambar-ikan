"""Capture Fish Riddle Club on a dedicated, disposable Android QA emulator."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / ".capture"
PACKAGE = "com.deploydulupulangnanti.tekatekigambarikan"
PROFILES = {
    "phone": ("1080x1920", "420"),
    "tablet-7-inch": ("1920x1080", "320"),
    "tablet-10-inch": ("2560x1440", "320"),
    "desktop": ("2560x1440", "256"),
}
FISH = json.loads((ROOT / "app/src/main/assets/riddles.json").read_text(encoding="utf-8"))


class Emulator:
    def __init__(self, adb, serial):
        if not re.fullmatch(r"emulator-\d+", serial):
            raise ValueError("Use a dedicated emulator. This script clears the app's data.")
        self.cmd = [str(adb), "-s", serial]

    def run(self, *args):
        return subprocess.check_output([*self.cmd, *args], timeout=100)

    def shell(self, *args):
        return self.run("shell", *args)

    def resize(self, profile):
        size, density = PROFILES[profile]
        self.shell("wm", "size", size)
        self.shell("wm", "density", density)
        time.sleep(0.8)

    def configure(self, profile):
        self.resize(profile)
        self.shell("settings", "put", "system", "accelerometer_rotation", "0")
        self.shell("input", "keyevent", "KEYCODE_WAKEUP")
        self.shell("wm", "dismiss-keyguard")

    def launch(self, clear=False):
        if clear:
            result = self.shell("pm", "clear", PACKAGE)
            if b"Success" not in result:
                raise RuntimeError(result)
        self.shell("am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
        time.sleep(0.7)

    def locate(self, label):
        self.shell("uiautomator", "dump", "/data/local/tmp/fish-ui.xml")
        xml = self.run("exec-out", "cat", "/data/local/tmp/fish-ui.xml")
        tree = ET.fromstring(xml)
        for node in tree.iter("node"):
            text = node.attrib.get("text", "")
            description = node.attrib.get("content-desc", "")
            if label in (text, description) or label in description.split(", ") or label in text.split(", "):
                numbers = [int(n) for n in re.findall(r"\d+", node.attrib["bounds"])]
                x1, y1, x2, y2 = numbers
                if x2 > x1 and y2 > y1:
                    return ((x1+x2)//2, (y1+y2)//2)
        candidates = [n.attrib["text"] for n in tree.iter("node") if n.attrib.get("text")]
        raise RuntimeError(f"{label!r} missing on screen. Visible text: {candidates}")

    def tap(self, point):
        self.shell("input", "tap", str(point[0]), str(point[1]))
        time.sleep(0.36)

    def click(self, label):
        point = self.locate(label)
        self.tap(point)
        return point

    def capture(self, profile, name):
        path = OUT / profile / (name + ".png")
        path.parent.mkdir(parents=True, exist_ok=True)
        data = self.run("exec-out", "screencap", "-p")
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            raise RuntimeError(f"No screenshot at {profile}/{name}")
        path.write_bytes(data)


def play_profile(emu, profile, full_qa):
    emu.configure(profile)
    emu.launch(clear=True)
    start = emu.locate("Start diving")
    emu.capture(profile, "00-cover")
    emu.tap(start)
    points = {"start": start}
    emu.locate("Which fish carries its own lamp?")
    emu.capture(profile, "01-clue")
    points["listen"] = emu.click("Hear clue")
    if full_qa:
        emu.click("Lanternfish")
        emu.locate("Try again. Look closely at the picture.")
    points["correct"] = emu.click("Anglerfish")
    emu.locate("SPECIMEN IDENTIFIED")
    emu.capture(profile, "02-fact")
    points["next"] = emu.click("Next riddle")
    emu.locate(FISH[1]["question"])
    emu.capture(profile, "03-next")
    for index, fish in enumerate(FISH[1:4], 1):
        emu.click(fish["name"])
        emu.click("See dive summary" if index == 3 else "Next riddle")
    emu.locate("EXPEDITION COMPLETE")
    emu.capture(profile, "05-summary")
    emu.click("Back to expeditions")
    emu.click("Field guide")
    emu.locate("4 OF 16 DISCOVERED")
    emu.capture(profile, "04-guide")
    emu.click("Anglerfish")
    emu.locate(FISH[0]["fact"])
    emu.click("Close")
    if full_qa:
        emu.click("Settings")
        emu.click("Music")
        emu.click("Narration")
        emu.shell("am", "force-stop", PACKAGE)
        emu.launch()
        emu.click("Field guide")
        emu.locate("4 OF 16 DISCOVERED")
    print(f"Captured and exercised {profile}.", flush=True)
    return points


def capture_large_profiles(emu, profiles):
    primary = profiles[0]
    emu.configure(primary)
    emu.launch(clear=True)

    def across_profiles(name):
        emu.capture(primary, name)
        for profile in profiles[1:]:
            emu.resize(profile)
            emu.capture(profile, name)
        if len(profiles) > 1:
            emu.resize(primary)

    emu.click("Start diving")
    emu.locate(FISH[0]["question"])
    across_profiles("01-clue")
    emu.click(FISH[0]["name"])
    emu.locate("SPECIMEN IDENTIFIED")
    across_profiles("02-fact")
    emu.click("Next riddle")
    across_profiles("03-next")
    for index, fish in enumerate(FISH[1:4], 1):
        emu.click(fish["name"])
        emu.click("See dive summary" if index == 3 else "Next riddle")
    emu.click("Back to expeditions")
    emu.click("Field guide")
    emu.locate("4 OF 16 DISCOVERED")
    across_profiles("04-guide")
    print(f"Captured live layouts for {', '.join(profiles)}.", flush=True)

def record_phone(emu, points):
    emu.configure("phone")
    emu.launch(clear=True)
    remote = "/data/local/tmp/fish-preview.mp4"
    local = OUT / "phone-gameplay.mp4"
    recording = subprocess.Popen([
        *emu.cmd, "shell", "screenrecord", "--size", "540x960", "--bit-rate", "5000000",
        "--time-limit", "25", remote,
    ])
    started = time.monotonic()
    for moment, action in [(3.5, "start"), (6, "listen"), (11, "correct"), (18.5, "next")]:
        time.sleep(max(0, started + moment - time.monotonic()))
        emu.tap(points[action])
    if recording.wait(timeout=30):
        raise RuntimeError("Android screenrecord failed")
    emu.run("pull", remote, str(local))
    print(f"Recorded {local.name}.", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    default_sdk = os.environ.get("ANDROID_HOME", Path.home() / "AppData/Local/Android/Sdk")
    parser.add_argument("--adb", type=Path, default=Path(default_sdk) / "platform-tools/adb.exe")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--profile", choices=PROFILES, action="append")
    parser.add_argument("--video", action="store_true")
    args = parser.parse_args()
    emu = Emulator(args.adb, args.serial)
    profiles = args.profile or list(PROFILES)
    if "phone" in profiles:
        points = play_profile(emu, "phone", full_qa=True)
        if args.video:
            try:
                record_phone(emu, points)
            except (RuntimeError, subprocess.SubprocessError) as error:
                print(f"Screen recording unavailable: {error}", flush=True)
    large = [profile for profile in profiles if profile != "phone"]
    if large:
        capture_large_profiles(emu, large)

if __name__ == "__main__":
    main()
