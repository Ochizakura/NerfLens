package gregtech.api.enums;
public class Materials {
    private final boolean metal;
    private Materials(boolean metal) { this.metal = metal; }
    public static Materials get(String name) { return new Materials(name.equals("Aluminium") || name.equals("Steel") || name.equals("Iron")); }
    public boolean contains(SubTag tag) { return metal && tag == SubTag.METAL; }
}
