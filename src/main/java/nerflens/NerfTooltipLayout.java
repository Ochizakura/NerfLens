package nerflens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

/** Wrap the text before adding indentation, avoiding 1.7.10's leading-space line breaks. */
public final class NerfTooltipLayout {

    private NerfTooltipLayout() {}

    public static List<String> wrap(FontRenderer font, String text, String firstPrefix, String continuationPrefix,
        int width) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        int available = Math
            .max(1, width - Math.max(font.getStringWidth(firstPrefix), font.getStringWidth(continuationPrefix)));
        for (String paragraph : text.split("\\n")) {
            String body = paragraph.trim();
            if (body.isEmpty()) continue;
            boolean first = true;
            for (String line : font.listFormattedStringToWidth(body, available)) {
                if (EnumChatFormatting.getTextWithoutFormattingCodes(line)
                    .trim()
                    .isEmpty()) continue;
                out.add((first ? firstPrefix : continuationPrefix) + line);
                first = false;
            }
        }
        return out;
    }
}
