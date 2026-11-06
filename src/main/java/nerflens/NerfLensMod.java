package nerflens;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * NerfLens — GTNH 2.9.0 + ExtraNerf 客户端显示辅助。
 * 只读展示: 不注册任何物品/方块, 不修改任何配方或机器行为。
 * 功能:
 * 1) 机器/物品 tooltip 标注 ExtraNerf 改动(含"相比原版新增材料"标记)
 * 2) NEI 中同样生效(tooltip 事件在 NEI 页面同样触发)
 */
@Mod(
    modid = "nerflens",
    name = "NerfLens",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:gtnhextranerf",
    acceptableRemoteVersions = "*")
public class NerfLensMod {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) NerfDB.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            TooltipHandler th = new TooltipHandler();
            MinecraftForge.EVENT_BUS.register(th);
            FMLCommonHandler.instance()
                .bus()
                .register(new CertTracker());
        }
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            NerfRecipes.clearCache();
            NerfItems.initialize();
            NerfChromaticTooltips.register();
        }
    }

}
