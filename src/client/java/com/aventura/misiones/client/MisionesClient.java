package com.aventura.misiones.client;

import com.aventura.misiones.MisionesMod;
import com.aventura.misiones.client.mixin.PantallaContenedorAccessor;
import com.aventura.misiones.net.MisionesPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Cliente de Misiones: tecla J para abrir el panel, boton en el inventario, panel lateral en la pantalla (HUD)
 * y aviso cuando completas una mision.
 */
public class MisionesClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(MisionesMod.id("misiones"));
	public static final KeyMapping ABRIR = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.misiones.abrir", InputConstants.KEY_J, CATEGORIA));
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(6000L);

	/** Ultimo estado recibido del servidor (para el HUD). */
	static MisionesPayloads.@Nullable Sync ultimo;
	/** Mostrar el panel lateral con las misiones activas. */
	static boolean hud = true;

	@Override
	public void onInitializeClient() {
		cargarOpciones();
		ClientPlayNetworking.registerGlobalReceiver(MisionesPayloads.Sync.TYPE, (payload, context) -> {
			avisarCompletadas(context.client(), ultimo, payload);
			ultimo = payload;
			var gui = context.client().gui;
			if (gui.screen() instanceof MisionesScreen screen) {
				screen.actualizar(payload);
			} else if (payload.abrir()) {
				gui.setScreen(new MisionesScreen(payload));
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ultimo = null);

		// tecla J
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ABRIR.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					abrir();
				}
			}
		});

		// panel lateral
		HudElementRegistry.addLast(MisionesMod.id("seguimiento"), MisionesHud::dibujar);

		// boton en el inventario (arriba a la derecha del panel)
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen inventario) {
				BotonIcono boton = new BotonIcono(0, 0, new ItemStack(Items.WRITABLE_BOOK), b -> abrir());
				boton.setTooltip(Tooltip.create(Component.literal("Misiones diarias (J)")));
				Screens.getWidgets(screen).add(boton);
				ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
					PantallaContenedorAccessor acc = (PantallaContenedorAccessor) inventario;
					boton.setX(acc.misiones$left() + acc.misiones$ancho() - 20);
					boton.setY(acc.misiones$top() - 20);
				});
			}
		});
	}

	static void abrir() {
		ClientPlayNetworking.send(new MisionesPayloads.Accion(MisionesPayloads.Accion.ABRIR, 0));
	}

	/** Aviso (toast) cuando una mision pasa a completa. */
	private static void avisarCompletadas(Minecraft client, MisionesPayloads.@Nullable Sync antes, MisionesPayloads.Sync ahora) {
		if (antes == null || !antes.dia().equals(ahora.dia())) {
			return;
		}
		for (int i = 0; i < Math.min(antes.misiones().size(), ahora.misiones().size()); i++) {
			MisionesPayloads.Vista a = antes.misiones().get(i);
			MisionesPayloads.Vista b = ahora.misiones().get(i);
			if (a.nombre().equals(b.nombre()) && !a.completa() && b.completa() && !b.reclamada()) {
				SystemToast.addOrUpdate(client.gui.toastManager(), TOAST, Component.literal("✔ ¡Misión completada!"),
					Component.literal(b.nombre() + " · J para reclamar"));
			}
		}
	}

	// ---------------------------------------------------------------- opciones del cliente
	private static Path archivo() {
		return FabricLoader.getInstance().getConfigDir().resolve("misiones-cliente.properties");
	}

	private static void cargarOpciones() {
		Properties p = new Properties();
		try (Reader r = Files.newBufferedReader(archivo())) {
			p.load(r);
			hud = Boolean.parseBoolean(p.getProperty("hud", "true"));
		} catch (IOException ignored) {
			// primera vez: valores por defecto
		}
	}

	static void guardarOpciones() {
		Properties p = new Properties();
		p.setProperty("hud", Boolean.toString(hud));
		try (Writer w = Files.newBufferedWriter(archivo())) {
			p.store(w, "Misiones Diarias - opciones del cliente");
		} catch (IOException e) {
			MisionesMod.LOGGER.warn("No se pudo guardar {}", archivo(), e);
		}
	}
}
