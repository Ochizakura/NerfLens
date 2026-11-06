package nerflens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.Widget;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.widget.RecipeWidget;
import codechicken.nei.scroll.ScrollContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NerfCompareOverlay {

    public static final int HEIGHT = 62;
    private static final Map<ScrollContainer, State> STATES = new WeakHashMap<>();

    private static final class State {

        RecipeWidget widget;
        NerfDB.RecipeEntry recipe;
        String language;
        List<NerfVisuals.Card> cards;
        int page;
    }

    private NerfCompareOverlay() {}

    public static void reserve(GuiRecipe<?> gui, ScrollContainer container) {
        if (NerfRecipes.supports(gui.getHandler()) && container.h > HEIGHT + 36) container.h -= HEIGHT;
    }

    private static State selected(ScrollContainer container, int mouseX, int mouseY) {
        State state = STATES.computeIfAbsent(container, c -> new State());
        Widget hovered = container.getWidgetUnderMouse(mouseX, mouseY);
        RecipeWidget widget = hovered instanceof RecipeWidget ? (RecipeWidget) hovered : state.widget;
        if (widget != null && (!container.getWidgets()
            .contains(widget) || widget.y + widget.h <= container.y || widget.y >= container.y + container.h))
            widget = null;
        if (widget == null) for (Widget candidate : container.getWidgets()) {
            if (!(candidate instanceof RecipeWidget) || candidate.y + candidate.h <= container.y
                || candidate.y >= container.y + container.h) continue;
            RecipeHandlerRef ref = ((RecipeWidget) candidate).getRecipeHandlerRef();
            if (!NerfRecipes.match(ref.handler, ref.recipeIndex)
                .isEmpty()) {
                widget = (RecipeWidget) candidate;
                break;
            }
        }
        if (widget == null) return null;
        RecipeHandlerRef ref = widget.getRecipeHandlerRef();
        List<NerfDB.RecipeEntry> matches = NerfRecipes.match(ref.handler, ref.recipeIndex);
        if (matches.isEmpty()) {
            state.widget = null;
            state.recipe = null;
            return null;
        }
        NerfDB.RecipeEntry recipe = matches.get(0);
        if (state.widget != widget || state.recipe != recipe
            || !NerfText.language()
                .equals(state.language)) {
            state.widget = widget;
            state.recipe = recipe;
            state.language = NerfText.language();
            state.page = 0;
            state.cards = NerfVisuals.cards(recipe, ref.handler, ref.recipeIndex);
        }
        return state;
    }

    private static int perPage(int width) {
        return Math.max(1, Math.min(3, (width - 16) / 52));
    }

    private static int pages(State state, int width) {
        return Math.max(1, (state.cards.size() + perPage(width) - 1) / perPage(width));
    }

    private static void clampPage(State state, int width) {
        state.page = Math.max(0, Math.min(state.page, pages(state, width) - 1));
    }

    private static boolean active(GuiRecipe<?> gui, ScrollContainer container, int top, int height) {
        return NerfRecipes.supports(gui.getHandler()) && container.y + container.h <= top + height - HEIGHT;
    }

    public static void draw(GuiRecipe<?> gui, ScrollContainer container, int left, int top, int width, int height,
        int mouseX, int mouseY) {
        if (!active(gui, container, top, height)) return;
        State state = selected(container, mouseX, mouseY);
        if (state == null) return;
        clampPage(state, width);
        int y = height - HEIGHT;
        NEIClientUtils.gl2DRenderContext(() -> {
            GuiDraw.drawRect(4, y, width - 8, HEIGHT - 3, 0xF0202226);
            GuiDraw.drawRect(4, y, width - 8, 1, 0xFFFFC44D);
            text(NerfText.tr("compare"), 9, y + 4, width - 70, 0.8F, 0xFFD074);
            int pageCount = pages(state, width);
            if (pageCount > 1) {
                text("<", width - 53, y + 4, 8, 0.8F, 0xFFE390);
                text((state.page + 1) + "/" + pageCount, width - 44, y + 4, 23, 0.65F, 0xBDBDBD);
                text(">", width - 24, y + 4, 8, 0.8F, 0xFFE390);
            }
            text("?", width - 13, y + 4, 8, 0.8F, 0xFFD074);
            int count = perPage(width), start = state.page * count;
            int shown = Math.min(count, state.cards.size() - start);
            int cardWidth = Math.min(76, (width - 16 - (shown - 1) * 3) / shown);
            for (int i = 0; i < shown; i++)
                card(state.cards.get(start + i), 8 + i * (cardWidth + 3), y + 16, cardWidth);
        });
    }

    private static void card(NerfVisuals.Card card, int x, int y, int width) {
        GuiDraw.drawRect(x, y, width, 41, 0xFF31343A);
        text(card.label, x + 3, y + 2, width - 6, 0.65F, 0xE2E2E2);
        int oldX = x + 3, newX = x + width - 19;
        icon(card.previous, oldX, y + 11, card.kind, zero(card.before));
        icon(card.current, newX, y + 11, card.kind, zero(card.after));
        int arrowX = x + width / 2 - 3;
        GuiDraw.drawRect(arrowX, y + 18, 6, 1, 0xFFFFC44D);
        GuiDraw.drawRect(arrowX + 4, y + 16, 1, 5, 0xFFFFC44D);
        text(NerfVisuals.compact(card.before), oldX, y + 30, 23, 0.65F, 0xB5B8BD);
        text(NerfVisuals.compact(card.after), newX - 2, y + 30, 22, 0.65F, 0xFFE390);
    }

    private static void icon(ItemStack stack, int x, int y, String kind, boolean empty) {
        if (stack != null && !empty) {
            GuiContainerManager.drawItem(x, y, stack, false, "");
            return;
        }
        int color = 0xFFB4BDC8;
        if (empty) {
            GuiDraw.drawRect(x + 4, y + 7, 8, 2, color);
            return;
        }
        if ("time".equals(kind)) {
            GuiDraw.drawRect(x + 2, y + 2, 12, 12, color);
            GuiDraw.drawRect(x + 3, y + 3, 10, 10, 0xFF31343A);
            GuiDraw.drawRect(x + 7, y + 4, 1, 5, color);
            GuiDraw.drawRect(x + 7, y + 8, 4, 1, color);
        } else if ("energy".equals(kind) || "heat".equals(kind)) {
            GuiDraw.drawRect(x + 7, y + 1, 5, 5, 0xFFFFC44D);
            GuiDraw.drawRect(x + 4, y + 6, 7, 3, 0xFFFFC44D);
            GuiDraw.drawRect(x + 4, y + 9, 4, 5, 0xFFFFC44D);
        } else text("chance".equals(kind) ? "%" : "item".equals(kind) ? "?" : "#", x + 3, y + 3, 13, 1.0F, 0xB4BDC8);
    }

    private static void text(String value, int x, int y, int width, float scale, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int available = (int) (width / scale);
        if (font.getStringWidth(value) > available)
            value = font.trimStringToWidth(value, Math.max(0, available - font.getStringWidth("…"))) + "…";
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(scale, scale, 1);
            font.drawString(value, 0, 0, color);
        } finally {
            GL11.glPopMatrix();
        }
    }

    public static boolean click(GuiRecipe<?> gui, ScrollContainer container, int left, int top, int width, int height,
        int mouseX, int mouseY, int button) {
        if (button != 0 || !active(gui, container, top, height)
            || mouseY < top + height - HEIGHT
            || mouseY >= top + height - HEIGHT + 14) return false;
        State state = selected(container, mouseX, mouseY);
        if (state == null || pages(state, width) <= 1) return false;
        clampPage(state, width);
        int x = mouseX - left,
            direction = x >= width - 55 && x < width - 45 ? -1 : x >= width - 27 && x < width - 16 ? 1 : 0;
        if (direction == 0) return false;
        state.page = (state.page + direction + pages(state, width)) % pages(state, width);
        return true;
    }

    public static List<String> tooltip(GuiRecipe<?> gui, ScrollContainer container, int left, int top, int width,
        int height, int mouseX, int mouseY, List<String> original) {
        if (!active(gui, container, top, height) || mouseX < left + 4
            || mouseX >= left + width - 4
            || mouseY < top + height - HEIGHT
            || mouseY >= top + height - 3) return original;
        State state = selected(container, mouseX, mouseY);
        if (state == null) return original;
        clampPage(state, width);
        List<String> out = original == null ? new ArrayList<>() : new ArrayList<>(original);
        int x = mouseX - left, y = mouseY - top - height + HEIGHT;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        if (x >= width - 16 && y < 14) {
            NerfDB.RecipeEntry recipe = state.recipe;
            out.add(EnumChatFormatting.GOLD + "ExtraNerf: " + NerfText.tr("compare"));
            out.add(EnumChatFormatting.DARK_GRAY + NerfText.tr("compare.legend"));
            for (NerfVisuals.Card card : state.cards) {
                String name = card.current == null ? card.label : NerfWikiNames.display(card.current);
                String line;
                if (differentItems(card)) {
                    line = NerfWikiNames.display(card.previous) + " " + card.before + " → " + name + " " + card.after;
                } else line = name + ": " + card.before + " → " + card.after;
                out.addAll(font.listFormattedStringToWidth(EnumChatFormatting.GRAY + line, 260));
            }
            String note = NerfText.pick(recipe.note, recipe.noteEn);
            if (!note.isEmpty()) out.addAll(font.listFormattedStringToWidth(EnumChatFormatting.AQUA + note, 260));
            return out;
        }
        int count = perPage(width), start = state.page * count, shown = Math.min(count, state.cards.size() - start);
        int cardWidth = Math.min(76, (width - 16 - (shown - 1) * 3) / shown);
        for (int i = 0; i < shown; i++) {
            int cardX = 8 + i * (cardWidth + 3);
            if (x < cardX || x >= cardX + cardWidth || y < 16 || y >= 57) continue;
            NerfVisuals.Card card = state.cards.get(start + i);
            out.add(EnumChatFormatting.GOLD + card.label);
            if (differentItems(card)) out.addAll(
                font.listFormattedStringToWidth(
                    NerfWikiNames.display(card.previous) + " → " + NerfWikiNames.display(card.current),
                    260));
            else if (card.current != null)
                out.addAll(font.listFormattedStringToWidth(NerfWikiNames.display(card.current), 260));
            out.add(EnumChatFormatting.GRAY + NerfText.tr("before") + ": " + card.before);
            out.add(EnumChatFormatting.YELLOW + NerfText.tr("after") + ": " + card.after);
            if (!card.note.isEmpty()) out.addAll(font.listFormattedStringToWidth(card.note, 260));
            break;
        }
        return out;
    }

    private static boolean differentItems(NerfVisuals.Card card) {
        return card.previous != null && card.current != null
            && (card.previous.getItem() != card.current.getItem()
                || card.previous.getItemDamage() != card.current.getItemDamage());
    }

    private static boolean zero(String value) {
        return "0".equals(value.replace(" mB", ""));
    }
}
