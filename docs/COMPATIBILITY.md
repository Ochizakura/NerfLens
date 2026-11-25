# Version compatibility

One NerfLens JAR supports the following verified display profiles in GTNH 2.9.0 and 2.9.1, Minecraft 1.7.10 / Forge 1614:

| ExtraNerf | Descriptions | Compare records | Selection |
| --- | ---: | ---: | --- |
| 1.0.1 | 71 | 123 | Automatic |
| 1.1.0 | 91 | 140 | Automatic |
| 1.1.1 | 91 | 143 | Automatic |
| 1.2.0 | 100 | 176 | Automatic |
| 1.2.1 | 102 | 177 | Automatic |
| 1.3.0 | 104 | 178 | Automatic |

The ExtraNerf version comes from Forge's loaded mod metadata. Leading `v` and build metadata such as `1.1.1+build.42` are recognized. Unverified prereleases, unrecognized releases and an absent ExtraNerf do not receive a guessed built-in profile. User-provided display records remain available.

The older profile preserves its Gallium changes and its original high-tier casing material costs. The 1.1.0 profile includes the motor, heating-coil and industrial recipe changes, but does not claim the 1.1.1 Livingrock hammer route or metal transmutation changes. The 1.1.1 profile is frozen in `profiles/1.1.1.json` and retains its original 91 descriptions and 143 compare records.

The 1.2.0 profile adds the Electric Iron Bath, its Wrought/Cast/Pig Iron alloy feeds, revised industrial Steel routes, Kevlar drones, Botania HV glass, the Universal Wireless Terminal input, Tool Forge removals, Crystal Blood conversion and the other verified 1.2.0 changes. It follows runtime code when upstream prose disagrees: the Ilmenite EBF recipe outputs Cast Iron in `BlastFurnaceNerf.java`, so NerfLens does not repeat the stale Wrought Iron table entry.

Global material frames follow the selected profile. Changing the database invalidates recipe matches and item classification, including when the same NEI handler is reused. Output-only changes retain their local recipe frames; the legacy high-tier casing finishing step does not create a global material frame.

The 1.2.0 profile is frozen byte-for-byte in `profiles/1.2.0.json`. The independent 1.2.1 profile changes the Bacterial Vat's added full Mineral Mix dust to one Small Pile (1/4 full dust), adds 64 Kevlar Combs to the verified QFT plastics route and updates Kevlar parent/specialty, Iron Bath structure and wireless-cover certification descriptions. Exact QFT materials and all four output quantities scope the new match; only the intended plastics receive local output frames. Other polymer routes and unchanged byproducts do not inherit those frames.

Changed Iron Bath language keys keep both verified English meanings. Reference text selects the corresponding translation, so 1.2.1 chimney-placement text does not replace 1.2.0 side-platform wording. Native, resource-pack and dynamic translations retain priority. Generated material names accept only the verified 1.2.0/1.2.1 owners and unchanged native English.

The 1.2.0 and 1.2.1 profiles are frozen byte-for-byte in `profiles/1.2.0.json` and `profiles/1.2.1.json`. The independent 1.3.0 profile adds the two zero-count QFT catalysts (Radox Polymer Pulp on the 64-comb route, Kevlar Dust on the 24-comb route), the full GregTech part sets and the stepped tapping/persistence rules of the Iron Bath metals, the Lapotronic Supercapacitor's laser-hatch certification tiers, sub-tick Brainrotter parallels and the party-wide quest completion reward. Zero-count inputs are matched as quantity 0 and marked as non-consumed additions, so routes without them keep their previous matches.

Slight name alignment follows the installed Chinese language files: 兰波顿超级电容库 (Lapotronic Supercapacitor), 量子操纵者 (Quantum Force Transformer), 任务书 (Quest Book), 炉渣 (Slag) and the 锻铁 / 铸铁 / 生铁 / 过渗碳铁 metal names. Older profiles keep their released bytes; only the 1.3.0 profile carries the aligned wording.

## User data

`config/nerflens/user_nerfs.json` replaces built-in records by ID. An optional top-level `extraNerfVersion` restricts that file to a specific version; an unversioned file applies to all profiles.

```json
{
  "schemaVersion": 3,
  "extraNerfVersion": "1.3.0",
  "entries": [],
  "recipes": []
}
```

## Certification tooltips

NerfLens 0.3.1 displays read-only certification state in machine and certificate item tooltips. Machine requirements use the installed ExtraNerf classifier, including its legacy obfuscated API in 1.0.1. Individual certificate items show their own ownership bit; machines check their full prerequisite chain. ExtraNerf 1.2.0/1.2.1 multiblocks inherit the installed classifier's energy/dynamo-hatch tier rule, while the Industrial Apiary remains HV and the Ice Cream Machine remains exempt.

Singleplayer reads only an already-loaded record in the integrated server's MapStorage, on its server thread, every half second. It serializes that existing record into a temporary NBT object without loading, creating, saving or dirtying world data. World/server changes invalidate the snapshot. Remote or unavailable state is shown as unknown.

Default tooltips show ready or missing requirements. Shift shows each prerequisite independently, so revoking Steam does not incorrectly label a still-owned LV certificate as lost. Native certificate activation, exams and scoring are unchanged.

## Verification boundaries

The release has deterministic profile checks, actual Forge metadata checks, real Minecraft/NEI/Chromatic API checks with isolated GregTech and Thaumcraft registries, and production Mixin bytecode checks. This does not claim three complete game runs or upstream processing tests in real saves.

In-game steam-consumption readouts are outside the automated verification scope. Check machine readouts and actual consumption in the running game when evaluating steam usage.

GTNH 2.9.1 display-interface verification uses the official NEI 2.8.175-GTNH development API and installed native JAR, Chromatic Tooltips 1.0.38-GTNH, and production Mixin bytecode hooks. It does not substitute for loading the upgraded existing save or processing machines in it. Maintenance requirements are in [MAINTENANCE_RULES.md](MAINTENANCE_RULES.md).
