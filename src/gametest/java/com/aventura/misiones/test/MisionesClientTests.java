package com.aventura.misiones.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Abre la pantalla de misiones con /misiones y saca una captura. */
public class MisionesClientTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("execute as @a run misiones");
			context.waitTicks(20);
			context.takeScreenshot("misiones_pantalla");
			context.setScreen(() -> null);
			context.waitTicks(30);
			context.takeScreenshot("misiones_hud");
			context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("misiones_boton_inventario");
			context.setScreen(() -> null);
		}
	}
}
