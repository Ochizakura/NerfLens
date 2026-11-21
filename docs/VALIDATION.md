# Release validation

NerfLens 0.2.1 supports the official ExtraNerf 1.2.1 source tag at `d064eaa3cbd54ef15abd75985662bca81aa192d1`. The installed official 1.2.1 JAR matches the upstream release SHA-256.

- Offline Gradle build, Spotless and Checkstyle passed; 62 version/profile assertions and 16 certificate-state assertions.
- Integration harness passed 1,228 assertions with Chromatic Tooltips and 1,212 without it.
- Five profiles contain 759 version-scoped recipe records. Four older profile files are frozen; the 1.2.1 profile contains 102 descriptions and 177 compare records.
- Native solar snapshots run for both 1.2.0 and 1.2.1: 148 assertions cover seven affected and four unchanged routes, normal and normalized item counts, local acid frames and empty-cell exclusions.
- New 1.2.1 routes and native language references pass 254 assertions, including Small Pile versus full dust, fluid amplification, 64 Comb identity/count, exact QFT output quantities, unchanged-route controls, cache invalidation, QFT controller tooltip, wireless-cover IDs, all 8 changed meanings and generated-owner bounds.
- Certificate tooltips pass 91 assertions across all five profiles. Full catalog scope checks, previous 46 display regressions and 10 comparison-card boundary assertions remain included.
- Production Mixin bytecode probes passed NEI RecipeWidget, GuiRecipe, Minecraft Locale/StringTranslate, certificate registration and the absence of forbidden world load/write calls.

The harness uses real Minecraft/NEI/Chromatic APIs with isolated GregTech/Thaumcraft registries and a Forestry API fixture. Solar snapshots were captured from the earlier real GTNH client; new 1.2.1 selectors and language references were verified against official release source and installed native JAR bytecode. This does not claim a new real-world machine-processing run or visual acceptance of the updated JAR in the current save.

Reproduction: build normally, generate the local classpath with `verification/export-classpath.gradle`, and follow `verification/README.md` with your own GTNH mods and JDKs.
