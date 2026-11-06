package nerflens;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Supplies missing Chinese text at lookup time, without inserting entries into native language maps. */
@SideOnly(Side.CLIENT)
public final class NerfTranslations {

    private static final Map<String, Fallback> FALLBACKS = load();
    private static final Pattern NUMERIC_FORMAT = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static volatile State state = new State(
        false,
        Collections.emptyMap(),
        Collections.emptySet(),
        Collections.emptySet());
    private static volatile long generation;

    private static final class Fallback {

        String english;
        String chinese;
    }

    private static final class State {

        final boolean chinese;
        final Map<String, String> english;
        final Set<String> suppliedKeys;
        final Set<String> suppliedNames;

        State(boolean chinese, Map<String, String> english, Set<String> suppliedKeys, Set<String> suppliedNames) {
            this.chinese = chinese;
            this.english = english;
            this.suppliedKeys = suppliedKeys;
            this.suppliedNames = suppliedNames;
        }
    }

    private NerfTranslations() {}

    private static Map<String, Fallback> load() {
        try (InputStreamReader reader = new InputStreamReader(
            NerfTranslations.class.getResourceAsStream("/assets/nerflens/translation_fallbacks_zh_CN.json"),
            "UTF-8")) {
            Map<String, Fallback> values = new Gson()
                .fromJson(reader, new TypeToken<Map<String, Fallback>>() {}.getType());
            return Collections.unmodifiableMap(values);
        } catch (Exception failure) {
            FMLLog.warning("[NerfLens] unavailable translation fallbacks: %s", failure);
            return Collections.emptyMap();
        }
    }

    /** Includes every resource domain, so translations supplied by another mod or TX Loader also take precedence. */
    public static void reloaded(IResourceManager resources, List<String> languages) {
        boolean chinese = !languages.isEmpty() && "zh_cn".equals(
            languages.get(languages.size() - 1)
                .toLowerCase(Locale.ROOT));
        if (!chinese) {
            state = new State(false, Collections.emptyMap(), Collections.emptySet(), Collections.emptySet());
            generation++;
            return;
        }
        Map<String, String> english = new HashMap<>();
        Set<String> suppliedKeys = new HashSet<>();
        Set<String> suppliedNames = new HashSet<>();
        for (String language : languages) for (String domain : resources.getResourceDomains()) {
            try {
                for (IResource resource : resources
                    .getAllResources(new ResourceLocation(domain, "lang/" + language + ".lang"))) {
                    try (InputStream stream = resource.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty() || line.charAt(0) == '#') continue;
                            int separator = line.indexOf('=');
                            if (separator < 0) continue;
                            String key = line.substring(0, separator);
                            String value = NUMERIC_FORMAT.matcher(line.substring(separator + 1))
                                .replaceAll("%$1s");
                            if ("en_us".equals(language.toLowerCase(Locale.ROOT))) {
                                if (FALLBACKS.containsKey(key)) english.put(key, value);
                            } else {
                                suppliedKeys.add(key);
                                if (key.endsWith(".name")) suppliedNames.add(nameKey(value));
                            }
                        }
                    }
                }
            } catch (IOException absent) {
                // Most domains do not contain every selected language.
            }
        }
        state = new State(
            true,
            Collections.unmodifiableMap(english),
            Collections.unmodifiableSet(suppliedKeys),
            Collections.unmodifiableSet(suppliedNames));
        generation++;
        int missing = 0, supplied = 0, unavailable = 0;
        for (Map.Entry<String, Fallback> value : FALLBACKS.entrySet()) {
            if (suppliedKeys.contains(value.getKey())) supplied++;
            else if (NUMERIC_FORMAT.matcher(value.getValue().english)
                .replaceAll("%$1s")
                .equals(english.get(value.getKey()))) missing++;
            else unavailable++;
        }
        FMLLog.info(
            "[NerfLens] Chinese fallbacks: %d available, %d supplied elsewhere, %d English references differ or are absent",
            missing,
            supplied,
            unavailable);
    }

    public static String fallback(String key, String original) {
        State current = state;
        if (!current.chinese || !FALLBACKS.containsKey(key) || current.suppliedKeys.contains(key)) return original;
        Fallback fallback = FALLBACKS.get(key);
        String english = current.english.get(key);
        if (!NUMERIC_FORMAT.matcher(fallback.english)
            .replaceAll("%$1s")
            .equals(english)) return original;
        // A translator can also inject text directly. Any result different from English/the unresolved key wins.
        if (!key.equals(original) && !original.equals(current.english.get(key))) return original;
        return fallback.chinese;
    }

    public static boolean supplied(String key) {
        return state.suppliedKeys.contains(key);
    }

    public static long generation() {
        return generation;
    }

    public static boolean suppliedName(String name) {
        return name != null && state.suppliedNames.contains(nameKey(name));
    }

    private static String nameKey(String name) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(name)
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
