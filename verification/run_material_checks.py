from pathlib import Path
import json
import os
import shutil
import subprocess
import sys

WORK = Path(__file__).resolve().parent
ROOT = WORK / "tooltip-check"
PROJECT = Path(os.environ.get("NERFLENS_TEST_PROJECT", str(WORK.parent)))
MODS = Path(os.environ["NERFLENS_TEST_MODS"])
JAVA8 = Path(os.environ["NERFLENS_JAVA8_HOME"])
EXE = ".exe" if os.name == "nt" else ""
ROOT.joinpath("classes").mkdir(parents=True, exist_ok=True)
dependencies = [line.strip() for line in (WORK / "frame-check-classpath.txt").read_text(encoding="utf-8").splitlines()
                if line.strip() and "jabel-javac-plugin" not in line]
base = [ROOT / "classes", PROJECT / "build/libs/nerflens-0.2.0-rc2-dev.jar",
        PROJECT / "build/rfg/recompiled_minecraft-1.7.10.jar", *dependencies]
chromatic = MODS / "chromatictooltips-1.0.36-GTNH.jar"

db = json.loads((PROJECT / "src/main/resources/assets/nerflens/nerfs.json").read_text(encoding="utf-8"))
keys = set()
def walk(value):
    if isinstance(value, list):
        for child in value:
            walk(child)
    elif isinstance(value, dict):
        for field, key_prefix in (("gt", ""), ("nh", "nh:"), ("ic2", "ic2:")):
            if field in value:
                keys.add(key_prefix + value[field])
        if "fluidGt" in value:
            keys.add("fluid:" + value["fluidGt"] + ":" + value.get("phase", "fluid"))
        if "bwMeta" in value:
            keys.add("bw-meta:" + str(value["bwMeta"]) + ":" + value.get("prefix", "dust"))
        if "material" in value:
            keys.add(value["material"] + ":" + value.get("prefix", "dust"))
        for child in value.values():
            walk(child)
walk(db)
(ROOT / "unavailable-gt-keys.txt").write_text("\n".join(sorted(keys)), encoding="utf-8")

def run(command, log):
    process = subprocess.run(list(map(str, command)), cwd=ROOT, capture_output=True,
                             text=True, encoding="utf-8", errors="replace")
    output = process.stdout + process.stderr
    (ROOT / log).write_text(output, encoding="utf-8")
    print("Exit:", process.returncode, "log:", log, flush=True)
    print(output[-5500:], flush=True)
    if process.returncode:
        sys.exit(process.returncode)

run([JAVA8 / ("bin/javac" + EXE), "-J-Dfile.encoding=UTF-8", "-encoding", "UTF-8", "-proc:none", "-cp",
     os.pathsep.join(map(str, [*base, chromatic])), "-d", ROOT / "classes",
     *sorted((ROOT / "src").rglob("*.java"))], "compile.log")
for mode in ("with-chromatic", "without-chromatic"):
    config = ROOT / ("config-" + mode)
    config.mkdir(exist_ok=True)
    override = config / "nerflens/user_nerfs.json"
    if override.is_file():
        override.unlink()
    classpath = base + ([chromatic] if mode == "with-chromatic" else [])
    run([JAVA8 / ("bin/java" + EXE), "-Dfile.encoding=UTF-8", "-cp", os.pathsep.join(map(str, classpath)),
         "HeadlessLaunch", config, ROOT / "unavailable-gt-keys.txt", mode], mode + ".log")
