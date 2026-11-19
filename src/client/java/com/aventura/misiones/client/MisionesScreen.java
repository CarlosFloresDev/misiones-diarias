package com.aventura.misiones.client;

import com.aventura.misiones.net.MisionesPayloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Panel de misiones: las 3 del dia y el desafio semanal, con progreso, recompensas (fichas y premio extra),
 * botones para reclamar o cambiar, tiempo para las nuevas y opcion de mostrar el panel lateral.
 */
public class MisionesScreen extends Screen {
	private static final int W = 340;
	private static final int H = 238;
	private static final int FILA = 36;
	private static final int PRIMERA = 34;
	/** Columna de la recompensa (fichas + premio extra), antes de los botones. */
	private static final int RECOMPENSA_X = W - 70 - 68;
	private static final int SEMANAL_Y = PRIMERA + 3 * FILA + 14;
	private static final String[] DIFICULTAD = {"FÁCIL", "MEDIA", "DIFÍCIL", "SEMANAL"};
	private static final int[] COLOR_DIF = {0xFF4CAF50, 0xFFF5A623, 0xFFE53935, 0xFF9C27B0};
	/** Como se completa cada tipo (mismo orden que Mision.Tipo). */
	private static final String[] AYUDA = {
		"Rompe esos bloques (en supervivencia).",
		"Derrota a esas criaturas.",
		"Tenlos en tu inventario: se entregan al reclamar.",
		"Ve a las coordenadas con los objetos (a menos de 6 bloques).",
		"Entra a ese bioma.",
		"Cuenta lo que fabriques o cocines desde que empezó la misión.",
		"Cuenta desde que empezó la misión (pesca, crianza, comercio, distancia...)."
	};

	private MisionesPayloads.Sync datos;

	public MisionesScreen(MisionesPayloads.Sync datos) {
		super(Component.literal("Misiones Diarias"));
		this.datos = datos;
	}

	public void actualizar(MisionesPayloads.Sync nuevos) {
		this.datos = nuevos;
		this.rebuildWidgets();
	}

	private int left() {
		return (this.width - W) / 2;
	}

	private int top() {
		return Math.max(2, (this.height - H) / 2);
	}

	private int filaY(int indice) {
		return this.top() + (indice >= 3 ? SEMANAL_Y : PRIMERA + indice * FILA);
	}

	private static void accion(int accion, int indice) {
		ClientPlayNetworking.send(new MisionesPayloads.Accion(accion, indice));
	}

	@Override
	protected void init() {
		boolean algoPorReclamar = false;
		for (int i = 0; i < this.datos.misiones().size(); i++) {
			MisionesPayloads.Vista v = this.datos.misiones().get(i);
			int y = this.filaY(i) + 8;
			int x = this.left() + W - 70;
			final int indice = i;
			if (v.completa() && !v.reclamada()) {
				algoPorReclamar = true;
				this.addRenderableWidget(Button.builder(Component.literal("¡Reclamar!").withStyle(ChatFormatting.GOLD),
					b -> accion(MisionesPayloads.Accion.RECLAMAR, indice)).bounds(x, y, 62, 18).build());
			} else if (i < 3 && !v.completa() && !v.reclamada() && !this.datos.cambioUsado()) {
				this.addRenderableWidget(Button.builder(Component.literal("Cambiar"),
						b -> accion(MisionesPayloads.Accion.CAMBIAR, indice)).bounds(x, y, 62, 18)
					.tooltip(Tooltip.create(Component.literal("Cambia esta misión por otra (1 vez al día)"))).build());
			}
		}
		int by = this.top() + H - 24;
		Button todo = Button.builder(Component.literal("Reclamar todo"), b -> accion(MisionesPayloads.Accion.RECLAMAR_TODO, 0))
			.bounds(this.left() + 8, by, 100, 20).build();
		todo.active = algoPorReclamar;
		this.addRenderableWidget(todo);
		this.addRenderableWidget(Button.builder(this.textoHud(), b -> {
				MisionesClient.hud = !MisionesClient.hud;
				MisionesClient.guardarOpciones();
				b.setMessage(this.textoHud());
			}).bounds(this.left() + W / 2 - 60, by, 120, 20)
			.tooltip(Tooltip.create(Component.literal("Muestra tus misiones al costado de la pantalla mientras juegas"))).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
			.bounds(this.left() + W - 108, by, 100, 20).build());
	}

	private Component textoHud() {
		return Component.literal("Panel lateral: " + (MisionesClient.hud ? "Sí" : "No"));
	}

	private static String tiempo(int segundos) {
		int h = segundos / 3600;
		int m = segundos % 3600 / 60;
		return h > 0 ? h + " h " + m + " min" : m + " min";
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		int l = this.left();
		int t = this.top();
		// panel
		g.fill(l - 2, t - 2, l + W + 2, t + H + 2, 0xFF1C1C22);
		g.fill(l, t, l + W, t + H, 0xF0EEF1F4);
		g.fill(l, t, l + W, t + 20, 0xFF2B6CB0);
		g.text(this.font, Component.literal("★ Misiones de hoy").withStyle(ChatFormatting.BOLD), l + 8, t + 6, 0xFFFFFFFF);
		int racha = this.datos.racha();
		String textoRacha = "Racha: " + racha + (racha == 1 ? " día" : " días") + (racha > 0 ? " (+" + Math.min(50, racha * 10) + "%)" : "");
		g.text(this.font, textoRacha, l + W - 8 - this.font.width(textoRacha), t + 6, 0xFFFFE082);
		g.text(this.font, "Nuevas en " + tiempo(this.datos.segundosDia()), l + 8, t + 24, 0xFF607D8B, false);
		String total = "Completadas en total: " + this.datos.total();
		g.text(this.font, total, l + W - 8 - this.font.width(total), t + 24, 0xFF607D8B, false);

		int hovered = -1;
		for (int i = 0; i < this.datos.misiones().size(); i++) {
			int y = this.filaY(i);
			if (i == 3) {
				String cab = "◆ Desafío semanal · termina en " + this.datos.diasSemana() + (this.datos.diasSemana() == 1 ? " día" : " días");
				g.text(this.font, Component.literal(cab).withStyle(ChatFormatting.BOLD), l + 8, y - 11, 0xFF7B1FA2);
			}
			this.fila(g, this.datos.misiones().get(i), l, y);
			if (mouseX >= l + 6 && mouseX < l + W - 72 && mouseY >= y && mouseY < y + FILA - 3) {
				hovered = i;
			}
		}
		String bono = this.datos.bonoCobrado() ? "★ ¡Bono diario cobrado!"
			: "Completa las 3 de hoy: bono +" + this.datos.bono() + " fichas" + (this.datos.cambioUsado() ? "" : " · puedes cambiar 1 misión");
		g.text(this.font, bono, l + 8, t + H - 36, this.datos.bonoCobrado() ? 0xFF2E7D32 : 0xFF555555, false);
		super.extractRenderState(g, mouseX, mouseY, a);

		if (hovered >= 0) {
			MisionesPayloads.Vista v = this.datos.misiones().get(hovered);
			List<Component> lineas = new ArrayList<>();
			lineas.add(Component.literal(v.nombre()).withStyle(ChatFormatting.GOLD));
			lineas.add(Component.literal(AYUDA[Math.min(AYUDA.length - 1, v.tipo())]).withStyle(ChatFormatting.GRAY));
			lineas.add(Component.literal("Recompensa: " + v.recompensa() + " fichas" + (racha > 0 ? " (+" + Math.min(50, racha * 10) + "% por racha)" : ""))
				.withStyle(ChatFormatting.YELLOW));
			ItemStack botin = botin(v.botin());
			if (!botin.isEmpty()) {
				lineas.add(Component.literal("Premio extra: " + botin.getCount() + " x ").append(botin.getHoverName()).withStyle(ChatFormatting.AQUA));
			}
			g.setComponentTooltipForNextFrame(this.font, lineas, mouseX, mouseY);
		}
	}

	private void fila(GuiGraphicsExtractor g, MisionesPayloads.Vista v, int l, int y) {
		int dif = Math.min(3, v.dificultad());
		g.fill(l + 6, y, l + W - 6, y + FILA - 3, v.reclamada() ? 0xFFD7E8D0 : dif == 3 ? 0xFFF3E5F5 : 0xFFFFFFFF);
		g.fill(l + 6, y, l + 9, y + FILA - 3, COLOR_DIF[dif]);
		ItemStack icono = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(v.icono())));
		g.item(icono, l + 13, y + 9);
		g.text(this.font, DIFICULTAD[dif], l + 34, y + 3, COLOR_DIF[dif], false);
		int rx = l + RECOMPENSA_X;
		int nx = l + 34 + this.font.width(DIFICULTAD[dif]) + 6;
		String nombre = this.font.plainSubstrByWidth(v.nombre(), rx - nx - 4);
		if (!nombre.equals(v.nombre())) {
			nombre = this.font.plainSubstrByWidth(v.nombre(), rx - nx - 4 - this.font.width("…")) + "…";
		}
		g.text(this.font, nombre, nx, y + 3, 0xFF1C1C22, false);
		// barra de progreso
		int bx = l + 34;
		int by = y + 14;
		int bw = rx - bx - 8;
		g.fill(bx, by, bx + bw, by + 8, 0xFFB0BEC5);
		int lleno = (int) (bw * Math.min(1.0, v.progreso() / (double) Math.max(1, v.meta())));
		g.fill(bx, by, bx + lleno, by + 8, v.completa() ? 0xFF4CAF50 : dif == 3 ? 0xFF9C27B0 : 0xFF2B6CB0);
		String prog = v.progreso() + " / " + v.meta();
		g.text(this.font, prog, bx + bw / 2 - this.font.width(prog) / 2, by, 0xFF1C1C22, false);
		if (!v.extra().isEmpty()) {
			g.text(this.font, this.font.plainSubstrByWidth(v.extra(), rx - bx), l + 34, y + 24, 0xFF555555, false);
		} else if (v.reclamada()) {
			g.text(this.font, "✔ Reclamada", l + 34, y + 24, 0xFF2E7D32, false);
		}
		// recompensa: fichas y premio extra
		g.item(new ItemStack(ficha()), rx, y + 9);
		g.text(this.font, "+" + v.recompensa(), rx + 17, y + 13, 0xFF8D6E00, false);
		ItemStack botin = botin(v.botin());
		if (!botin.isEmpty()) {
			g.item(botin, rx + 46, y + 9);
			g.itemDecorations(this.font, botin, rx + 46, y + 9);
		}
	}

	private static Item ficha() {
		return BuiltInRegistries.ITEM.getOptional(Identifier.parse("arequipa:ficha")).orElse(Items.EMERALD);
	}

	static ItemStack botin(String texto) {
		if (texto == null || texto.isEmpty()) {
			return ItemStack.EMPTY;
		}
		String[] partes = texto.split("\\*");
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(partes[0])).orElse(Items.AIR);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, partes.length > 1 ? Integer.parseInt(partes[1]) : 1);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (MisionesClient.ABRIR.matches(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
