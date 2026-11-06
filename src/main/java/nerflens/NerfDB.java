package nerflens;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;

import cpw.mods.fml.common.FMLLog;

/** Display data only. User records replace builtin records with the same ID (or legacy title). */
public final class NerfDB {

    public static final class Entry {

        public String id = "";
        public String kind = "item";
        public String title = "";
        public String heading = "";
        public String headingEn = "";
        public boolean warning;
        public boolean materialInputsChanged;
        public List<String> matchUnloc = Collections.emptyList();
        public List<String> requireUnloc = Collections.emptyList();
        public List<String> matchName = Collections.emptyList();
        public List<StackSpec> matchStacks = Collections.emptyList();
        public List<String> lines = Collections.emptyList();
        public List<String> summary = Collections.emptyList();
        public List<String> summaryEn = Collections.emptyList();
        public List<String> newMat = Collections.emptyList();
        public List<String> linesEn = Collections.emptyList();
        public List<String> newMatEn = Collections.emptyList();
    }

    /** Item identity or fluid identity; n/mB constrain summed recipe quantities when supplied. */
    public static final class StackSpec {

        public String od;
        public String odPrefix;
        public String odSuffix;
        public String item;
        public Integer meta;
        public String mod;
        public String gt;
        public Integer gtCableMinTier;
        public String gtMachineClass;
        public Boolean steamPowered;
        public List<String> excludeMachineClasses;
        public Map<String, String> nbtStrings;
        public String nh;
        public String ic2;
        public String material;
        public Integer bwMeta;
        public String prefix = "dust";
        public String unloc;
        public String dn;
        public String dnzh;
        public List<StackSpec> any;
        public String f;
        public List<String> fany;
        public String fluidGt;
        public String phase = "fluid";
        public Integer mB;
        public Integer n;
        public String circuit;
    }

    public static final class RecipeEntry {

        public String id = "";
        public String source = "";
        public String title = "";
        public List<String> maps = Collections.emptyList();
        public List<StackSpec> inputs = Collections.emptyList();
        public List<StackSpec> outputs = Collections.emptyList();
        public List<StackSpec> changedInputs = Collections.emptyList();
        public List<StackSpec> absentInputs = Collections.emptyList();
        public boolean allInputsChanged;
        public boolean removed;
        public boolean noticeOnly;
        public Boolean materialInputsChanged;
        public String rule = "";
        public String before = "";
        public String after = "";
        public String note = "";
        public String titleEn = "";
        public String beforeEn = "";
        public String afterEn = "";
        public String noteEn = "";
        public List<VisualChange> visualChanges = Collections.emptyList();
    }

    /** Compact visual deltas. Empty after values use the actual recipe quantity. */
    public static final class VisualChange {

        public String label = "投入";
        public String labelEn = "Input";
        public String kind = "item";
        public StackSpec stack;
        public StackSpec oldStack;
        public String oldPrefix = "";
        public String oldSuffix = "";
        public String before = "";
        public String after = "";
        public String beforeEn = "";
        public String afterEn = "";
        public String note = "";
        public boolean output;
    }

    public static final class Root {

        public int schemaVersion = 3;
        public String extraNerfVersion = "";
        public List<Entry> entries = Collections.emptyList();
        public List<RecipeEntry> recipes = Collections.emptyList();
    }

    private static volatile List<Entry> ENTRIES = Collections.emptyList();
    private static volatile List<RecipeEntry> RECIPES = Collections.emptyList();
    private static volatile String PROFILE_VERSION = "";
    private static volatile String DETECTED_VERSION = "";

    private NerfDB() {}

    public static void load(File configDir) {
        load(configDir, NerfCompatibility.detectedVersion());
    }

    /** Explicit version entry also permits reproducible checks without inventing an installed mod container. */
    public static void load(File configDir, String extraNerfVersion) {
        NerfCompatibility.Profile profile = NerfCompatibility.profile(extraNerfVersion);
        Map<String, Entry> entries = new LinkedHashMap<>();
        Map<String, RecipeEntry> recipes = new LinkedHashMap<>();
        if (profile != NerfCompatibility.Profile.NONE) {
            try {
                Root builtin = read(NerfDB.class.getResourceAsStream(profile.resource));
                if (builtin.schemaVersion != 3 || !profile.version.equals(builtin.extraNerfVersion))
                    throw new IllegalArgumentException("display profile version mismatch");
                merge(builtin, entries, recipes);
            } catch (Exception failure) {
                FMLLog.warning("[NerfLens] failed to load %s profile: %s", profile.version, failure);
            }
        }
        File user = new File(configDir, "nerflens/user_nerfs.json");
        if (user.isFile()) {
            try {
                Root custom = read(new FileInputStream(user));
                String requestedVersion = text(custom.extraNerfVersion);
                if (requestedVersion.isEmpty() || requestedVersion.equals(text(extraNerfVersion))
                    || !NerfCompatibility.normalized(requestedVersion)
                        .isEmpty() && NerfCompatibility.normalized(requestedVersion)
                            .equals(NerfCompatibility.normalized(extraNerfVersion)))
                    merge(custom, entries, recipes);
                else FMLLog.info(
                    "[NerfLens] user display data targets %s; active ExtraNerf is %s",
                    requestedVersion,
                    extraNerfVersion);
            } catch (Exception failure) {
                FMLLog.warning("[NerfLens] failed to load user_nerfs.json: %s", failure);
            }
        }
        ENTRIES = Collections.unmodifiableList(new ArrayList<>(entries.values()));
        RECIPES = Collections.unmodifiableList(new ArrayList<>(recipes.values()));
        PROFILE_VERSION = profile.version;
        DETECTED_VERSION = text(extraNerfVersion);
        int live = 0, removed = 0, notes = 0, legacy = 0;
        for (RecipeEntry recipe : RECIPES) {
            if (recipe.removed) removed++;
            else if (recipe.noticeOnly) notes++;
            else if (!recipe.maps.isEmpty() && !recipe.outputs.isEmpty()) live++;
            else legacy++;
        }
        FMLLog.info("[NerfLens] loaded %d nerf entries, %d compare recipes", ENTRIES.size(), RECIPES.size());
        FMLLog.info(
            "[NerfLens] ExtraNerf=%s; data profile=%s",
            DETECTED_VERSION.isEmpty() ? "absent" : DETECTED_VERSION,
            PROFILE_VERSION.isEmpty() ? "user data only" : PROFILE_VERSION);
        FMLLog.info("[NerfLens] recipe scopes: %d live, %d removed, %d notice-only", live, removed, notes);
        if (legacy > 0)
            FMLLog.warning("[NerfLens] %d legacy compare records have no recipe scope; migrate to schema v3", legacy);
    }

    private static Root read(InputStream stream) throws Exception {
        if (stream == null) throw new IllegalArgumentException("missing database resource");
        try (Reader reader = new InputStreamReader(stream, "UTF-8")) {
            return new Gson().fromJson(reader, Root.class);
        }
    }

    private static void merge(Root root, Map<String, Entry> entries, Map<String, RecipeEntry> recipes) {
        if (root == null) return;
        if (root.entries != null) for (Entry entry : root.entries) {
            if (entry == null) continue;
            entry.id = text(entry.id);
            entry.title = text(entry.title);
            entry.heading = text(entry.heading);
            entry.headingEn = text(entry.headingEn);
            entry.matchUnloc = list(entry.matchUnloc);
            entry.requireUnloc = list(entry.requireUnloc);
            entry.matchName = list(entry.matchName);
            entry.matchStacks = list(entry.matchStacks);
            entry.lines = list(entry.lines);
            entry.summary = list(entry.summary);
            entry.summaryEn = list(entry.summaryEn);
            entry.newMat = list(entry.newMat);
            entry.linesEn = list(entry.linesEn);
            entry.newMatEn = list(entry.newMatEn);
            String replacementKey = key(entry.id, entry.title, entries.size());
            if (entry.id.isEmpty() && !entry.title.isEmpty()) {
                for (Map.Entry<String, Entry> existing : entries.entrySet()) {
                    if (entry.title.equals(existing.getValue().title)) {
                        replacementKey = existing.getKey();
                        break;
                    }
                }
            }
            entries.put(replacementKey, entry);
        }
        if (root.recipes != null) for (RecipeEntry recipe : root.recipes) {
            if (recipe == null) continue;
            recipe.id = text(recipe.id);
            recipe.title = text(recipe.title);
            recipe.before = text(recipe.before);
            recipe.after = text(recipe.after);
            recipe.note = text(recipe.note);
            recipe.titleEn = text(recipe.titleEn);
            recipe.beforeEn = text(recipe.beforeEn);
            recipe.afterEn = text(recipe.afterEn);
            recipe.noteEn = text(recipe.noteEn);
            recipe.maps = list(recipe.maps);
            recipe.inputs = list(recipe.inputs);
            recipe.outputs = list(recipe.outputs);
            recipe.changedInputs = list(recipe.changedInputs);
            recipe.absentInputs = list(recipe.absentInputs);
            recipe.visualChanges = list(recipe.visualChanges);
            for (VisualChange change : recipe.visualChanges) {
                if (change == null) continue;
                change.label = text(change.label);
                change.labelEn = text(change.labelEn);
                change.kind = text(change.kind);
                change.before = text(change.before);
                change.after = text(change.after);
                change.beforeEn = text(change.beforeEn);
                change.afterEn = text(change.afterEn);
                change.oldPrefix = text(change.oldPrefix);
                change.oldSuffix = text(change.oldSuffix);
            }
            String replacementKey = key(recipe.id, recipe.title, recipes.size());
            if (recipe.id.isEmpty() && !recipe.title.isEmpty()) {
                for (Map.Entry<String, RecipeEntry> existing : recipes.entrySet()) {
                    if (recipe.title.equals(existing.getValue().title)) {
                        replacementKey = existing.getKey();
                        break;
                    }
                }
            }
            recipes.put(replacementKey, recipe);
        }
    }

    private static String key(String id, String title, int index) {
        return !id.isEmpty() ? "id:" + id : !title.isEmpty() ? "title:" + title : "anonymous:" + index;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> list(List<T> value) {
        return value == null ? Collections.emptyList() : value;
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<RecipeEntry> recipes() {
        return RECIPES;
    }

    public static String profileVersion() {
        return PROFILE_VERSION;
    }

    public static String detectedVersion() {
        return DETECTED_VERSION;
    }

    public static List<Entry> match(String unlocalizedName, String displayName) {
        String unloc = text(unlocalizedName).toLowerCase(Locale.ROOT);
        String display = text(displayName).toLowerCase(Locale.ROOT);
        List<Entry> hits = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            boolean allowed = true;
            for (String token : entry.requireUnloc) {
                if (token != null && !unloc.contains(token.toLowerCase(Locale.ROOT))) {
                    allowed = false;
                    break;
                }
            }
            if (!allowed) continue;
            boolean matched = false;
            for (String token : entry.matchUnloc) {
                if (token != null && !token.isEmpty() && unloc.contains(token.toLowerCase(Locale.ROOT))) {
                    matched = true;
                    break;
                }
            }
            if (!matched) for (String token : entry.matchName) {
                if (token == null || token.isEmpty()) continue;
                String query = token.toLowerCase(Locale.ROOT);
                if (query.startsWith("=") ? display.equals(query.substring(1)) : display.contains(query)) {
                    matched = true;
                    break;
                }
            }
            if (matched) hits.add(entry);
        }
        return hits;
    }
}
