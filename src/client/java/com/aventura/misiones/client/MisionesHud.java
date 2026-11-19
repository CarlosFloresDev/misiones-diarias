package com.aventura.misiones.client;

import com.aventura.misiones.net.MisionesPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Panel lateral derecho con las misiones que faltan: icono, nombre y barra de progreso. */
public final class MisionesHud {
	private static final int ANCHO = 150;
	private static final int FILA = 22;

	private MisionesHud() {
	}

	public static void dibujar(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		MisionesPayloads.Sync datos = MisionesClient.ultimo;
		if (!MisionesClient.hud || datos == null || mc.player == null || mc.gui.screen() != null || mc.debugEntries.isOverlayVisible()) {
			return;
		}
		List<MisionesPayloads.Vista> activas = new ArrayList<>();
		for (MisionesPayloads.Vista v : datos.misiones()) {
			if (!v.reclamada()) {
				activas.add(v);
			}
		}
		if (activas.isEmpty()) {
			return;
		}
		Font font = mc.font;
		int alto = 14 + activas.size() * FILA;
		int x = g.guiWidth() - ANCHO - 4;
		int y = g.guiHeight() / 2 - alto / 2 - 10;
		g.fill(x, y, x + ANCHO, y + alto, 0x90101018);
		g.fill(x, y, x + ANCHO, y + 12, 0xC02B6CB0);
		g.text(font, "★ Misiones", x + 4, y + 2, 0xFFFFFFFF, false);
		String tecla = "[" + MisionesClient.ABRIR.getTranslatedKeyMessage().getString() + "]";
		g.text(font, tecla, x + ANCHO - 4 - font.width(tecla), y + 2, 0xFFFFE082, false);
		int fy = y + 14;
		for (MisionesPayloads.Vista v : activas) {
			ItemStack icono = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(v.icono())));
			g.item(icono, x + 3, fy + 2);
			boolean semanal = v.dificultad() >= 3;
			String nombre = font.plainSubstrByWidth((semanal ? "◆ " : "") + v.nombre(), ANCHO - 26);
			g.text(font, nombre, x + 22, fy + 1, v.completa() ? 0xFF7CFC00 : semanal ? 0xFFE0A0FF : 0xFFFFFFFF, false);
			int bx = x + 22;
			int by = fy + 12;
			int bw = ANCHO - 26 - 34;
			g.fill(bx, by, bx + bw, by + 4, 0xFF37474F);
			int lleno = (int) (bw * Math.min(1.0, v.progreso() / (double) Math.max(1, v.meta())));
			g.fill(bx, by, bx + lleno, by + 4, v.completa() ? 0xFF4CAF50 : semanal ? 0xFFAB47BC : 0xFF42A5F5);
			String prog = v.completa() ? "¡Listo!" : compacto(v.progreso()) + "/" + compacto(v.meta());
			g.text(font, prog, x + ANCHO - 3 - font.width(prog), fy + 10, v.completa() ? 0xFF7CFC00 : 0xFFCFD8DC, false);
			fy += FILA;
		}
	}

	private static String compacto(int n) {
		return n >= 10000 ? (n / 1000) + "k" : Integer.toString(n);
	}
}
