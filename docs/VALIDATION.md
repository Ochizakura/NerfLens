# Release validation

NerfLens 0.2.0 promotes the verified rc6 implementation and display data to a stable release. No gameplay or display algorithm changes are introduced by this promotion.

- Offline Gradle build, Spotless and Checkstyle; 51 version/profile assertions and 16 certificate-state assertions.
- Integration harness: 794 assertions with Chromatic Tooltips and 778 without it, including 74 native solar-oracle assertions, 46 display regressions, 207 catalog-scope assertions, 73 certificate-tooltip assertions and 10 comparison-card boundary checks per environment.
- Four display profiles contain 582 version-scoped recipe records. Older profile resource bytes remain frozen.
- Live GTNH 2.9.0 registry identities corrected 55 exact selectors and the TPV suffix. Eleven client-native solar routes were captured: seven affected routes match and four unchanged Silane routes remain excluded.
- Production Mixin probes cover NEI frame/tooltip hooks, comparison footer hooks, Minecraft translation hooks and certificate registration. No forbidden world load/write calls are present.
- Chinese fallback precedence, English-reference matching, resource reload, native generated material references and owner-version boundaries are checked.

The optional harness uses real Minecraft/NEI interfaces with isolated GT/BW/Thaumcraft registries and a Forestry API fixture. The native solar fixture records the actual client item IDs, metadata, ore identities, quantities and container layouts. This does not claim automated processing of every machine in a real save.

Reproduction: build normally, generate the local classpath with `verification/export-classpath.gradle`, and follow `verification/README.md` with your own GTNH mods and JDKs.
