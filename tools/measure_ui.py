import argparse
import json
import pathlib
import re
import subprocess
import time

__author__ = "carunodev"


def adb(serial, *args):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, timeout=45)
    if result.returncode:
        raise RuntimeError(result.stderr or result.stdout)
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="37151FDJH0050Y")
    parser.add_argument("--package", default="app.orpheon.beta.dev")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if adb(args.serial, "get-state").strip() != "device":
        raise RuntimeError("Device is not authorized")
    package = adb(args.serial, "shell", "dumpsys", "package", args.package)
    adb(args.serial, "shell", "dumpsys", "gfxinfo", args.package, "reset")
    for _ in range(2):
        adb(args.serial, "shell", "input", "swipe", "540", "1750", "540", "650", "400")
        adb(args.serial, "shell", "input", "swipe", "540", "650", "540", "1750", "400")
    time.sleep(1)
    raw = adb(args.serial, "shell", "dumpsys", "gfxinfo", args.package)
    metrics = {}
    for line in raw.splitlines():
        if any(key in line for key in ("Total frames rendered:", "Janky frames:", "percentile:", "Number Slow UI thread:")):
            key, value = line.strip().split(":", 1)
            metrics[key] = value.strip()
    if not re.search(r"Total frames rendered:\s*[1-9]", raw):
        raise RuntimeError("No rendered frames were collected")
    report = {"author": __author__, "package": args.package, "serial": args.serial,
              "debuggable": bool(re.search(r"flags=\[[^\]]*DEBUGGABLE", package)),
              "metrics": metrics, "raw_gfxinfo": raw}
    output = pathlib.Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "raw_gfxinfo"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
