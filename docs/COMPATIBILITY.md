# Version compatibility

One NerfLens JAR supports the following verified display profiles in GTNH 2.9.0, Minecraft 1.7.10 / Forge 1614:

| ExtraNerf | Descriptions | Compare records | Selection |
| --- | ---: | ---: | --- |
| 1.0.1 | 71 | 123 | Automatic |
| 1.1.0 | 91 | 140 | Automatic |
| 1.1.1 | 91 | 143 | Automatic |
| 1.2.0 | 100 | 176 | Automatic |

The ExtraNerf version comes from Forge's loaded mod metadata. Leading `v` and build metadata such as `1.1.1+build.42` are recognized. Unverified prereleases, unrecognized releases and an absent ExtraNerf do not receive a guessed built-in profile. User-provided display records remain available.

The older profile preserves its Gallium changes and its original high-tier casing material costs. The 1.1.0 profile includes the motor, heating-coil and industrial recipe changes, but does not claim the 1.1.1 Livingrock hammer route or metal transmutation changes. The 1.1.1 profile is frozen in `profiles/1.1.1.json` and retains its original 91 descriptions and 143 compare records.

The 1.2.0 profile adds the Electric Iron Bath, its Wrought/Cast/Pig Iron alloy feeds, revised industrial Steel routes, Kevlar drones, Botania HV glass, the Universal Wireless Terminal input, Tool Forge removals, Crystal Blood conversion and the other verified 1.2.0 changes. It follows runtime code when upstream prose disagrees: the Ilmenite EBF recipe outputs Cast Iron in `BlastFurnaceNerf.java`, so NerfLens does not repeat the stale Wrought Iron table entry.

Global material frames follow the selected profile. Changing the database invalidates recipe matches and item classification, including when the same NEI handler is reused. Output-only changes retain their local recipe frames; the legacy high-tier casing finishing step does not create a global material frame.

## User data

`config/nerflens/user_nerfs.json` replaces built-in records by ID. An optional top-level `extraNerfVersion` restricts that file to a specific version; an unversioned file applies to all profiles.

```json
{
  "schemaVersion": 3,
  "extraNerfVersion": "1.2.0",
  "entries": [],
  "recipes": []
}
```

## Certification tooltips

NerfLens 0.2.0 displays read-only certification state in machine and certificate item tooltips. Machine requirements use the installed ExtraNerf classifier, including its legacy obfuscated API in 1.0.1. Individual certificate items show their own ownership bit; machines check their full prerequisite chain. ExtraNerf 1.2.0 multiblocks inherit the installed classifier's energy/dynamo-hatch tier rule, while the Industrial Apiary remains HV and the Ice Cream Machine remains exempt.

Singleplayer reads only an already-loaded record in the integrated server's MapStorage, on its server thread, every half second. It serializes that existing record into a temporary NBT object without loading, creating, saving or dirtying world data. World/server changes invalidate the snapshot. Remote or unavailable state is shown as unknown.

Default tooltips show ready or missing requirements. Shift shows each prerequisite independently, so revoking Steam does not incorrectly label a still-owned LV certificate as lost. Native certificate activation, exams and scoring are unchanged.

## Verification boundaries

The release has deterministic profile checks, actual Forge metadata checks, real Minecraft/NEI/Chromatic API checks with isolated GregTech and Thaumcraft registries, and production Mixin bytecode checks. This does not claim three complete game runs or upstream processing tests in real saves.

In-game steam-consumption readouts are outside the automated verification scope. Check machine readouts and actual consumption in the running game when evaluating steam usage.
