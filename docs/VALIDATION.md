# Release validation

NerfLens 0.2.0-rc3 is a GitHub prerelease. The following results describe the released code and its automated checks.

- Gradle check includes 35 dependency-free version/profile checks shipped with the source.
- The complete offline harness passes 324 assertions with Chromatic Tooltips and 308 without it.
- An additional 16 shipped certificate state checks verify exact ownership keys, missing/unknown/ready states, prerequisite revocation and no world record load/create/save/dirty operations.
- The integration harness includes 55 certificate tooltip assertions covering three version classifiers, individual certificate items, LV and higher requirements, compact/Shift states and unknown-version exclusion.
- The matrix covers ExtraNerf 1.0.1, 1.1.0 and 1.1.1, reading actual Forge mod metadata through isolated mod containers, reusing NEI handlers across profile changes, and checking that cached global frames do not leak between versions.
- Unknown, absent and unverified prerelease versions do not receive an invented built-in profile. Versioned user overrides stay scoped; unversioned overrides remain available.
- NBT-sensitive catalysts, native item identity fields, output-only frames, returned rods, resource reload and translation precedence are exercised.
- Changed future English wording and missing owner text do not receive old Chinese fallback text.
- Ten comparison-card boundary checks cover valid scoped records with empty inputs, fluid-only inputs, zero-count tools and null slots, plus ordinary quantity comparisons and native-stack preservation.
- All four production client Mixins are applied to actual NEI RecipeWidget/GuiRecipe and Minecraft Locale/StringTranslate bytecode. The comparison footer reserve, draw, tooltip and click hooks are checked.
- The installed GTNH mods expose all 135 referenced selector fields and 17 direct interface contracts checked by the release audit.
- Two existing native certification save records were parsed read-only to verify the `certifiedTiers` schema. Private save records are not redistributed.
- The source ZIP is built without a `.git` directory and its generated version and runtime artifact are checked against the release.

The harness uses real Minecraft, NEI and optional Chromatic interfaces with isolated GregTech/Thaumcraft registries. Full in-game visuals, processing in real saves and steam-consumption readouts are outside these automated checks.

## Reproduction

Build normally to run the shipped compatibility checks. The source distribution also includes `verification/README.md`, which describes the optional complete offline harness. It requires your own GTNH mods and installed JDKs; no dependency JARs are redistributed.
