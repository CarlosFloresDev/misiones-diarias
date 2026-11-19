package com.aventura.misiones.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Boton cuadrado de 20x20 con el icono de un item. */
public class BotonIcono extends AbstractButton {
	private final ItemStack icono;
	private final Consumer<BotonIcono> accion;

	public BotonIcono(int x, int y, ItemStack icono, Consumer<BotonIcono> accion) {
		super(x, y, 20, 20, Component.literal("Misiones"));
		this.icono = icono;
		this.accion = accion;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.accion.accept(this);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractDefaultSprite(graphics);
		graphics.item(this.icono, this.getX() + 2, this.getY() + 2);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
