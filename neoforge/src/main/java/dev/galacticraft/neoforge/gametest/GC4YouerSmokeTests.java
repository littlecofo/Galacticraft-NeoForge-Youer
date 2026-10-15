package dev.galacticraft.neoforge.gametest;

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.machine.GeothermalGeneratorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.SolarArrayControllerBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.machinelib.api.machine.configuration.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Runs only with an explicit JVM switch in the isolated local server. */
public final class GC4YouerSmokeTests {
    private static MinecraftServer server;
    private static int ticks;
    private static BlockPos origin;
    private static final Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
    private static final Set<Long> forcedByTest = new HashSet<>();
    public static void started(ServerStartedEvent event) { server = event.getServer(); ticks = 0; }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void tick(ServerTickEvent.Post event) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        try {
            if (++ticks == 1) {
                origin = level.getSharedSpawnPos().offset(8, 35, 8);
                var chunk = new ChunkPos(origin);
                for (int x = chunk.x - 1; x <= chunk.x + 1; x++) for (int z = chunk.z - 1; z <= chunk.z + 1; z++) {
                    long key = ChunkPos.asLong(x,z);
                    if (!level.getForcedChunks().contains(key)) { level.setChunkForced(x,z,true); forcedByTest.add(key); }
                }
            }
            if (ticks == 60) {
                for (int x = -2; x < 9; x++) for (int y = -20; y < 6; y++) for (int z = -2; z <= 2; z++) {
                    BlockPos pos = origin.offset(x,y,z);
                    require(level.getBlockEntity(pos) == null, "fixture would overwrite an existing block entity");
                    originalBlocks.put(pos,level.getBlockState(pos));
                }
                for (BlockPos pos : originalBlocks.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(origin.below(),GCBlocks.VAPOR_SPOUT.defaultBlockState(),3);
                for (var direction : net.minecraft.core.Direction.values()) {
                    if (direction != net.minecraft.core.Direction.UP)
                        level.setBlock(origin.below(4).relative(direction),Blocks.STONE.defaultBlockState(),3);
                }
                level.setBlock(origin.below(4),GCBlocks.SULFURIC_ACID.defaultBlockState(),3);
                level.setBlock(origin,GCBlocks.GEOTHERMAL_GENERATOR.defaultBlockState(),3);
                var machine = (GeothermalGeneratorBlockEntity) level.getBlockEntity(origin);
                machine.itemStorage().slot(0).set(GCItems.BATTERY,net.minecraft.core.component.DataComponentPatch.EMPTY,1);
                require(Capabilities.EnergyStorage.BLOCK.getCapability(level,origin,null,null,null)!=null,"Youer generator capability unavailable");
                level.setBlock(origin.east(3),GCBlocks.SOLAR_ARRAY_CONTROLLER.defaultBlockState(),3);
                level.setBlock(origin.east(4),GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(),3);
                level.setBlock(origin.east(5),GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(),3);
                require(SolarArrayControllerBlockEntity.countIlluminatedPanels(level,origin.east(3))==2,"Youer array connectivity failed");
            }
            if (ticks == 100) {
                var machine=(GeothermalGeneratorBlockEntity)level.getBlockEntity(origin);
                var battery=machine.itemStorage().getItem(0).getCapability(Capabilities.EnergyStorage.ITEM);
                require(battery!=null && battery.getEnergyStored()>0,"real Youer ticking failed to charge a battery");
                machine.itemStorage().slot(0).extract(1);
                level.setBlock(origin.below(2),Blocks.STONE.defaultBlockState(),3);
                long before=machine.energyStorage().getAmount();
                machine.tickBase(level,origin,machine.getBlockState(),level.getProfiler());
                require(machine.energyStorage().getAmount()==before,"blocked source kept producing on Youer");
                machine.setRedstoneMode(RedstoneMode.HIGH);
                var tag=machine.saveWithFullMetadata(level.registryAccess());
                var restored=new GeothermalGeneratorBlockEntity(origin,machine.getBlockState());
                restored.loadWithComponents(tag,level.registryAccess());
                require(restored.energyStorage().getAmount()==before && restored.getRedstoneMode()==RedstoneMode.HIGH,"Youer saved energy/control state failed");
                for (String recipe : new String[]{"geothermal_generator","solar_array_controller","solar_array_module"})
                    require(level.getRecipeManager().byKey(dev.galacticraft.mod.Constant.id(recipe)).isPresent(),"Youer recipe missing: "+recipe);
                System.out.println("GC4_RESTORATION_YOUER_PASS: real ticking/battery charge; source removal; array connectivity; capabilities; recipes; energy/redstone persistence");
                finish(level);
            }
        } catch (Throwable failure) {
            System.out.println("GC4_RESTORATION_YOUER_FAILED"); failure.printStackTrace(); finish(level);
        }
    }

    private static void finish(ServerLevel level) {
        // The sulfuric-acid source is enclosed for the brief test; restore only recorded cells.
        for (Map.Entry<BlockPos,BlockState> entry : originalBlocks.entrySet()) level.setBlock(entry.getKey(),entry.getValue(),2);
        for (long key : forcedByTest) { var chunk=new ChunkPos(key);level.setChunkForced(chunk.x,chunk.z,false); }
        originalBlocks.clear();forcedByTest.clear();
        MinecraftServer finished=server;server=null;finished.halt(false);
    }
}
