package nerflens;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

/** Verifies ownership, prerequisite handling and existing-record reads without creating game records. */
public final class CertificateStatusChecks {

    private static int checks;

    private CertificateStatusChecks() {}

    private static void require(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static final class GuardStorage extends MapStorage {

        int loads;
        int writes;

        GuardStorage() {
            super(null);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public WorldSavedData loadData(Class type, String name) {
            loads++;
            throw new AssertionError("Display must not load or create world records");
        }

        @Override
        public void setData(String name, WorldSavedData data) {
            writes++;
            super.setData(name, data);
        }
    }

    private static final class FixtureData extends WorldSavedData {

        FixtureData() {
            super("gtnhextranerf_progression");
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {}

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            NBTTagCompound tiers = new NBTTagCompound();
            tiers.setBoolean("steam", true);
            tiers.setBoolean("lv", true);
            tag.setTag("certifiedTiers", tiers);
        }
    }

    public static void run() {
        CertRules.Need lv = CertRules.forCertificate("lv");
        require(lv.required.equals(Arrays.asList("steam", "lv")), "LV includes both native prerequisites");
        require(
            CertRules.forCertificate("steam").required.equals(Collections.singletonList("steam")),
            "Steam only needs its own certificate");
        require(
            CertRules.forCertificate("hv").required.equals(Arrays.asList("steam", "lv", "mv", "hv")),
            "HV uses the complete chain");
        require(CertRules.forCertificate("unknown") == null, "Unknown certificate is not guessed");
        CertTracker.Status unknown = CertTracker.evaluate(lv.required, null);
        require(!unknown.known && unknown.missing.isEmpty(), "Unknown state is not rendered as missing");
        CertTracker.Status locked = CertTracker.evaluate(lv.required, Collections.emptySet());
        require(locked.known && locked.missing.equals(lv.required), "Known empty state is locked");
        CertTracker.Status ready = CertTracker.evaluate(lv.required, new HashSet<>(lv.required));
        require(ready.known && ready.missing.isEmpty(), "Owned chain is unlocked");
        CertTracker.Status revoked = CertTracker.evaluate(lv.required, Collections.singleton("lv"));
        require(
            revoked.missing.equals(Collections.singletonList("steam")),
            "Steam revocation does not claim that the LV bit was lost");
        require(revoked.owned.contains("lv"), "Existing LV ownership is preserved in display state");
        require(CertTracker.readOwned(new NBTTagCompound()) == null, "Absent certificate compound is unknown");
        NBTTagCompound tag = new NBTTagCompound(), tiers = new NBTTagCompound();
        tag.setTag("certifiedTiers", tiers);
        require(
            CertTracker.readOwned(tag)
                .isEmpty(),
            "An existing empty certificate compound is a known state");
        tiers.setBoolean("lv", true);
        tiers.setBoolean("steam", false);
        tiers.setInteger("hv", 1);
        tag.setBoolean("mv", true);
        Set<String> owned = CertTracker.readOwned(tag);
        require(owned.equals(Collections.singleton("lv")), "Only exact byte-boolean ownership keys are read");
        GuardStorage storage = new GuardStorage();
        require(CertTracker.readExisting(storage) == null, "Missing in-memory record is unknown");
        require(storage.loads == 0 && storage.writes == 0, "A display read does not touch the storage lifecycle");
        FixtureData data = new FixtureData();
        storage.setData("gtnhextranerf_progression", data);
        storage.writes = 0;
        require(
            CertTracker.readExisting(storage)
                .equals(new HashSet<>(Arrays.asList("steam", "lv"))),
            "Existing native in-memory record is read");
        require(
            storage.loads == 0 && storage.writes == 0 && !data.isDirty(),
            "Reading ownership does not create, save or dirty a record");
        System.out.println("PASS: " + checks + " certificate state checks");
    }
}
