# Offline integration harness

Build the project first. Run the wrapper with the init script to generate your own local classpath:

    gradlew.bat -I verification/export-classpath.gradle nerflensVerificationClasspath --no-configuration-cache

Set NERFLENS_TEST_MODS to the GTNH 2.9.0 client mods directory and NERFLENS_JAVA8_HOME to your Java 8 JDK.
NERFLENS_TEST_PROJECT optionally selects a prebuilt matching source checkout; it otherwise uses the source root.
Run these scripts from the source root:

    python verification/run_material_checks.py
    python verification/run_material_mixin_probe.py

The harness uses your own Minecraft/NEI/Chromatic dependency artifacts and isolated GregTech/Thaumcraft registries.
The two runtime modes exercise the optional Chromatic bridge. It does not run a real save or validate full machine processing.
The compact language fixture contains only the reference strings used by the shipped fallback records.
Generated classpath files, classes, logs and test configs are local output, not release source.
