package forestry.apiculture.genetics;

import forestry.api.genetics.IAllele;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** The harness controls the active allele; production delegates the actual dominance rule to Forestry. */
public final class BeeGenome {
    public static IAllele getSpecies(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        NBTTagList chromosomes = stack.getTagCompound().getCompoundTag("Genome").getTagList("Chromosomes", 10);
        if (chromosomes.tagCount() == 0) return null;
        NBTTagCompound chromosome = chromosomes.getCompoundTagAt(0);
        String uid = chromosome.getString(chromosome.getBoolean("SecondaryActive") ? "UID1" : "UID0");
        return uid.isEmpty() ? null : () -> uid;
    }
}
