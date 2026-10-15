package dev.galacticraft.mod.content.block.entity.machine;

import com.mojang.datafixers.util.Pair;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.*;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.Galacticraft;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.screen.GCMenuTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Modern implementation of the GC4 vent / clear shaft / sulfuric-acid power source. */
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity {
    public static final int MAX_SOURCE_DEPTH = 19;
    public static final MachineStatus NO_HEAT = MachineStatus.create(
            "machine_status.galacticraft.no_geothermal_source", ChatFormatting.RED, MachineStatus.Type.MISSING_RESOURCE);
    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(ItemResourceSlot.builder(TransferType.PROCESSING)
                    .pos(8, 62).capacity(1).filter(ResourceFilters.CAN_INSERT_ENERGY)
                    .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY))),
            MachineEnergyStorage.spec(Galacticraft.CONFIG.machineEnergyStorageSize(), 0,
                    Galacticraft.CONFIG.geothermalGeneratorMaxProductionRate() * 2));

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.GEOTHERMAL_GENERATOR, pos, state, SPEC);
        // Initialize before a level is assigned; live setOption sends neighbor notifications.
        this.getIOConfig().get(dev.galacticraft.machinelib.api.util.BlockFace.BACK).readTag(
                new dev.galacticraft.machinelib.api.machine.configuration.IOFace(
                dev.galacticraft.machinelib.api.transfer.ResourceType.ENERGY,
                dev.galacticraft.machinelib.api.transfer.ResourceFlow.OUTPUT).createTag());
    }

    public static boolean hasHeatSource(ServerLevel level, BlockPos pos) {
        // Never cause chunk generation or create a ticket while inspecting a heat source.
        if (!level.isLoaded(pos.below()) || !level.getBlockState(pos.below()).is(GCBlocks.VAPOR_SPOUT)) return false;
        for (int depth = 2; depth <= MAX_SOURCE_DEPTH; depth++) {
            BlockPos sample = pos.below(depth);
            if (level.isOutsideBuildHeight(sample) || !level.isLoaded(sample)) return false;
            BlockState state = level.getBlockState(sample);
            if (state.getFluidState().is(GCFluids.SULFURIC_ACID)
                    || state.getFluidState().is(GCFluids.FLOWING_SULFURIC_ACID)) return true;
            if (!state.isAir()) return false;
        }
        return false;
    }

    public static long productionAt(long tick, long maximum) {
        if (maximum <= 0) return 0;
        // Keep the GC4 fluctuating output, expressed in this port's native energy units.
        double phase = (Math.sin((tick % 1_000_000L) / 50.0) + 1.0) * 0.5;
        return Math.max(1L, Math.min(maximum, (long) (maximum * (0.15 + 0.85 * phase))));
    }

    @Override
    protected void tickConstant(ServerLevel level, BlockPos pos, BlockState state, ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.drainPowerToSlot(0);
    }

    @Override
    protected MachineStatus tick(ServerLevel level, BlockPos pos, BlockState state, ProfilerFiller profiler) {
        LoadedEnergyOutput.spread(this, level, pos, state);
        // A bounded, same-column inspection prevents stale generation after source removal.
        if (!hasHeatSource(level, pos)) return NO_HEAT;
        long maximum = Galacticraft.CONFIG.geothermalGeneratorMaxProductionRate();
        if (maximum == 0) return MachineStatuses.IDLE;
        if (this.energyStorage().isFull()) return MachineStatuses.CAPACITOR_FULL;
        this.energyStorage().insert(productionAt(level.getGameTime(), maximum));
        return MachineStatuses.ACTIVE;
    }

    @Override
    public MachineMenu<? extends MachineBlockEntity> createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu<>(GCMenuTypes.GEOTHERMAL_GENERATOR, id, player, this);
    }
}
