# Release-candidate validation

NerfLens 0.2.0-rc2 is prepared as a GitHub prerelease.

- Gradle check includes 35 dependency-free version/profile checks shipped with the source.
- The complete offline harness passes 314 assertions with Chromatic Tooltips and 298 without it.
- An additional 16 shipped certificate state checks verify exact ownership keys, missing/unknown/ready states, prerequisite revocation and no world record load/create/save/dirty operations.
- The integration harness includes 55 certificate tooltip assertions covering three version classifiers, individual certificate items, LV and higher requirements, compact/Shift states and unknown-version exclusion.
- The matrix covers ExtraNerf 1.0.1, 1.1.0 and 1.1.1, reading actual Forge mod metadata through isolated mod containers, reusing NEI handlers across profile changes, and checking that cached global frames do not leak between versions.
- Unknown, absent and unverified prerelease versions do not receive an invented built-in profile. Versioned user overrides stay scoped; unversioned overrides remain available.
- NBT-sensitive catalysts, native item identity fields, output-only frames, returned rods, resource reload and translation precedence are exercised.
- Changed future English wording and missing owner text do not receive old Chinese fallback text.
- Production Mixins are applied to actual NEI RecipeWidget and Minecraft Locale/StringTranslate bytecode.

The harness uses real Minecraft, NEI and optional Chromatic interfaces with isolated GregTech/Thaumcraft registries. It does not claim three complete client runs, real-save processing tests or full visual acceptance. The steam 5:1 display report was deferred and is not claimed fixed.

## Reproduction

Build normally to run the shipped compatibility checks. The source distribution also includes `verification/README.md`, which describes the optional complete offline harness. It requires your own GTNH mods and installed JDKs; no dependency JARs are redistributed.
