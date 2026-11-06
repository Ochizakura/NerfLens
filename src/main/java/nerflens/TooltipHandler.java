package nerflens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TooltipHandler {

    public TooltipHandler() {}

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent ev) {
        if (ev.toolTip == null || ev.itemStack == null) return;
        if (NerfDB.entries()
            .isEmpty()
            && NerfDB.recipes()
                .isEmpty())
            return;
        ItemStack st = ev.itemStack;
        if (st.getItem() == null) return;

        String display;
        try {
            display = st.getDisplayName();
        } catch (Throwable t) {
            return; // 有些物品在极端时机会抛异常, 显示类功能绝不引发崩溃
        }

        if (!ev.toolTip.isEmpty() && NerfText.chinese()) {
            String wikiName = NerfWikiNames.display(st);
            if (!display.equals(wikiName)) {
                String title = ev.toolTip.get(0);
                if (title.contains(display)) ev.toolTip.set(0, title.replace(display, wikiName));
            }
        }

        appendChanges(st, ev.toolTip, GuiScreen.isShiftKeyDown());
        CertRules.Need need = CertRules.forStack(st);
        if (need != null)
            appendCertificateStatus(ev.toolTip, need, CertTracker.check(need.required), GuiScreen.isShiftKeyDown());
    }

    /**
     * Only reports state; certificate items show their own bit, while machines show their complete prerequisite chain.
     */
    public static void appendCertificateStatus(List<String> tip, CertRules.Need need, CertTracker.Status status,
        boolean expanded) {
        addSection(tip, NerfText.tr("cert.title"), EnumChatFormatting.AQUA);
        addLabeledLine(tip, NerfText.tr("cert.required") + ": " + CertRules.labels(need.required));
        if (!status.known) {
            addIndented(tip, EnumChatFormatting.GRAY + NerfText.tr("cert.unknown"), "  • ");
        } else if (expanded) {
            for (String code : need.required) addIndented(
                tip,
                (status.owned.contains(code) ? EnumChatFormatting.DARK_GREEN : EnumChatFormatting.RED)
                    + CertRules.shortLabel(code)
                    + ": "
                    + NerfText.tr(status.owned.contains(code) ? "cert.unlocked" : "cert.locked"),
                "  • ");
        } else if (status.missing.isEmpty()) {
            addIndented(tip, EnumChatFormatting.DARK_GREEN + NerfText.tr("cert.ready"), "  • ");
        } else {
            addIndented(
                tip,
                EnumChatFormatting.RED + NerfText.tr("cert.locked") + ": " + CertRules.labels(status.missing),
                "  • ");
        }
    }

    /** Used by normal item events and by renderers which build fluid tooltips without an item event. */
    public static void appendChanges(ItemStack st, List<String> tip, boolean expanded) {
        if (st == null || st.getItem() == null || tip == null) return;
        List<NerfDB.Entry> hits = NerfRecipes.entriesFor(st);
        int initialSize = tip.size();
        boolean hiddenDetails = false;
        boolean hasDescription = false;
        if (hits != null && !hits.isEmpty()) {
            for (NerfDB.Entry en : hits) {
                String heading = NerfText.pick(en.heading, en.headingEn);
                if (heading.isEmpty()) heading = NerfText.tr("changes");
                addSection(
                    tip,
                    "ExtraNerf · " + heading,
                    en.warning ? EnumChatFormatting.RED : EnumChatFormatting.GOLD);
                List<String> details = NerfText.lines(en.lines, en.linesEn);
                List<String> summary = NerfText.lines(en.summary, en.summaryEn);
                hasDescription |= !details.isEmpty() || !summary.isEmpty() || !en.newMat.isEmpty();
                boolean hasSummary = summary != null && !summary.isEmpty();
                List<String> visible = expanded && !details.isEmpty() || !hasSummary ? details : summary;
                int limit = expanded ? visible.size() : Math.min(2, visible.size());
                for (int i = 0; i < limit; i++) addLabeledLine(tip, visible.get(i));
                if (!expanded && (hasSummary && !details.isEmpty() || visible.size() > limit || !en.newMat.isEmpty()))
                    hiddenDetails = true;
                if (expanded && en.newMat != null && !en.newMat.isEmpty()) {
                    tip.add(EnumChatFormatting.GOLD + NerfText.tr("added.materials"));
                    for (String m : NerfText.lines(en.newMat, en.newMatEn)) {
                        addIndented(tip, EnumChatFormatting.GOLD + m, "  + ");
                    }
                }
            }
        }

        // Many affected products have scoped compare records, but no separate hand-written item entry.
        if (!hasDescription) appendRecipeChanges(tip, NerfItems.recipesFor(st), expanded);

        // Removed routes have no live NEI page. Explain them on their product's tooltip.
        for (NerfDB.RecipeEntry recipe : NerfDB.recipes()) {
            if (!recipe.removed || recipe.outputs == null) continue;
            for (NerfDB.StackSpec output : recipe.outputs) {
                if (!NerfRecipes.matchesItem(output, st)) continue;
                if (!expanded && !hits.isEmpty()) {
                    hiddenDetails = true;
                    break;
                }
                addSection(
                    tip,
                    NerfText.tr("removed") + " " + NerfText.pick(recipe.title, recipe.titleEn),
                    EnumChatFormatting.GOLD);
                if (!expanded) {
                    hiddenDetails = true;
                    break;
                }
                addIndented(tip, EnumChatFormatting.GRAY + NerfText.pick(recipe.before, recipe.beforeEn), "  • ");
                if (!recipe.note.isEmpty())
                    addIndented(tip, EnumChatFormatting.DARK_GRAY + NerfText.pick(recipe.note, recipe.noteEn), "  • ");
                break;
            }
        }

        if (expanded && tip.size() > initialSize) tip.add(
            EnumChatFormatting.DARK_GRAY + (NerfDB.profileVersion()
                .isEmpty() ? NerfText.tr("source.user") : NerfText.tr("source", NerfDB.profileVersion())));
        if (hiddenDetails) addMoreHint(tip);
    }

    /** Explain a product's recipe records, or the current recipe's changed input, without broad name matching. */
    public static void appendRecipeChanges(List<String> tip, List<NerfDB.RecipeEntry> recipes, boolean expanded) {
        List<NerfDB.RecipeEntry> live = new ArrayList<>();
        for (NerfDB.RecipeEntry recipe : recipes) {
            if (!recipe.removed && !recipe.noticeOnly) live.add(recipe);
        }
        int limit = expanded ? live.size() : Math.min(2, live.size());
        boolean hiddenDetails = live.size() > limit;
        for (int i = 0; i < limit; i++) {
            NerfDB.RecipeEntry recipe = live.get(i);
            String title = NerfText.pick(recipe.title, recipe.titleEn);
            addSection(tip, "ExtraNerf · " + title, EnumChatFormatting.GOLD);
            if (expanded)
                addLabeledLine(tip, NerfText.tr("before") + ": " + NerfText.pick(recipe.before, recipe.beforeEn));
            String after = NerfText.pick(recipe.after, recipe.afterEn)
                .replace("quantity shown on the item card", "the recipe quantity of");
            addLabeledLine(tip, NerfText.tr("after") + ": " + after);
            String note = NerfText.pick(recipe.note, recipe.noteEn);
            if (expanded && !note.isEmpty()) addIndented(tip, EnumChatFormatting.GRAY + note, "  • ");
            if (!expanded && (!recipe.before.isEmpty() || !note.isEmpty())) hiddenDetails = true;
        }
        if (hiddenDetails) addMoreHint(tip);
    }

    private static void addMoreHint(List<String> tip) {
        String hint = EnumChatFormatting.DARK_GRAY + NerfText.tr("more");
        if (!tip.contains(hint)) tip.add(hint);
    }

    /** GT 风格短行: "标签: 值" 的标签染青色, 普通句子整体灰色。 */
    private static void addLabeledLine(List<String> tip, String line) {
        if (line == null || line.trim()
            .isEmpty()) return;
        int idx = -1;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (c == ':' || c == '：') {
                idx = i;
                break;
            }
        }
        // 仅当冒号前的"标签"足够短时才视为键值对
        if (idx > 0 && idx <= 14) {
            String label = line.substring(0, idx)
                .trim();
            String value = line.substring(idx + 1)
                .trim();
            addIndented(
                tip,
                EnumChatFormatting.AQUA + label + (NerfText.chinese() ? "：" : ": ") + EnumChatFormatting.GRAY + value,
                "  • ");
        } else {
            addIndented(tip, EnumChatFormatting.GRAY + line.trim(), "  • ");
        }
    }

    private static void addWrapped(List<String> tip, String line) {
        tip.addAll(
            NerfTooltipLayout.wrap(net.minecraft.client.Minecraft.getMinecraft().fontRenderer, line, "", "", 260));
    }

    private static void addIndented(List<String> tip, String line, String prefix) {
        tip.addAll(
            NerfTooltipLayout
                .wrap(net.minecraft.client.Minecraft.getMinecraft().fontRenderer, line, prefix, "     ", 260));
    }

    private static void addSection(List<String> tip, String title, EnumChatFormatting color) {
        if (!tip.isEmpty() && !EnumChatFormatting.getTextWithoutFormattingCodes(tip.get(tip.size() - 1))
            .trim()
            .isEmpty()) tip.add("");
        addWrapped(tip, color.toString() + EnumChatFormatting.BOLD + "◆ " + title);
    }
}
