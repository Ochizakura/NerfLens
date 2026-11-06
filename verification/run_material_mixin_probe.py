from pathlib import Path
import json
import os
import shutil
import subprocess
import sys
import zipfile

WORK = Path(__file__).resolve().parent
ROOT = WORK / "nei-mixin-check"
PROJECT = Path(os.environ.get("NERFLENS_TEST_PROJECT", str(WORK.parent)))
MODS = Path(os.environ["NERFLENS_TEST_MODS"])
JAVA8 = Path(os.environ["NERFLENS_JAVA8_HOME"])
EXE = ".exe" if os.name == "nt" else ""
ROOT.joinpath("src").mkdir(parents=True, exist_ok=True)
ROOT.joinpath("classes").mkdir(exist_ok=True)
with zipfile.ZipFile(PROJECT / "build/libs/nerflens-0.2.0-rc2.jar") as jar:
    for name in ("TooltipHandler", "NerfLensMod", "CertTracker"):
        (ROOT / (name + ".class")).write_bytes(jar.read("nerflens/" + name + ".class"))
with zipfile.ZipFile(MODS / "NotEnoughItems-2.8.171-GTNH.jar") as jar:
    (ROOT / "RecipeWidget.class").write_bytes(jar.read("codechicken/nei/recipe/widget/RecipeWidget.class"))
with zipfile.ZipFile(PROJECT / "build/rfg/srg_merged_minecraft.jar") as jar:
    for name in ("net/minecraft/client/resources/Locale", "net/minecraft/util/StringTranslate"):
        (ROOT / (name.rsplit("/", 1)[1] + ".class")).write_bytes(jar.read(name + ".class"))
(ROOT / "mixins.nerflens.frameprobe.json").write_text(json.dumps({
    "required": True, "minVersion": "0.8.7", "package": "nerflens.mixins",
    "refmap": "mixins.nerflens.refmap.json", "compatibilityLevel": "JAVA_8",
    "client": ["MixinRecipeWidget", "MixinLocale", "MixinStringTranslate"], "injectors": {"defaultRequire": 1},
}), encoding="utf-8")
dependencies = [line.strip() for line in (WORK / "frame-check-classpath.txt").read_text(encoding="utf-8").splitlines()
                if line.strip() and not any(key in line for key in ("jabel-javac-plugin", "NotEnoughItems-", "patchedMc"))]
classpath = os.pathsep.join(map(str, [ROOT / "classes", ROOT, PROJECT / "build/libs/nerflens-0.2.0-rc2.jar",
    MODS / "NotEnoughItems-2.8.171-GTNH.jar", PROJECT / "build/rfg/srg_merged_minecraft.jar",
    PROJECT / "build/rfg/recompiled_minecraft-1.7.10.jar", *dependencies]))

def run(command, log):
    process = subprocess.run(list(map(str, command)), cwd=ROOT, capture_output=True,
                             text=True, encoding="utf-8", errors="replace")
    output = process.stdout + process.stderr
    (ROOT / log).write_text(output, encoding="utf-8")
    print("Exit:", process.returncode, "log:", log, flush=True)
    print(output[-3500:], flush=True)
    if process.returncode:
        sys.exit(process.returncode)

run([JAVA8 / ("bin/javac" + EXE), "-encoding", "UTF-8", "-proc:none", "-cp", classpath,
     "-d", ROOT / "classes", ROOT / "src/FrameMixinProbe.java"], "compile.log")
run([JAVA8 / ("bin/java" + EXE), "-Dfile.encoding=UTF-8", "-cp", classpath,
     "FrameMixinProbe", ROOT / "RecipeWidget.class"], "mixin-probe.log")
