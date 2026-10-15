package dev.galacticraft.neoforge.gametest;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.machine.GeothermalGeneratorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.SolarArrayControllerBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.machinelib.api.machine.configuration.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class GC4RestorationTests {
    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void newGeneratorOutputDoesNotLoadNeighborChunks(GameTestHelper h) {
        var level=h.getLevel(); var origin=h.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((Math.floorDiv(origin.getX(),16)+4096)*16+15,origin.getY()+35,
                (Math.floorDiv(origin.getZ(),16)+4096)*16+8);
        var target=pos.east();
        h.assertTrue(!level.isLoaded(target),"chunk guard fixture is unexpectedly loaded");
        var state=GCBlocks.GEOTHERMAL_GENERATOR.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                net.minecraft.core.Direction.WEST);
        var machine=new GeothermalGeneratorBlockEntity(pos,state);
        machine.energyStorage().insert(1000);
        dev.galacticraft.mod.content.block.entity.machine.LoadedEnergyOutput.spread(machine,level,pos,state);
        h.assertTrue(!level.isLoaded(target),"native output loaded the neighboring chunk");
        h.assertTrue(machine.energyStorage().getAmount()==1000,"unloaded output lost stored energy");
        h.succeed();
    }
    private BlockPos position(GameTestHelper h) { return h.absolutePos(new BlockPos(1, 35, 1)); }
    private GeothermalGeneratorBlockEntity setup(GameTestHelper h, int depth) {
        var level = h.getLevel(); var pos = position(h);
        for (int y = 1; y <= 20; y++) level.setBlock(pos.below(y), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.below(), GCBlocks.VAPOR_SPOUT.defaultBlockState(), 2);
        level.setBlock(pos.below(depth), GCBlocks.SULFURIC_ACID.defaultBlockState(), 2);
        level.setBlock(pos, GCBlocks.GEOTHERMAL_GENERATOR.defaultBlockState(), 3);
        return (GeothermalGeneratorBlockEntity) level.getBlockEntity(pos);
    }
    private void tick(GameTestHelper h, GeothermalGeneratorBlockEntity machine) {
        machine.tickBase(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), h.getLevel().getProfiler());
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void geothermalGeneratesAndStopsWithoutFuel(GameTestHelper h) {
        var machine = setup(h, 5); var pos = position(h);
        tick(h, machine);
        long amount = machine.energyStorage().getAmount();
        h.assertTrue(amount > 0 && amount <= 200, "valid heat did not generate bounded power");
        h.getLevel().setBlock(pos.below(3), Blocks.STONE.defaultBlockState(), 2);
        tick(h, machine);
        h.assertTrue(machine.energyStorage().getAmount() == amount, "blocked shaft kept generating");
        h.assertTrue(machine.getState().getStatus() == GeothermalGeneratorBlockEntity.NO_HEAT, "missing heat status absent");
        h.getLevel().setBlock(pos.below(3), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(pos.below(5), Blocks.LAVA.defaultBlockState(), 2);
        tick(h, machine);
        h.assertTrue(machine.energyStorage().getAmount() == amount, "lava substituted for sulfuric acid");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void geothermalDepthAndVentRequirements(GameTestHelper h) {
        var machine = setup(h, 19); var pos = position(h); var level = h.getLevel();
        h.assertTrue(GeothermalGeneratorBlockEntity.hasHeatSource(level, pos), "depth 19 rejected");
        level.setBlock(pos.below(19), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.below(20), GCBlocks.SULFURIC_ACID.defaultBlockState(), 2);
        h.assertTrue(!GeothermalGeneratorBlockEntity.hasHeatSource(level, pos), "depth 20 exceeded bounded scan");
        level.setBlock(pos.below(2), GCBlocks.SULFURIC_ACID.defaultBlockState(), 2);
        h.assertTrue(GeothermalGeneratorBlockEntity.hasHeatSource(level, pos), "depth 2 rejected");
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
        h.assertTrue(!GeothermalGeneratorBlockEntity.hasHeatSource(level, pos), "no vent accepted");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void geothermalExportsNativeEnergyWithoutDuplication(GameTestHelper h) {
        var machine = setup(h,4); var level=h.getLevel(); var pos=position(h);
        level.setBlock(pos.south(),GCBlocks.ENERGY_STORAGE_MODULE.defaultBlockState(),3);
        var receiver=(dev.galacticraft.mod.content.block.entity.machine.EnergyStorageModuleBlockEntity)level.getBlockEntity(pos.south());
        receiver.getIOConfig().get(dev.galacticraft.machinelib.api.util.BlockFace.FRONT).setOption(
                dev.galacticraft.machinelib.api.transfer.ResourceType.ENERGY,
                dev.galacticraft.machinelib.api.transfer.ResourceFlow.INPUT);
        machine.energyStorage().insert(1000);
        tick(h,machine);
        h.assertTrue(receiver.energyStorage().getAmount()>0,"native energy output did not reach storage module");
        long expected=1000+GeothermalGeneratorBlockEntity.productionAt(level.getGameTime(),dev.galacticraft.mod.Galacticraft.CONFIG.geothermalGeneratorMaxProductionRate());
        h.assertTrue(machine.energyStorage().getAmount()+receiver.energyStorage().getAmount()==expected,"native transfer duplicated or lost energy");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void geothermalChargesBatteryAndPersists(GameTestHelper h) {
        var machine = setup(h, 4);
        machine.energyStorage().insert(10000);
        machine.itemStorage().slot(0).set(GCItems.BATTERY, net.minecraft.core.component.DataComponentPatch.EMPTY, 1);
        tick(h, machine);
        var battery = machine.itemStorage().getItem(0).getCapability(Capabilities.EnergyStorage.ITEM);
        h.assertTrue(battery != null && battery.getEnergyStored() > 0, "battery did not charge through native capability");
        machine.setRedstoneMode(RedstoneMode.HIGH);
        long before = machine.energyStorage().getAmount();
        machine.itemStorage().slot(0).extract(1);
        for (int i = 0; i < 4; i++) tick(h, machine);
        h.assertTrue(machine.energyStorage().getAmount() == before, "disabled generator generated");
        var tag = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new GeothermalGeneratorBlockEntity(machine.getBlockPos(), machine.getBlockState());
        restored.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(restored.energyStorage().getAmount() == before, "stored energy lost on reload");
        h.assertTrue(restored.getRedstoneMode() == RedstoneMode.HIGH, "redstone mode lost on reload");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void geothermalRecipeAndEnergyCapabilityAvailable(GameTestHelper h) {
        var machine = setup(h, 4);
        var capability = Capabilities.EnergyStorage.BLOCK.getCapability(h.getLevel(), machine.getBlockPos(), null, null, null);
        h.assertTrue(capability != null && capability.canExtract() && !capability.canReceive(), "generator capability missing or accepts external power");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(Constant.id("geothermal_generator")).isPresent(), "survival recipe missing");
        for (long time = 0; time < 700; time++) {
            long value = GeothermalGeneratorBlockEntity.productionAt(time, 200);
            h.assertTrue(value >= 30 && value <= 200, "production exceeded limits");
        }
        h.assertTrue(GeothermalGeneratorBlockEntity.productionAt(1, 0) == 0, "zero output config ignored");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void solarArrayConnectivityShadingAndControllerElection(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(1, 5, 1));
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) level.setBlock(pos.offset(x,y,0), Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(pos, GCBlocks.SOLAR_ARRAY_CONTROLLER.defaultBlockState(), 3);
        level.setBlock(pos.east(), GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(), 3);
        level.setBlock(pos.east(2), GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(), 3);
        level.setBlock(pos.east(4), GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(), 3);
        h.assertTrue(SolarArrayControllerBlockEntity.countIlluminatedPanels(level,pos)==2, "disconnected cell counted or connected cells lost");
        level.setBlock(pos.east().above(), Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(SolarArrayControllerBlockEntity.countIlluminatedPanels(level,pos)==1, "shaded cell still counted");
        level.setBlock(pos.east(3), GCBlocks.SOLAR_ARRAY_CONTROLLER.defaultBlockState(), 3);
        int a = SolarArrayControllerBlockEntity.countIlluminatedPanels(level,pos);
        int b = SolarArrayControllerBlockEntity.countIlluminatedPanels(level,pos.east(3));
        h.assertTrue(a+b==2 && (a==0 || b==0), "connected controllers counted cells twice");
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void solarArrayGeneratesAtDayStopsAtNightAndPersists(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(1,5,1));
        long oldTime = level.getDayTime();
        try {
            for (int x=0; x<3; x++) for (int y=0;y<5;y++) level.setBlock(pos.offset(x,y,0),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(pos, GCBlocks.SOLAR_ARRAY_CONTROLLER.defaultBlockState(),3);
            level.setBlock(pos.east(), GCBlocks.SOLAR_ARRAY_MODULE.defaultBlockState(),3);
            var machine = (SolarArrayControllerBlockEntity) level.getBlockEntity(pos);
            level.setDayTime(6000);
            machine.tickBase(level,pos,machine.getBlockState(),level.getProfiler());
            long amount=machine.energyStorage().getAmount();
            h.assertTrue(amount>0,"daytime array did not generate");
            level.setDayTime(18000);
            machine.tickBase(level,pos,machine.getBlockState(),level.getProfiler());
            h.assertTrue(machine.energyStorage().getAmount()==amount,"array generated at night");
            var tag=machine.saveWithFullMetadata(level.registryAccess());
            var restored=new SolarArrayControllerBlockEntity(pos,machine.getBlockState());
            restored.loadWithComponents(tag,level.registryAccess());
            h.assertTrue(restored.energyStorage().getAmount()==amount,"array energy lost on reload");
            h.assertTrue(level.getRecipeManager().byKey(Constant.id("solar_array_controller")).isPresent(),"controller recipe missing");
            h.assertTrue(level.getRecipeManager().byKey(Constant.id("solar_array_module")).isPresent(),"module recipe missing");
        } finally { level.setDayTime(oldTime); }
        h.succeed();
    }
}
