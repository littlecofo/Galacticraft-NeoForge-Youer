package dev.galacticraft.neoforge.gametest;

import dev.galacticraft.mod.attachments.GCServerPlayer;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.ParachestBlockEntity;
import dev.galacticraft.mod.content.entity.ParachestEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.teleporters.StationArrivalCargo;
import dev.galacticraft.impl.universe.celestialbody.type.SatelliteWorldMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.function.Consumer;

@PrefixGameTestTemplate(false)
public final class StationArrivalTests {
    @GameTest(template = "dynamicdimensionsgametest.empty", timeoutTicks = 120)
    public void realStationArrivalHasZeroGravityAndReturnsRocket(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var registry = server.registryAccess().registryOrThrow(dev.galacticraft.api.registry.AddonRegistries.CELESTIAL_BODY);
        var parent = registry.holders().filter(holder -> holder.value().type() instanceof
                dev.galacticraft.api.universe.celestialbody.landable.Landable landable
                && landable.world(holder.value().config()).equals(net.minecraft.world.level.Level.OVERWORLD)).findFirst().orElseThrow();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("16b52572-790e-4cb6-b391-bf72ce41aa4e"), "station_probe"));
        var station = dev.galacticraft.impl.universe.celestialbody.type.SatelliteType.registerSatellite(server,
                player, parent.key(), new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate(), registry);
        h.runAfterDelay(2, () -> {
        var level = server.getLevel(station.config().getWorld());
        h.assertTrue(level != null, "station level missing");
        System.out.println("STATION_PROBE dimension=" + level.dimension() + " body=" + level.galacticraft$getCelestialBody() + " resolved=" + ((dev.galacticraft.impl.internal.accessor.InternalLevelBodyAccessor) level).galacticraft$getResolvedCelestialBody() + " registry=" + registry.size() + " levelRegistry=" + level.registryAccess().registryOrThrow(dev.galacticraft.api.registry.AddonRegistries.CELESTIAL_BODY).size() + " stored=" + registry.get(station.config().getId()));
        h.assertTrue(level.galacticraft$getCelestialBody() != null, "station body missing");
        h.assertTrue(level.galacticraft$getCelestialBody().value().gravity() == 0, "station still uses Earth gravity");
        var item = new net.minecraft.world.entity.item.ItemEntity(level, 10.5, 75, 3.5, new ItemStack(Items.STONE));
        item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        item.tick();
        h.assertTrue(Math.abs(item.getDeltaMovement().y) < 0.000001, "item accelerated under station gravity");
        var arrivalPlayer = new net.neoforged.neoforge.common.util.FakePlayer(level, player.getGameProfile());
        var data = GCServerPlayer.get(arrivalPlayer);
        var saved = flight();
        data.setRocketStacks(saved.getRocketStacks()); data.setFuel(saved.getFuel());
        dev.galacticraft.impl.universe.celestialbody.landable.teleporter.type.DirectCelestialTeleporterType.INSTANCE
                .onEnterAtmosphere(level, arrivalPlayer, station, parent.value(),
                        dev.galacticraft.impl.universe.celestialbody.landable.teleporter.config.DefaultCelestialTeleporterConfig.INSTANCE);
        h.assertTrue(arrivalPlayer.serverLevel() == level, "player did not arrive");
        arrivalPlayer.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); arrivalPlayer.setOnGround(false);
        arrivalPlayer.travel(net.minecraft.world.phys.Vec3.ZERO);
        h.assertTrue(Math.abs(arrivalPlayer.getDeltaMovement().y) < 0.000001, "player accelerated under station gravity");
        var chests = level.getEntitiesOfClass(ParachestEntity.class, arrivalPlayer.getBoundingBox().inflate(10));
        h.assertTrue(chests.size() == 1 && chests.getFirst().cargo.get(82).is(GCItems.ROCKET), "real station arrival failed to deliver rocket");
        h.assertTrue(chests.getFirst().cargo.get(81).getCount() == 9, "real station arrival failed to deliver pads");
        h.assertTrue(data.getRocketStacks().stream().allMatch(ItemStack::isEmpty), "station source cargo retained");
        chests.getFirst().discard(); arrivalPlayer.discard();
        ((dev.galacticraft.api.accessor.SatelliteAccessor) server).galacticraft$removeSatellite(station.config().getId());
        h.succeed();
        });
    }
    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void ordinaryWorldsShareCanonicalEarthAndPacketEncodes(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var registry = server.registryAccess().registryOrThrow(dev.galacticraft.api.registry.AddonRegistries.CELESTIAL_BODY);
        var earth = registry.getHolderOrThrow(dev.galacticraft.mod.content.GCCelestialBodies.EARTH);
        var earthDestination = ((dev.galacticraft.api.universe.celestialbody.landable.Landable) earth.value().type()).world(earth.value().config());
        h.assertTrue(earthDestination.equals(net.minecraft.world.level.Level.OVERWORLD), "Earth destination changed");
        for (var key : java.util.List.of(net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.END)) {
            var level = server.getLevel(key);
            h.assertTrue(level != null && level.galacticraft$getCelestialBody() == earth, "ordinary world has a different Earth holder: " + key);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
            try {
                var packet = new dev.galacticraft.mod.network.s2c.OpenCelestialScreenPayload(null, level.galacticraft$getCelestialBody());
                dev.galacticraft.mod.network.s2c.OpenCelestialScreenPayload.STREAM_CODEC.encode(buffer, packet);
                var decoded = dev.galacticraft.mod.network.s2c.OpenCelestialScreenPayload.STREAM_CODEC.decode(buffer);
                h.assertTrue(decoded.celestialBody() == earth, "packet did not retain canonical Earth");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void protectedWorldsRejectExistingAndDirectMeteors(GameTestHelper h) {
        var level = h.getLevel().getServer().overworld();
        var pos = level.getSharedSpawnPos().above(20);
        var old = level.getBlockState(pos);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        try {
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos);
            chest.setItem(0, new ItemStack(Items.DIAMOND, 17));
            var meteor = new dev.galacticraft.mod.content.entity.FallingMeteorEntity(dev.galacticraft.mod.content.GCEntityTypes.FALLING_METEOR, level);
            meteor.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.above()));
            meteor.tick();
            h.assertTrue(meteor.isRemoved(), "existing meteor not discarded in world");
            dev.galacticraft.mod.world.dimension.meteor.MeteorImpact.strike(level, net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    new dev.galacticraft.mod.world.dimension.meteor.MeteoroidState(1000000, 0, -1000, 0, 0),
                    dev.galacticraft.mod.world.dimension.meteor.MeteoroidClass.IRON, 7, 20, 5);
            h.assertTrue(level.getBlockEntity(pos) == chest && chest.getItem(0).getCount() == 17, "direct impact damaged protected world");
            h.assertTrue(!dev.galacticraft.mod.world.dimension.meteor.MeteorShowerManager.isCelestialWorld(level), "protected world still admits spawning");
        } finally { level.setBlockAndUpdate(pos, old); }
        h.succeed();
    }

    private static GCServerPlayer flight() {
        GCServerPlayer data = new GCServerPlayer();
        NonNullList<ItemStack> cargo = NonNullList.withSize(83, ItemStack.EMPTY);
        cargo.set(0, new ItemStack(Items.DIAMOND, 17));
        cargo.set(80, new ItemStack(Items.EMERALD, 23));
        data.setRocketStacks(cargo);
        data.setLaunchpadStack(new ItemStack(GCBlocks.ROCKET_LAUNCH_PAD, 9));
        data.finishReturnInventory(new ItemStack(GCItems.ROCKET));
        data.setFuel(81000);
        return data;
    }

    @GameTest(template = "dynamicdimensionsgametest.empty", timeoutTicks = 120)
    public void stationCargoLandsAndSurvivesReload(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(0, 2, 0));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        GCServerPlayer data = flight();
        h.assertTrue(StationArrivalCargo.deliver(h.getLevel(), data, pos, "test"), "delivery failed");
        h.assertTrue(data.getRocketStacks().stream().allMatch(ItemStack::isEmpty), "source cargo retained after delivery");
        h.assertTrue(data.getFuel() == 0, "source fuel retained");
        h.assertTrue(!StationArrivalCargo.deliver(h.getLevel(), data, pos, "test"), "duplicate delivery");
        var couriers = h.getLevel().getEntitiesOfClass(ParachestEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(10));
        h.assertTrue(couriers.size() == 1, "courier missing after acceptance");
        for (int tick = 0; tick < 40 && !couriers.getFirst().isRemoved(); tick++) couriers.getFirst().tick();
        System.out.println("COURIER_PROBE target=" + pos + " actual=" + couriers.getFirst().position() + " ground=" + couriers.getFirst().onGround() + " removed=" + couriers.getFirst().isRemoved() + " targetBlock=" + h.getLevel().getBlockState(pos));
        BlockPos chestPos = couriers.getFirst().blockPosition();
        h.succeedWhen(() -> {
            h.assertTrue(h.getLevel().getBlockEntity(chestPos) instanceof ParachestBlockEntity, "courier has not landed");
            ParachestBlockEntity chest = (ParachestBlockEntity) h.getLevel().getBlockEntity(chestPos);
            CompoundTag saved = chest.saveWithFullMetadata(h.getLevel().registryAccess());
            chest = new ParachestBlockEntity(chestPos, chest.getBlockState());
            chest.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(chest.getItem(0).getCount() == 17, "first cargo lost");
            h.assertTrue(chest.getItem(80).getCount() == 23, "last cargo lost");
            h.assertTrue(chest.getItem(81).is(GCBlocks.ROCKET_LAUNCH_PAD.asItem()) && chest.getItem(81).getCount() == 9, "launch pads lost");
            h.assertTrue(chest.getItem(82).is(GCItems.ROCKET), "rocket lost");
            h.assertTrue(chest.tank.getFluidAmount() == 1000, "fuel lost on reload");
        });
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void rejectedCourierKeepsPlayerCargo(GameTestHelper h) {
        GCServerPlayer data = flight();
        Consumer<EntityJoinLevelEvent> reject = event -> {
            if (event.getLevel() == h.getLevel() && event.getEntity() instanceof ParachestEntity) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, reject);
        try {
            h.assertTrue(!StationArrivalCargo.deliver(h.getLevel(), data, h.absolutePos(BlockPos.ZERO), "rejected-test"), "rejection ignored");
            h.assertTrue(data.getRocketStacks().get(82).is(GCItems.ROCKET), "rocket cleared on rejection");
            h.assertTrue(data.getRocketStacks().get(81).getCount() == 9, "pads cleared on rejection");
            h.assertTrue(data.getFuel() == 81000, "fuel cleared on rejection");
        } finally { NeoForge.EVENT_BUS.unregister(reject); }
        h.succeed();
    }

    @GameTest(template = "dynamicdimensionsgametest.empty")
    public void stationGravityMigrationPreservesWeightlessness(GameTestHelper h) {
        h.assertTrue(SatelliteWorldMigration.repairLegacyGravity(0) == 0, "zero gravity overwritten");
        h.assertTrue(SatelliteWorldMigration.repairLegacyGravity(1) == 0, "Earth-gravity station not repaired");
        h.assertTrue(SatelliteWorldMigration.repairLegacyGravity(0.16F) == 0.16F, "custom gravity overwritten");
        h.succeed();
    }
}
