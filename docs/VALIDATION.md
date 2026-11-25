# Release validation

NerfLens 0.3.1 supports the official ExtraNerf 1.3.0 source tag at `825923725f87dee6bb2ae32de2088135888e0563`.

- Offline Gradle build, Spotless and Checkstyle passed; 65 version/profile assertions and 16 certificate-state assertions.
- Integration harness passed 1,376 assertions with Chromatic Tooltips and 1,360 without it.
- Six profiles contain 937 version-scoped recipe records. The 1.0.1/1.1.0/1.1.1/1.2.0/1.2.1 files are frozen; the 1.3.0 profile contains 104 descriptions and 178 compare records.
- New 1.3.0 routes and native language references pass 49 assertions: zero-count Radox Polymer Pulp and Kevlar Dust, local input/output frames, exact QFT output quantities, iron-family part identities, Supercapacitor and Quest Book tooltips, Brainrotter sub-tick wording, three changed-meaning variants and generated-owner bounds.
- The 1.2.1 route and language checks keep their 254 assertions, the catalog scope audit covers 937 records with 345 assertions, the native solar oracle keeps 148 and certificate tooltips 109.
- Production Mixin bytecode probes passed NEI RecipeWidget, GuiRecipe, Minecraft Locale/StringTranslate, certificate registration and the absence of forbidden world load/write calls.

The harness uses real Minecraft/NEI/Chromatic APIs with isolated GregTech/Thaumcraft registries and a Forestry API fixture. The 1.3.0 selector identities (`multimachine.supercapacitor`, `questbook:ItemQuestBook`, iron-family ore names) were read from the installed GTNH 2.9.1 jars and the official ExtraNerf 1.3.0 source and language files. This does not claim a real-save machine-processing run or visual acceptance of the new JAR in the current save.

Reproduction: build normally, generate the local classpath with `verification/export-classpath.gradle`, and follow `verification/README.md` with your own GTNH mods and JDKs.
