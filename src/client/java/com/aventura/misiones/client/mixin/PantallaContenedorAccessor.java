package com.aventura.misiones.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Posicion del panel de un inventario (para poner el boton de misiones al lado). */
@Mixin(AbstractContainerScreen.class)
public interface PantallaContenedorAccessor {
	@Accessor("leftPos")
	int misiones$left();

	@Accessor("topPos")
	int misiones$top();

	@Accessor("imageWidth")
	int misiones$ancho();
}
