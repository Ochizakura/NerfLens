# Version compatibility

One NerfLens JAR supports the following verified display profiles in GTNH 2.9.0, Minecraft 1.7.10 / Forge 1614:

| ExtraNerf | Descriptions | Compare records | Selection |
| --- | ---: | ---: | --- |
| 1.0.1 | 71 | 123 | Automatic |
| 1.1.0 | 91 | 140 | Automatic |
| 1.1.1 | 91 | 143 | Automatic |

The ExtraNerf version comes from Forge's loaded mod metadata. Leading `v` and build metadata such as `1.1.1+build.42` are recognized. Unverified prereleases, unrecognized releases and an absent ExtraNerf do not receive a guessed built-in profile. User-provided display records remain available.

The older profile preserves its Gallium changes and its original high-tier casing material costs. The 1.1.0 profile includes the motor, heating-coil and industrial recipe changes, but does not claim the 1.1.1 Livingrock hammer route or metal transmutation changes. The 1.1.1 profile includes those changes and the expanded cutter rule.

Global material frames follow the selected profile. Changing the database invalidates recipe matches and item classification, including when the same NEI handler is reused. Output-only changes retain their local recipe frames; the legacy high-tier casing finishing step does not create a global material frame.

## User data

`config/nerflens/user_nerfs.json` replaces built-in records by ID. An optional top-level `extraNerfVersion` restricts that file to a specific version; an unversioned file applies to all profiles.

```json
{
  "schemaVersion": 3,
  "extraNerfVersion": "1.1.1",
  "entries": [],
  "recipes": []
}
```

## Certification tooltips

Read-only certification state has been restored in 0.2.0-rc2. Machine requirements use the installed ExtraNerf classifier, including its legacy obfuscated API in 1.0.1. Individual certificate items show their own ownership bit; machines check their full prerequisite chain.

Singleplayer reads only an already-loaded record in the integrated server's MapStorage, on its server thread, every half second. It serializes that existing record into a temporary NBT object without loading, creating, saving or dirtying world data. World/server changes invalidate the snapshot. Remote or unavailable state is shown as unknown.

Default tooltips show ready or missing requirements. Shift shows each prerequisite independently, so revoking Steam does not incorrectly label a still-owned LV certificate as lost. Native certificate activation, exams and scoring are unchanged.

## Verification boundaries

The release has deterministic profile checks, actual Forge metadata checks, real Minecraft/NEI/Chromatic API checks with isolated GregTech and Thaumcraft registries, and production Mixin bytecode checks. This does not claim three complete game runs or upstream processing tests in real saves.

The reported steam 5:1 display issue was deferred at the user's request. This release does not claim to fix the native steam gauge or that report.
