import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class HeadlessLaunch {
    public static void main(String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator))
            urls.add(new File(path).toURI().toURL());
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("fml.deobfuscatedEnvironment", Boolean.TRUE);
        Launch.classLoader = new LaunchClassLoader(urls.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(Launch.classLoader);
        Class<?> loader = Class.forName("cpw.mods.fml.common.Loader", true, Launch.classLoader);
        loader.getMethod("injectData", Object[].class).invoke(null, new Object[]{new Object[]{
            "7", "10", "99", "99", "1.7.10", "9.05", new File(args[0]), Collections.emptyList()}});
        Class<?> log = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog", true, Launch.classLoader);
        java.lang.reflect.Field side = log.getDeclaredField("side"); side.setAccessible(true);
        for (Object value : Class.forName("cpw.mods.fml.relauncher.Side", true, Launch.classLoader).getEnumConstants())
            if (((Enum<?>) value).name().equals("CLIENT")) side.set(null, value);
        try {
            Class.forName("FrameChecks", true, Launch.classLoader).getMethod("main", String[].class)
                .invoke(null, (Object) args);
        } catch (InvocationTargetException failure) {
            failure.getCause().printStackTrace();
            System.exit(1);
        }
    }
}
