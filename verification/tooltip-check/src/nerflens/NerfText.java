package nerflens;

/** Headless display-language fixture; matching and badge classes come from the built development JAR. */
public final class NerfText {
    public static boolean useChinese = true;
    public static boolean chinese() { return useChinese; }
    public static String language() { return useChinese ? "zh_CN" : "en_US"; }
    public static String tr(String key, Object... args) { return key; }
    public static String pick(String zh, String en) { return useChinese ? zh : en; }
    public static java.util.List<String> lines(java.util.List<String> zh, java.util.List<String> en) {
        return !useChinese && en != null && !en.isEmpty() ? en : zh;
    }
}
