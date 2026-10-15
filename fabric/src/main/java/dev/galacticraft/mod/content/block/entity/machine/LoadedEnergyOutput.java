package dev.galacticraft.mod.content.block.entity.machine;

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.machinelib.impl.platform.MachineLibPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Uses native transfers while never querying a capability in an unloaded neighbor. */
public final class LoadedEnergyOutput {
    private LoadedEnergyOutput() {}

    public static void spread(MachineBlockEntity machine, ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (Direction direction : Direction.values()) {
            var face = machine.getIOConfig().get(BlockFace.from(facing, direction));
            if (face.getType().willAcceptResource(ResourceType.ENERGY)
                    && face.getFlow().canFlowIn(ResourceFlow.OUTPUT)
                    && level.isLoaded(pos.relative(direction))) {
                MachineLibPlatform.spreadEnergy(level, pos, direction, machine.energyStorage());
            }
        }
    }
}
