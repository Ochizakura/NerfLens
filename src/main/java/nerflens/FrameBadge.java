package nerflens;

import codechicken.lib.gui.GuiDraw;
import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.Badge;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FrameBadge extends Badge {

    public FrameBadge(int color, String... tooltip) {
        super("", tooltip);
        setColor(color);
    }

    @Override
    public void draw(Rectangle4i rect) {
        NEIClientUtils.gl2DRenderContext(() -> {
            int x = rect.x - 1;
            int y = rect.y - 1;
            int w = rect.w + 2;
            int h = rect.h + 2;
            GuiDraw.drawRect(x, y, w, 1, getColor());
            GuiDraw.drawRect(x, y + h - 1, w, 1, getColor());
            GuiDraw.drawRect(x, y + 1, 1, h - 2, getColor());
            GuiDraw.drawRect(x + w - 1, y + 1, 1, h - 2, getColor());
        });
    }
}
