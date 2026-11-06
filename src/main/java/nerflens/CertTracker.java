package nerflens;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Publishes read-only integrated-server snapshots; missing or remote state is never reported as locked. */
@SideOnly(Side.CLIENT)
public final class CertTracker {

    private static final String DATA_NAME = "gtnhextranerf_progression";
    private static final long INTERVAL = 500_000_000L;
    private static final Field LOADED_DATA = loadedDataField();
    private static volatile Snapshot snapshot = new Snapshot(null, null, null);
    private volatile World world;
    private volatile IntegratedServer server;
    private volatile long lastCheck;

    private static final class Snapshot {

        final World world;
        final IntegratedServer server;
        final Set<String> owned;

        Snapshot(World world, IntegratedServer server, Set<String> owned) {
            this.world = world;
            this.server = server;
            this.owned = owned;
        }
    }

    public static final class Status {

        public final boolean known;
        public final List<String> missing;
        public final Set<String> owned;

        private Status(Set<String> owned, List<String> missing) {
            known = owned != null;
            this.owned = owned == null ? Collections.emptySet() : owned;
            this.missing = Collections.unmodifiableList(missing);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer active = mc.getIntegratedServer();
        if (world != mc.theWorld || server != active) {
            world = mc.theWorld;
            server = active;
            snapshot = new Snapshot(null, null, null);
            lastCheck = 0;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        World clientWorld = world;
        IntegratedServer active = server;
        if (clientWorld == null || active == null) return;
        long now = System.nanoTime();
        if (lastCheck != 0 && now - lastCheck < INTERVAL) return;
        lastCheck = now;
        Set<String> owned = null;
        try {
            WorldServer overworld = active.worldServerForDimension(0);
            if (overworld != null) owned = readExisting(overworld.mapStorage);
        } catch (RuntimeException | LinkageError unreadable) {
            // Unknown snapshots keep their unknown state.
        }
        if (world == clientWorld && server == active) snapshot = new Snapshot(clientWorld, active, owned);
    }

    private static Field loadedDataField() {
        for (String name : new String[] { "loadedDataMap", "field_75749_b" }) {
            try {
                Field field = MapStorage.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException unavailable) {}
        }
        return null;
    }

    static Set<String> readExisting(MapStorage storage) {
        if (storage == null || LOADED_DATA == null) return null;
        try {
            Object data = ((Map<?, ?>) LOADED_DATA.get(storage)).get(DATA_NAME);
            if (!(data instanceof WorldSavedData)) return null;
            NBTTagCompound tag = new NBTTagCompound();
            ((WorldSavedData) data).writeToNBT(tag);
            return readOwned(tag);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    static Set<String> readOwned(NBTTagCompound tag) {
        if (!tag.hasKey("certifiedTiers", 10)) return null;
        NBTTagCompound values = tag.getCompoundTag("certifiedTiers");
        Set<String> result = new HashSet<>();
        for (String code : CertRules.CODES) if (values.hasKey(code, 1) && values.getBoolean(code)) result.add(code);
        return Collections.unmodifiableSet(result);
    }

    static Status evaluate(List<String> required, Set<String> owned) {
        List<String> missing = new ArrayList<>();
        if (owned != null) for (String code : required) if (!owned.contains(code)) missing.add(code);
        return new Status(owned, missing);
    }

    public static Status check(List<String> required) {
        Minecraft mc = Minecraft.getMinecraft();
        Snapshot current = snapshot;
        Set<String> owned = current.world == mc.theWorld && current.server == mc.getIntegratedServer() ? current.owned
            : null;
        return evaluate(required, owned);
    }
}
