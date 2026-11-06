import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.*;

/** Apply the built production Mixin to the actual installed NEI RecipeWidget bytecode. */
public final class FrameMixinProbe {
    public static void main(String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator))
            urls.add(new File(path).toURI().toURL());
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("TweakClasses", new ArrayList<String>());
        Launch.blackboard.put("Tweaks", new ArrayList<Object>());
        Launch.classLoader = new LaunchClassLoader(urls.toArray(new URL[0]));
        Launch.classLoader.addClassLoaderExclusion("cpw.mods.fml.");
        Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");
        // This probe tests bytecode transformation, without launching Forge coremods a second time.
        Class<?> fmlAgent = Class.forName("org.spongepowered.asm.launch.platform.MixinPlatformAgentFMLLegacy");
        Field loadedCoreMods = fmlAgent.getDeclaredField("loadedCoreMods"); loadedCoreMods.setAccessible(true);
        ((Set<String>) loadedCoreMods.get(null)).add("io.github.legacymoddingmc.unimixins.all.AllCore");
        for (ClassLoader loader : new ClassLoader[]{FrameMixinProbe.class.getClassLoader(), Launch.classLoader}) {
            for (String type : new String[]{"cpw.mods.fml.relauncher.FMLRelaunchLog", "cpw.mods.fml.relauncher.FMLLaunchHandler"}) {
                Class<?> target = Class.forName(type, true, loader);
                for (Field field : target.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType().isEnum()
                        && field.getType().getSimpleName().equals("Side")) {
                        field.setAccessible(true);
                        for (Object side : field.getType().getEnumConstants())
                            if (((Enum<?>) side).name().equals("CLIENT")) field.set(null, side);
                    }
                }
            }
        }
        MixinBootstrap.init();
        Method phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true); phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("searge");
        Mixins.getConfigs().clear();
        Mixins.addConfiguration("mixins.nerflens.frameprobe.json");
        Object transformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (transformer == null) {
            Constructor<?> constructor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();
            constructor.setAccessible(true); transformer = constructor.newInstance();
        }
        Method transform = transformer.getClass().getMethod("transformClassBytes", String.class, String.class, byte[].class);
        transform.setAccessible(true);
        String name = "codechicken.nei.recipe.widget.RecipeWidget";
        byte[] before = Files.readAllBytes(Paths.get(args[0]));
        byte[] after = (byte[]) transform.invoke(transformer, name, name, before);
        Files.write(Paths.get(args[0] + ".transformed"), after);
        ClassNode node = new ClassNode(); new ClassReader(after).accept(node, 0);
        int drawHooks = 0, tooltipHooks = 0, explanations = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (method.name.equals("drawItem") && call.name.contains("nerflens$drawFrame")) drawHooks++;
                if (method.name.equals("handleItemTooltip") && call.name.contains("nerflens$frameTooltip")) tooltipHooks++;
                if (call.owner.equals("nerflens/TooltipHandler") && call.name.equals("appendRecipeChanges")) explanations++;
            }
        }
        if (drawHooks < 1 || tooltipHooks < 1 || explanations < 1) throw new AssertionError("Missing production hooks: draw=" + drawHooks + ", tooltip=" + tooltipHooks);
        System.out.println("PASS: installed NEI RecipeWidget accepts production frame Mixin; draw hooks=" + drawHooks + ", tooltip hooks=" + tooltipHooks + ", recipe explanation calls=" + explanations);
        for (String target : new String[]{"net.minecraft.client.resources.Locale", "net.minecraft.util.StringTranslate"}) {
            String file = target.substring(target.lastIndexOf('.') + 1) + ".class";
            byte[] result = (byte[]) transform.invoke(transformer, target, target, Files.readAllBytes(Paths.get(file)));
            Files.write(Paths.get(file + ".transformed"), result);
            ClassNode translated = new ClassNode(); new ClassReader(result).accept(translated, 0);
            int fallbacks = 0, reloads = 0;
            for (MethodNode method : translated.methods) for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("nerflens/NerfTranslations") && call.name.equals("fallback")) fallbacks++;
                if (call.owner.equals("nerflens/NerfTranslations") && call.name.equals("reloaded")) reloads++;
            }
            if (fallbacks != 1 || (file.equals("Locale.class") && reloads != 1))
                throw new AssertionError("Missing native translation hooks in " + target + ": fallback=" + fallbacks + ", reload=" + reloads);
            System.out.println("PASS: production translation Mixin accepts " + target + "; fallback hooks=" + fallbacks + ", reload hooks=" + reloads);
        }
        ClassNode tooltip = new ClassNode();new ClassReader(Files.readAllBytes(Paths.get("TooltipHandler.class"))).accept(tooltip,0);
        int statusHooks=0;
        for (MethodNode method:tooltip.methods) if (method.name.equals("onItemTooltip"))
            for (AbstractInsnNode instruction=method.instructions.getFirst();instruction!=null;instruction=instruction.getNext())
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if (call.owner.equals("nerflens/TooltipHandler") && call.name.equals("appendCertificateStatus"))statusHooks++;
                }
        if(statusHooks!=1)throw new AssertionError("Certificate status is not connected to item tooltip events");
        ClassNode tracker=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get("CertTracker.class"))).accept(tracker,0);
        Set<String> forbidden=new HashSet<>(Arrays.asList("loadData","setData","markDirty","saveAllData","func_75742_a","func_75745_a","func_76185_a","func_75744_a"));
        for (MethodNode method:tracker.methods)for(AbstractInsnNode instruction=method.instructions.getFirst();instruction!=null;instruction=instruction.getNext())
            if(instruction instanceof MethodInsnNode && forbidden.contains(((MethodInsnNode)instruction).name))throw new AssertionError("Tracker contains a world lifecycle write/load call");
        ClassNode mod=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get("NerfLensMod.class"))).accept(mod,0);
        int registrations=0;
        for(MethodNode method:mod.methods)if(method.name.equals("init"))for(AbstractInsnNode instruction=method.instructions.getFirst();instruction!=null;instruction=instruction.getNext())
            if(instruction instanceof TypeInsnNode && ((TypeInsnNode)instruction).desc.equals("nerflens/CertTracker"))registrations++;
        if(registrations!=1)throw new AssertionError("Missing certificate tracker registration");
        System.out.println("PASS: certificate tooltip event hook=1, tracker registration=1, forbidden world load/write calls=0");
    }
}
