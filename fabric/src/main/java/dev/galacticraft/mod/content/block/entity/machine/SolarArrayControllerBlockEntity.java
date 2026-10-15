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
import dev.galacticraft.mod.screen.GCMenuTypes;
import dev.galacticraft.mod.world.dimension.duststorm.MarsDustStormManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

/** Connected, same-height solar cells feed one deterministically selected controller. */
public class SolarArrayControllerBlockEntity extends MachineBlockEntity {
    public static final int MAX_NETWORK_BLOCKS = 128;
    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(ItemResourceSlot.builder(TransferType.PROCESSING)
                    .pos(8, 62).capacity(1).filter(ResourceFilters.CAN_INSERT_ENERGY)
                    .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY))),
            MachineEnergyStorage.spec(Galacticraft.CONFIG.machineEnergyStorageSize(), 0,
                    Galacticraft.CONFIG.solarArrayMaxProductionRate()));
    private int illuminatedPanels;
    private long scannedAt = Long.MIN_VALUE;

    public SolarArrayControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.SOLAR_ARRAY_CONTROLLER, pos, state, SPEC);
        this.getIOConfig().get(dev.galacticraft.machinelib.api.util.BlockFace.BACK).readTag(
                new dev.galacticraft.machinelib.api.machine.configuration.IOFace(
                dev.galacticraft.machinelib.api.transfer.ResourceType.ENERGY,
                dev.galacticraft.machinelib.api.transfer.ResourceFlow.OUTPUT).createTag());
    }

    private record ArrayState(BlockPos owner, int panels, List<BlockPos> controllers) {}

    public static int countIlluminatedPanels(ServerLevel level, BlockPos controller) {
        ArrayState array = inspectArray(level, controller);
        return controller.equals(array.owner()) ? array.panels() : 0;
    }

    private static ArrayState inspectArray(ServerLevel level, BlockPos controller) {
        var pending = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        var controllers = new ArrayList<BlockPos>();
        BlockPos owner = controller;
        int panels = 0;
        pending.add(controller);
        visited.add(controller);
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (!level.isLoaded(pos)) return new ArrayState(controller, 0, controllers);
            BlockState state = level.getBlockState(pos);
            if (state.is(GCBlocks.SOLAR_ARRAY_CONTROLLER)) {
                controllers.add(pos);
                if (pos.compareTo(owner) < 0) owner = pos;
            } else if (state.is(GCBlocks.SOLAR_ARRAY_MODULE) && hasClearSky(level, pos)) {
                panels++;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next)) continue;
                // Incomplete networks pause instead of generating twice across an unloaded border.
                if (!level.isLoaded(next)) return new ArrayState(controller, 0, controllers);
                BlockState neighbor = level.getBlockState(next);
                if (!neighbor.is(GCBlocks.SOLAR_ARRAY_MODULE) && !neighbor.is(GCBlocks.SOLAR_ARRAY_CONTROLLER)) continue;
                if (visited.size() >= MAX_NETWORK_BLOCKS) return new ArrayState(controller, 0, controllers);
                visited.add(next);
                pending.addLast(next);
            }
        }
        return new ArrayState(owner, panels, controllers);
    }

    private static boolean hasClearSky(ServerLevel level, BlockPos panel) {
        // Heightmaps update synchronously; skylight propagation can lag a newly placed roof.
        // This also works on celestial dimensions whose vanilla skylight is disabled.
        int top = Math.min(level.getMaxBuildHeight(), level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, panel.getX(), panel.getZ()));
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        for (int y = panel.getY() + 1; y < top; y++) {
            sample.set(panel.getX(), y, panel.getZ());
            if (level.getBlockState(sample).getLightBlock(level, sample) > 0) return false;
        }
        return true;
    }

    @Override
    protected void tickConstant(ServerLevel level, BlockPos pos, BlockState state, ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.drainPowerToSlot(0);
    }

    @Override
    protected MachineStatus tick(ServerLevel level, BlockPos pos, BlockState state, ProfilerFiller profiler) {
        LoadedEnergyOutput.spread(this, level, pos, state);
        long now = level.getGameTime();
        if (this.scannedAt == Long.MIN_VALUE || now - this.scannedAt >= 20 || now < this.scannedAt) {
            ArrayState array = inspectArray(level, pos);
            // Share the election with every connected controller, so a fresh controller or
            // a merged array cannot leave older controllers generating from a stale count.
            for (BlockPos participant : array.controllers()) {
                if (level.getBlockEntity(participant) instanceof SolarArrayControllerBlockEntity controller) {
                    controller.illuminatedPanels = participant.equals(array.owner()) ? array.panels() : 0;
                    controller.scannedAt = now;
                }
            }
            this.illuminatedPanels = pos.equals(array.owner()) ? array.panels() : 0;
            this.scannedAt = now;
        }
        double daylight = Math.max(0.0, Math.cos(level.getSunAngle(1.0F)));
        double weather = level.isThundering() ? 0.1 : level.isRaining() ? 0.5 : 1.0;
        weather *= MarsDustStormManager.solarMultiplier(level);
        long energy = Math.min(Galacticraft.CONFIG.solarArrayMaxProductionRate(),
                (long) (this.illuminatedPanels * 6.3 * daylight * weather));
        if (energy == 0) return MachineStatuses.IDLE;
        if (this.energyStorage().isFull()) return MachineStatuses.CAPACITOR_FULL;
        this.energyStorage().insert(energy);
        return MachineStatuses.ACTIVE;
    }

    @Override
    public MachineMenu<? extends MachineBlockEntity> createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu<>(GCMenuTypes.SOLAR_ARRAY_CONTROLLER, id, player, this);
    }
}
