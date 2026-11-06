package nerflens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/** Selects only verified official data versions. Unknown versions never inherit newer gameplay claims. */
public final class NerfCompatibility {

    private static final Pattern VERSION = Pattern.compile("^[vV]?(\\d+\\.\\d+\\.\\d+)(?:\\+[A-Za-z0-9.-]+)?$");

    public enum Profile {

        LEGACY("1.0.1", "/assets/nerflens/profiles/1.0.1.json"),
        V110("1.1.0", "/assets/nerflens/profiles/1.1.0.json"),
        V111("1.1.1", "/assets/nerflens/nerfs.json"),
        NONE("", "");

        public final String version;
        public final String resource;

        Profile(String version, String resource) {
            this.version = version;
            this.resource = resource;
        }
    }

    private NerfCompatibility() {}

    public static String detectedVersion() {
        ModContainer mod = Loader.instance()
            .getIndexedModList()
            .get("gtnhextranerf");
        return mod == null ? "" : mod.getVersion();
    }

    public static String normalized(String value) {
        if (value == null) return "";
        Matcher matcher = VERSION.matcher(value.trim());
        return matcher.matches() ? matcher.group(1) : "";
    }

    public static Profile profile(String value) {
        String version = normalized(value);
        for (Profile profile : Profile.values())
            if (!profile.version.isEmpty() && profile.version.equals(version)) return profile;
        return Profile.NONE;
    }
}
