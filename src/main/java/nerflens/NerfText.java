package nerflens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

/** Presentation language follows the current game setting, including resource reloads. */
public final class NerfText {

    private NerfText() {}

    public static String language() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc == null || mc.gameSettings == null ? "en_US" : mc.gameSettings.language;
    }

    public static boolean chinese() {
        return language().toLowerCase(Locale.ROOT)
            .startsWith("zh");
    }

    public static String tr(String key, Object... args) {
        return I18n.format("nerflens." + key, args);
    }

    public static String pick(String zh, String en) {
        if (!chinese() && en != null && !en.isEmpty()) return en;
        return zh == null ? "" : NerfWikiNames.text(zh);
    }

    public static List<String> lines(List<String> zh, List<String> en) {
        if (!chinese() && en != null && !en.isEmpty()) return en;
        if (zh == null || !chinese()) return zh;
        List<String> localized = new ArrayList<>(zh.size());
        for (String line : zh) localized.add(NerfWikiNames.text(line));
        return localized;
    }
}
