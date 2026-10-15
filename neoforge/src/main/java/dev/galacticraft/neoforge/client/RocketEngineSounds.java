package dev.galacticraft.neoforge.client;

import dev.galacticraft.api.rocket.LaunchStage;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.sounds.RocketSound;
import dev.galacticraft.mod.content.entity.vehicle.RocketEntity;
import net.minecraft.client.Minecraft;

import java.util.IdentityHashMap;
import java.util.Map;

/** Observes synchronized client entities instead of server-only launch setter callbacks. */
public final class RocketEngineSounds {
    private static final Map<RocketEntity, Loop> LOOPS = new IdentityHashMap<>();
    private static long ticks;

    private record Loop(RocketSound sound, long startedAt) {}

    public static void tick(Minecraft minecraft) {
        ticks++;
        var manager = minecraft.getSoundManager();
        LOOPS.entrySet().removeIf(entry -> {
            var rocket = entry.getKey();
            if (rocket.level() == minecraft.level && shouldPlay(rocket)) return false;
            manager.stop(entry.getValue().sound());
            return true;
        });
        if (minecraft.level == null) return;

        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof RocketEntity rocket) || !shouldPlay(rocket)) continue;
            var loop = LOOPS.get(rocket);
            if (loop != null) {
                // Allow asynchronous sound loading to finish, and resume after audio reloads.
                if (ticks - loop.startedAt() < 20 || manager.isActive(loop.sound())) continue;
                manager.stop(loop.sound());
            }
            var sound = new RocketSound(rocket);
            // Initialize volume before SoundEngine decides whether to allocate a channel.
            sound.tick();
            manager.play(sound);
            LOOPS.put(rocket, new Loop(sound, ticks));
        }
    }

    private static boolean shouldPlay(RocketEntity rocket) {
        var stage = rocket.getLaunchStage();
        return rocket.isAlive() && rocket.getY() <= Constant.SPACE_HEIGHT
                && (stage == LaunchStage.IGNITED || stage == LaunchStage.LAUNCHED);
    }
}
