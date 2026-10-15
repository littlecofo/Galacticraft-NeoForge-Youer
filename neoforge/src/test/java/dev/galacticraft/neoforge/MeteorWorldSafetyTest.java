package dev.galacticraft.neoforge;
import dev.galacticraft.mod.world.dimension.meteor.MeteorWorldSafety;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MeteorWorldSafetyTest {
 @Test void protectsAllThreeRequestedWorldsAcrossWorldNamespaces() {
  assertTrue(MeteorWorldSafety.isProtectedDimension(Level.OVERWORLD));
  for (String ns : new String[]{"minecraft", "multiworld"}) for (String name : new String[]{"sc", "sc2", "world"})
   assertTrue(MeteorWorldSafety.isProtectedDimension(ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(ns,name))));
 }
 @Test void permitsBothResourceWorldsWithoutUnprotectingHomes() {
  for (String name : new String[]{"zysj", "zysj2"}) {
   var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("minecraft",name));
   assertTrue(MeteorWorldSafety.isResourceDimension(key));
   assertFalse(MeteorWorldSafety.isProtectedDimension(key));
  }
  assertFalse(MeteorWorldSafety.isResourceDimension(Level.OVERWORLD));
 }
 @Test void doesNotBanOtherCelestialDestinations() {
  for (String name : new String[]{"moon", "mars", "venus", "mercury", "asteroid", "earth_station_test"})
   assertFalse(MeteorWorldSafety.isProtectedDimension(ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("galacticraft",name))));
 }
}
