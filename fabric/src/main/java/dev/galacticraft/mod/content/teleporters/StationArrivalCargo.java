package dev.galacticraft.mod.content.teleporters;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.attachments.GCServerPlayer;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.ParachestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** Hands off the saved flight inventory only after the destination accepts its courier. */
public final class StationArrivalCargo {
    private StationArrivalCargo() {}

    public static boolean deliver(ServerLevel level, GCServerPlayer data, BlockPos arrival, String station) {
        if (data.getRocketStacks().stream().allMatch(ItemStack::isEmpty)) return false;
        ParachestEntity chest = new ParachestEntity(GCEntityTypes.PARACHEST, level,
                data.getRocketStacks(), DyeColor.WHITE, data.getFuel());
        // Keep the chest directly above the station's arrival platform, including shared stations.
        chest.setPos(arrival.getX() + 0.5, arrival.getY() + 6.0, arrival.getZ() + 0.5);
        if (!level.addFreshEntity(chest)) {
            Constant.LOGGER.error("Station {} rejected the return parachest; keeping rocket cargo in player data", station);
            return false;
        }
        data.getRocketStacks().clear();
        data.setFuel(0);
        return true;
    }
}
