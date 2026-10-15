/*
 * Copyright (c) 2021-2026 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.dynamicdimensions.impl.forge.gametest;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.impl.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static dev.galacticraft.dynamicdimensions.impl.gametest.Assertions.*;

public final class DynamicDimensionsGametest {
    private static final ResourceLocation TEST_LEVEL = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "neoforge_tick_test");

    /**
     * Checks that a runtime-created level enters NeoForge's cached world tick array.
     * @param context GameTest context
     */
    @GameTest(template = "empty", timeoutTicks = 6)
    public void runtimeDimensionTicks(@NotNull GameTestHelper context) {
        final MinecraftServer server = context.getLevel().getServer();
        final ServerLevel overworld = server.overworld();
        final DynamicDimensionRegistry dynRegistry = DynamicDimensionRegistry.from(server);
        final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, TEST_LEVEL);
        final AtomicBoolean tickObserved = new AtomicBoolean();
        final Consumer<LevelTickEvent.Post> tickListener = event -> {
            if (event.getLevel().dimension().equals(key)) {
                tickObserved.set(true);
            }
        };

        assertNotNull(overworld);
        assertFalse(dynRegistry.anyDimensionExists(TEST_LEVEL));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, tickListener);

        try {
            assertNotNull(dynRegistry.createDynamicDimension(TEST_LEVEL, overworld.getChunkSource().getGenerator(), createDimensionType()));
        } catch (RuntimeException | Error exception) {
            NeoForge.EVENT_BUS.unregister(tickListener);
            throw exception;
        }

        context.runAfterDelay(2, () -> {
            NeoForge.EVENT_BUS.unregister(tickListener);
            try {
                assertTrue(tickObserved.get());
            } finally {
                dynRegistry.deleteDynamicDimension(TEST_LEVEL, (server1, player) -> player.disconnect());
            }
            context.runAfterDelay(1, context::succeed);
        });
    }

    private static @NotNull DimensionType createDimensionType() {
        return new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0));
    }
}
