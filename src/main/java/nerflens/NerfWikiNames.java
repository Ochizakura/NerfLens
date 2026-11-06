package nerflens;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** Name fallbacks used only when the selected language has no existing item-name translation. */
public final class NerfWikiNames {

    private static final Map<String, String> NAMES = load();
    private static final Pattern PHRASES = phrases();

    private NerfWikiNames() {}

    private static Map<String, String> load() {
        try (InputStreamReader reader = new InputStreamReader(
            NerfWikiNames.class.getResourceAsStream("/assets/nerflens/wiki_names.json"),
            "UTF-8")) {
            Map<String, String> values = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, String> value : values.entrySet()) out.put(
                value.getKey()
                    .toLowerCase(Locale.ROOT),
                value.getValue());
            return Collections.unmodifiableMap(out);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    public static String display(ItemStack stack) {
        String original = stack.getDisplayName();
        if (stack.hasDisplayName() || NerfTranslations.supplied(stack.getUnlocalizedName() + ".name")) return original;
        return display(original);
    }

    public static String display(String original) {
        if (!NerfText.chinese() || original == null
            || containsChinese(original)
            || NerfTranslations.suppliedName(original)) return original;
        String key = EnumChatFormatting.getTextWithoutFormattingCodes(original)
            .trim()
            .toLowerCase(Locale.ROOT);
        return NAMES.getOrDefault(key, original);
    }

    private static Pattern phrases() {
        Map<String, String> phrases = new LinkedHashMap<>();
        for (Map.Entry<String, String> name : NAMES.entrySet())
            if (!containsChinese(name.getKey())) phrases.put(name.getKey(), name.getValue());
        List<String> keys = new ArrayList<>(phrases.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        StringBuilder pattern = new StringBuilder();
        for (String key : keys) {
            if (key.isEmpty()) continue;
            if (pattern.length() > 0) pattern.append('|');
            if (asciiWord(key.charAt(0))) pattern.append("(?<![A-Za-z0-9_])");
            pattern.append(Pattern.quote(key));
            if (asciiWord(key.charAt(key.length() - 1))) pattern.append("(?![A-Za-z0-9_])");
        }
        return Pattern.compile(pattern.length() == 0 ? "(?!)" : pattern.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static boolean asciiWord(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character >= '\u3400' && character <= '\u9FFF') return true;
        }
        return false;
    }

    /** Normalize presentation text, including old names in user display overrides. */
    public static String text(String value) {
        return text(value, NerfText.chinese());
    }

    public static String text(String value, boolean chinese) {
        if (!chinese || value == null) return value;
        Matcher matcher = PHRASES.matcher(value);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String original = matcher.group();
            String replacement = NerfTranslations.suppliedName(original) ? original
                : NAMES.getOrDefault(original.toLowerCase(Locale.ROOT), original);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
