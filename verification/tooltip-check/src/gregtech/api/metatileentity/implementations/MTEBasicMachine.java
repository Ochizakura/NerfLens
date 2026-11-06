package gregtech.api.metatileentity.implementations;
public class MTEBasicMachine extends gregtech.api.metatileentity.MetaTileEntity {
    private final boolean steam;
    public int mTier = 1;
    public MTEBasicMachine(boolean steam) { this.steam = steam; }
    public boolean isSteampowered() { return steam; }
}
