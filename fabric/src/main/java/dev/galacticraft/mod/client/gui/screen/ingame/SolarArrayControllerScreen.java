package dev.galacticraft.mod.client.gui.screen.ingame;

import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.SolarArrayControllerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SolarArrayControllerScreen extends MachineScreen<SolarArrayControllerBlockEntity, MachineMenu<SolarArrayControllerBlockEntity>> {
    public SolarArrayControllerScreen(MachineMenu<SolarArrayControllerBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title, Constant.id("textures/gui/geothermal_generator.png"));
    }
}
