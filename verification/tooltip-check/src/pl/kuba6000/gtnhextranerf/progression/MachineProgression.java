package pl.kuba6000.gtnhextranerf.progression;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
public final class MachineProgression {
    public enum Certificate { STEAM, LV, MV, HV, UV }
    public static Certificate requiredTier(MetaTileEntity value) {
        if (!(value instanceof MTEBasicMachine)) return null;
        if (value instanceof gregtech.common.tileentities.machines.basic.MTEIceCreamMachine && nerflens.NerfDB.profileVersion().equals("1.1.1")) return null;
        MTEBasicMachine machine = (MTEBasicMachine) value;
        if (machine.isSteampowered()) return Certificate.STEAM;
        switch (machine.mTier) {
            case 0:return null;
            case 1:return Certificate.LV;
            case 2:return Certificate.MV;
            case 3:return Certificate.HV;
            default:return Certificate.UV;
        }
    }
}
