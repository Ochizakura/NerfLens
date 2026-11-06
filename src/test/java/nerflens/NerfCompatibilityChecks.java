package nerflens;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;

/** Dependency-free checks shipped with the source distribution and run by Gradle check. */
public final class NerfCompatibilityChecks {

    private static int checks;

    private NerfCompatibilityChecks() {}

    private static void require(boolean result, String description) {
        checks++;
        if (!result) throw new AssertionError(description);
    }

    private static boolean recipe(String id) {
        return NerfDB.recipes()
            .stream()
            .anyMatch(value -> value.id.equals(id));
    }

    public static void main(String[] args) throws Exception {
        Field loggingSide = FMLRelaunchLog.class.getDeclaredField("side");
        loggingSide.setAccessible(true);
        loggingSide.set(null, Side.CLIENT);
        File directory = new File(args[0]);
        Path override = Paths.get(args[0], "nerflens", "user_nerfs.json");
        Files.createDirectories(override.getParent());
        Files.deleteIfExists(override);
        require(NerfCompatibility.profile("1.0.1") == NerfCompatibility.Profile.LEGACY, "legacy profile selection");
        require(NerfCompatibility.profile("v1.1.0") == NerfCompatibility.Profile.V110, "v-prefix version selection");
        require(
            NerfCompatibility.profile(" 1.1.1+build.42 ") == NerfCompatibility.Profile.V111,
            "build metadata selection");
        require(
            NerfCompatibility.profile("1.1.1-beta") == NerfCompatibility.Profile.NONE,
            "unverified prerelease stays unselected");
        require(
            NerfCompatibility.profile("1.2.0") == NerfCompatibility.Profile.NONE,
            "future version stays unselected");
        require(NerfCompatibility.profile(null) == NerfCompatibility.Profile.NONE, "missing mod stays unselected");
        require(
            NerfCompatibility.profile("NO-GIT-TAG-SET") == NerfCompatibility.Profile.NONE,
            "unknown local build stays unselected");

        for (String version : new String[] { "1.0.1", "1.1.0", "1.1.1" }) {
            NerfDB.load(directory, version);
            require(
                NerfDB.profileVersion()
                    .equals(version),
                "loaded profile identity: " + version);
            require(
                !NerfDB.entries()
                    .isEmpty()
                    && !NerfDB.recipes()
                        .isEmpty(),
                "profile data exists: " + version);
            require(recipe("sphalerite-electrolyzer") == version.equals("1.0.1"), "Gallium record is version-specific");
            require(
                recipe("mv-motor-magicnetized") == !version.equals("1.0.1"),
                "new motor record is version-specific");
            require(recipe("metal-duplication") == version.equals("1.1.1"), "metal duplication starts at 1.1.1");
            require(recipe("livingrock-plate-hammer") == version.equals("1.1.1"), "Livingrock route starts at 1.1.1");
            require(recipe("legacy-high-casing-uev") == version.equals("1.0.1"), "legacy output-only casing record");
        }
        NerfDB.load(directory, "1.0.1");
        NerfDB.Entry legacyCasings = NerfDB.entries()
            .stream()
            .filter(value -> value.id.equals("tooltip-high-casings-legacy"))
            .findFirst()
            .get();
        require(
            !legacyCasings.materialInputsChanged,
            "unchanged legacy high-tier material costs do not create global frames");
        require(
            NerfDB.entries()
                .stream()
                .filter(value -> value.id.equals("tooltip-042"))
                .findFirst()
                .get().matchStacks.stream()
                    .anyMatch(value -> "Casing_MAX".equals(value.gt)),
            "native Casing_MAX is the UHV identity");

        NerfDB.load(directory, "1.2.0");
        require(
            NerfDB.entries()
                .isEmpty()
                && NerfDB.recipes()
                    .isEmpty(),
            "unknown version has no invented builtin changes");
        NerfDB.load(directory, "");
        require(
            NerfDB.entries()
                .isEmpty()
                && NerfDB.recipes()
                    .isEmpty(),
            "missing ExtraNerf has no builtin changes");

        String custom = "{\"extraNerfVersion\":\"1.1.1\",\"entries\":[{\"id\":\"release-override\",\"summary\":[\"fixture\"]}]}";
        Files.write(override, custom.getBytes(StandardCharsets.UTF_8));
        NerfDB.load(directory, "1.0.1");
        require(
            NerfDB.entries()
                .stream()
                .noneMatch(value -> value.id.equals("release-override")),
            "versioned override is skipped on legacy");
        NerfDB.load(directory, "v1.1.1+build.42");
        require(
            NerfDB.entries()
                .stream()
                .anyMatch(value -> value.id.equals("release-override")),
            "versioned override uses normalized version");
        Files.write(
            override,
            custom.replace("\"extraNerfVersion\":\"1.1.1\",", "")
                .getBytes(StandardCharsets.UTF_8));
        NerfDB.load(directory, "1.2.0");
        require(
            NerfDB.entries()
                .stream()
                .anyMatch(value -> value.id.equals("release-override")),
            "unversioned user data works on unknown version");
        Files.delete(override);
        CertificateStatusChecks.run();
        System.out.println("PASS: " + checks + " version compatibility checks");
    }
}
