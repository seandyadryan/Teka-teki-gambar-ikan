"""Check that every advertised language has complete offline game content."""

from collections import Counter
import json
from pathlib import Path
import re
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1] / "app/src/main"
RES = ROOT / "res"
ASSETS = ROOT / "assets"
NAME = "{http://schemas.android.com/apk/res/android}name"
PLACEHOLDERS = re.compile(r"%\d+\$[0-9]*[ds]")


def read_strings(path):
    return {item.attrib["name"]: item for item in ElementTree.parse(path).findall("string")}


def localized_strings():
    base = read_strings(RES / "values/strings.xml")
    expected = {
        key: Counter(PLACEHOLDERS.findall(item.text))
        for key, item in base.items()
        if item.get("translatable") != "false"
    }
    seen = set()
    for path in RES.glob("values*/strings.xml"):
        strings = read_strings(path)
        language = strings.get("language_tag")
        if language is None:
            continue
        tag = language.text
        assert tag not in seen, f"Duplicate resources for {tag}"
        seen.add(tag)
        localized = {
            key: Counter(PLACEHOLDERS.findall(item.text))
            for key, item in strings.items()
            if item.get("translatable") != "false"
        }
        assert localized == expected, f"String keys or format arguments differ for {tag}"
    return seen


def check_catalogs(languages):
    catalog = json.loads((ASSETS / "riddles.json").read_text(encoding="utf-8"))
    ids = {fish["id"] for fish in catalog}
    assert len(catalog) == len(ids) == 16
    names = {fish["name"] for fish in catalog}
    distractors = {name for fish in catalog for name in fish["choices"]} - names

    files = {path.stem: path for path in (ASSETS / "translations").glob("*.json")}
    assert files.keys() == languages - {"en"}, "Translation file set differs from supported languages"

    for tag, path in files.items():
        translation = json.loads(path.read_text(encoding="utf-8"))
        fish = translation["fish"]
        other_names = translation["otherNames"]
        assert fish.keys() == ids, f"Fish catalog differs for {tag}"
        assert other_names.keys() == distractors, f"Distractor names differ for {tag}"
        labels = dict(other_names)
        for original in catalog:
            entry = fish[original["id"]]
            assert all(entry[field].strip() for field in ("name", "question", "fact", "habitat")), tag
            labels[original["name"]] = entry["name"]
        for original in catalog:
            choices = original["choices"]
            assert choices.count(original["name"]) == 1 and len(choices) == 4
            displayed = {labels[choice].strip().casefold() for choice in choices}
            assert len(displayed) == 4, f"Duplicate answers for {tag}/{original['id']}"


def main():
    languages = {item.attrib[NAME] for item in ElementTree.parse(RES / "xml/app_locales.xml").findall("locale")}
    kotlin = next((ROOT / "java").rglob("AppLanguage.kt")).read_text(encoding="utf-8")
    picker = re.findall(r'AppLanguage\("([a-zA-Z-]+)",', kotlin)
    assert len(picker) == len(languages) and set(picker) == languages, "Language picker differs from Android locale config"
    assert localized_strings() == languages, "Android string locales differ from locale config"
    check_catalogs(languages)
    print(f"Verified UI strings and 16 riddles across {len(languages)} locales.")


if __name__ == "__main__":
    main()
