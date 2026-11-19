package com.aventura.misiones;

import com.aventura.misiones.data.GeneradorDeMisiones;
import com.aventura.misiones.data.Mision;
import com.aventura.misiones.data.MisionesJugador;
import com.aventura.misiones.net.MisionesPayloads;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** Logica de servidor de las misiones diarias y el desafio semanal. */
public final class Misiones {
	public static final AttachmentType<MisionesJugador> DATOS = AttachmentRegistry.<MisionesJugador>builder()
		.persistent(MisionesJugador.CODEC)
		.copyOnDeath()
		.initializer(MisionesJugador::vacio)
		.buildAndRegister(MisionesMod.id("misiones"));

	/** Bono por completar las 3 misiones del dia. */
	public static final int BONO_DIARIO = 30;
	/** Indice del desafio semanal en las acciones (despues de las 3 diarias). */
	public static final int SEMANAL = 3;

	/** Jugadores cuyo progreso cambio y hay que mandarles el estado (una vez por segundo como maximo). */
	private static final Set<UUID> PENDIENTES = new HashSet<>();

	private Misiones() {
	}

	public static String hoy() {
		return LocalDate.now().toString();
	}

	public static String semanaActual() {
		LocalDate hoy = LocalDate.now();
		return hoy.get(IsoFields.WEEK_BASED_YEAR) + "-W" + hoy.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
	}

	/** Datos del jugador, renovando las misiones si cambio el dia o la semana. */
	public static MisionesJugador de(ServerPlayer player) {
		MisionesJugador datos = player.getAttachedOrCreate(DATOS);
		String hoy = hoy();
		boolean cambio = false;
		if (!hoy.equals(datos.dia)) {
			boolean ayerCompleto = LocalDate.now().minusDays(1).toString().equals(datos.ultimoDiaCompleto);
			datos.racha = ayerCompleto ? datos.racha : 0;
			datos.dia = hoy;
			datos.misiones = new ArrayList<>(GeneradorDeMisiones.delDia(player.getUUID(), hoy, player.blockPosition()));
			datos.misiones.forEach(m -> iniciar(player, m));
			datos.cambioUsado = false;
			datos.bonoCobrado = false;
			cambio = true;
			player.sendSystemMessage(Component.literal("¡Nuevas misiones diarias! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
				.append(Component.literal("Presiona J (o usa /misiones) para verlas").withStyle(ChatFormatting.YELLOW)));
		}
		String semana = semanaActual();
		if (!semana.equals(datos.semana) || datos.semanal == null) {
			datos.semana = semana;
			datos.semanal = GeneradorDeMisiones.deLaSemana(player.getUUID(), semana);
			iniciar(player, datos.semanal);
			cambio = true;
		}
		if (cambio) {
			player.setAttached(DATOS, datos);
			PENDIENTES.add(player.getUUID());
		}
		return datos;
	}

	/** Las misiones por estadistica empiezan a contar desde ahora. */
	private static void iniciar(ServerPlayer player, Mision m) {
		if (m.usaEstadistica()) {
			m.base = estadistica(player, m);
		}
	}

	private static void guardar(ServerPlayer player, MisionesJugador datos) {
		player.setAttached(DATOS, datos);
		PENDIENTES.add(player.getUUID());
	}

	// ---------------------------------------------------------------- avance
	public static void alRomper(ServerPlayer player, BlockState state) {
		if (player.isCreative()) {
			return;
		}
		MisionesJugador datos = de(player);
		for (Mision m : datos.todas()) {
			if (m.tipo == Mision.Tipo.MINAR && !m.completa() && coincideBloque(m.objetivo, state)) {
				fijar(player, datos, m, m.progreso + 1);
			}
		}
	}

	public static void alMatar(ServerPlayer player, Entity victima) {
		MisionesJugador datos = de(player);
		String id = BuiltInRegistries.ENTITY_TYPE.getKey(victima.getType()).toString();
		boolean monstruo = victima.getType().getCategory() == MobCategory.MONSTER;
		for (Mision m : datos.todas()) {
			if (m.tipo == Mision.Tipo.CAZAR && !m.completa() && (m.objetivo.equals(id) || monstruo && m.objetivo.equals("#monstruo"))) {
				fijar(player, datos, m, m.progreso + 1);
			}
		}
	}

	/** Se llama cada segundo: inventario, bioma, posicion y estadisticas; y manda el estado si cambio. */
	public static void cadaSegundo(ServerPlayer player) {
		MisionesJugador datos = de(player);
		for (Mision m : datos.todas()) {
			if (m.reclamada) {
				continue;
			}
			switch (m.tipo) {
				// "Junta X": el progreso es lo que tienes ahora (se entrega al reclamar)
				case CONSEGUIR -> {
					int tiene = Math.min(m.meta, contar(player, m.objetivo));
					if (tiene != m.progreso) {
						fijar(player, datos, m, tiene);
					}
				}
				case EXPLORAR -> {
					boolean dentro = player.level().getBiome(player.blockPosition()).unwrapKey()
						.map(k -> k.identifier().toString().equals(m.objetivo)).orElse(false);
					if (dentro && !m.completa()) {
						fijar(player, datos, m, 1);
					}
				}
				case ENTREGAR -> {
					if (!m.completa() && m.destino != null && horizontal(player.blockPosition(), m.destino) <= 6 && contar(player, m.objetivo) >= m.meta) {
						quitar(player, m.objetivo, m.meta);
						fijar(player, datos, m, m.meta);
					}
				}
				case FABRICAR, ESTADISTICA -> {
					int valor = Math.min(m.meta, Math.max(0, estadistica(player, m) - m.base));
					if (valor > m.progreso) {
						fijar(player, datos, m, valor);
					}
				}
				default -> {
				}
			}
		}
		if (PENDIENTES.remove(player.getUUID())) {
			sincronizar(player, false);
		}
	}

	private static double horizontal(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static void fijar(ServerPlayer player, MisionesJugador datos, Mision m, int valor) {
		m.progreso = Math.max(0, Math.min(m.meta, valor));
		if (m.completa() && !m.avisada) {
			m.avisada = true;
			player.sendSystemMessage(Component.literal("✔ Misión completada: ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
				.append(Component.literal(m.nombre).withStyle(ChatFormatting.WHITE))
				.append(Component.literal("  — presiona J para reclamarla").withStyle(ChatFormatting.GRAY)));
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		} else if (!m.completa() && (m.tipo == Mision.Tipo.MINAR || m.tipo == Mision.Tipo.CAZAR || m.tipo == Mision.Tipo.FABRICAR)) {
			player.sendOverlayMessage(Component.literal(m.nombre + ": " + m.progreso + "/" + m.meta).withStyle(ChatFormatting.YELLOW));
		}
		guardar(player, datos);
	}

	// ---------------------------------------------------------------- acciones del jugador
	private static Mision porIndice(MisionesJugador datos, int indice) {
		if (indice == SEMANAL) {
			return datos.semanal;
		}
		return indice >= 0 && indice < datos.misiones.size() ? datos.misiones.get(indice) : null;
	}

	/** @return true si se cobro */
	public static boolean reclamar(ServerPlayer player, int indice) {
		MisionesJugador datos = de(player);
		Mision m = porIndice(datos, indice);
		if (m == null || !m.completa() || m.reclamada) {
			return false;
		}
		if (m.tipo == Mision.Tipo.CONSEGUIR) {
			if (contar(player, m.objetivo) < m.meta) {
				player.sendOverlayMessage(Component.literal("Necesitas tener " + m.meta + " en tu inventario para entregarlos").withStyle(ChatFormatting.RED));
				return false;
			}
			quitar(player, m.objetivo, m.meta);
		}
		m.reclamada = true;
		datos.total++;
		int multiplicador = 100 + Math.min(50, datos.racha * 10);
		pagar(player, m.recompensa * multiplicador / 100);
		darBotin(player, m.botin);
		player.giveExperiencePoints(m.dificultad == Mision.SEMANAL ? 120 : 10 + m.dificultad * 15);
		player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
		if (m.dificultad == Mision.SEMANAL) {
			player.sendSystemMessage(Component.literal("★★ ¡Desafío semanal superado! ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
				.append(Component.literal(m.nombre).withStyle(ChatFormatting.WHITE)));
			player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
		if (datos.todasReclamadas() && !datos.bonoCobrado) {
			datos.bonoCobrado = true;
			datos.racha++;
			datos.ultimoDiaCompleto = datos.dia;
			pagar(player, BONO_DIARIO);
			player.sendSystemMessage(Component.literal("★ ¡Completaste todas las misiones de hoy! Bono +" + BONO_DIARIO
				+ " · Racha: " + datos.racha + " días").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
		guardar(player, datos);
		sincronizar(player, false);
		return true;
	}

	public static void reclamarTodo(ServerPlayer player) {
		for (int i = 0; i <= SEMANAL; i++) {
			reclamar(player, i);
		}
	}

	public static void cambiar(ServerPlayer player, int indice) {
		MisionesJugador datos = de(player);
		if (datos.cambioUsado || indice < 0 || indice >= datos.misiones.size() || datos.misiones.get(indice).completa()) {
			return;
		}
		Mision vieja = datos.misiones.get(indice);
		Mision nueva = vieja;
		for (int intento = 0; intento < 8 && nueva.nombre.equals(vieja.nombre); intento++) {
			nueva = GeneradorDeMisiones.reemplazo(player.getUUID(), datos.dia, vieja.dificultad, player.blockPosition(), intento);
		}
		iniciar(player, nueva);
		datos.misiones.set(indice, nueva);
		datos.cambioUsado = true;
		guardar(player, datos);
		sincronizar(player, false);
	}

	/** Paga en Fichas de Casino si el mod de Arequipa esta instalado; si no, en esmeraldas (1 por cada 5). */
	public static void pagar(ServerPlayer player, int fichas) {
		Item ficha = BuiltInRegistries.ITEM.getOptional(Identifier.parse("arequipa:ficha")).orElse(Items.AIR);
		ItemStack premio = ficha != Items.AIR ? new ItemStack(ficha, 1) : new ItemStack(Items.EMERALD, 1);
		int cantidad = ficha != Items.AIR ? fichas : Math.max(1, fichas / 5);
		while (cantidad > 0) {
			int n = Math.min(64, cantidad);
			player.getInventory().placeItemBackInInventory(premio.copyWithCount(n), Prediction.SERVER_ONLY);
			cantidad -= n;
		}
		player.sendOverlayMessage(Component.literal("+" + (ficha != Items.AIR ? fichas + " fichas" : Math.max(1, fichas / 5) + " esmeraldas"))
			.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	/** "id*cantidad" -> item. */
	public static ItemStack botin(String texto) {
		if (texto == null || texto.isEmpty()) {
			return ItemStack.EMPTY;
		}
		String[] partes = texto.split("\\*");
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(partes[0])).orElse(Items.AIR);
		int cantidad = partes.length > 1 ? Integer.parseInt(partes[1]) : 1;
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, cantidad);
	}

	private static void darBotin(ServerPlayer player, String texto) {
		ItemStack stack = botin(texto);
		if (!stack.isEmpty()) {
			player.sendSystemMessage(Component.literal("  + Premio: " + stack.getCount() + " x ").withStyle(ChatFormatting.AQUA)
				.append(stack.getHoverName()));
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
	}

	public static void sincronizar(ServerPlayer player, boolean abrir) {
		MisionesJugador datos = de(player);
		List<MisionesPayloads.Vista> vistas = new ArrayList<>();
		for (Mision m : datos.todas()) {
			String extra = "";
			if (m.tipo == Mision.Tipo.ENTREGAR && m.destino != null && !m.completa()) {
				extra = "Destino: X " + m.destino.getX() + "  Z " + m.destino.getZ() + "  (a "
					+ Math.round(horizontal(player.blockPosition(), m.destino)) + " bloques)";
			}
			vistas.add(new MisionesPayloads.Vista(m.nombre, m.icono, m.progreso, m.meta, m.recompensa, m.dificultad,
				m.reclamada, extra, m.tipo.ordinal(), m.botin));
		}
		LocalDateTime ahora = LocalDateTime.now();
		int segundosDia = (int) Duration.between(ahora, ahora.toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
		int diasSemana = (int) Duration.between(ahora.toLocalDate().atStartOfDay(),
			ahora.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay()).toDays();
		ServerPlayNetworking.send(player, new MisionesPayloads.Sync(abrir, datos.dia, datos.racha, datos.cambioUsado,
			datos.bonoCobrado, BONO_DIARIO, vistas, segundosDia, diasSemana, datos.total));
	}

	// ---------------------------------------------------------------- utilidades
	static boolean coincideBloque(String objetivo, BlockState state) {
		if (objetivo.startsWith("#")) {
			return state.is(TagKey.create(Registries.BLOCK, Identifier.parse(objetivo.substring(1))));
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(objetivo);
	}

	private static final Identifier[] DISTANCIAS = {Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM, Stats.SWIM_ONE_CM,
		Stats.WALK_ON_WATER_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM, Stats.CLIMB_ONE_CM, Stats.AVIATE_ONE_CM, Stats.BOAT_ONE_CM,
		Stats.HORSE_ONE_CM, Stats.MINECART_ONE_CM, Stats.PIG_ONE_CM, Stats.STRIDER_ONE_CM, Stats.HAPPY_GHAST_ONE_CM, Stats.NAUTILUS_ONE_CM};

	/** Valor actual de la estadistica de la mision (FABRICAR: veces fabricado; RECORRER: bloques). */
	static int estadistica(ServerPlayer player, Mision m) {
		ServerStatsCounter stats = player.getStats();
		if (m.tipo == Mision.Tipo.FABRICAR) {
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(m.objetivo)).orElse(Items.AIR);
			return item == Items.AIR ? 0 : stats.getValue(Stats.ITEM_CRAFTED.get(item));
		}
		if (GeneradorDeMisiones.RECORRER.equals(m.objetivo)) {
			long cm = 0;
			for (Identifier id : DISTANCIAS) {
				cm += stats.getValue(Stats.CUSTOM.get(id));
			}
			return (int) (cm / 100);
		}
		// hay que usar el Identifier registrado (el registro de estadisticas compara por identidad)
		Identifier registrado = BuiltInRegistries.CUSTOM_STAT.getValue(Identifier.parse(m.objetivo));
		return registrado == null ? 0 : stats.getValue(Stats.CUSTOM.get(registrado));
	}

	private static int contar(ServerPlayer player, String itemId) {
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(Items.AIR);
		if (item == Items.AIR) {
			return 0;
		}
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void quitar(ServerPlayer player, String itemId, int cantidad) {
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(Items.AIR);
		for (int i = 0; i < player.getInventory().getContainerSize() && cantidad > 0; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				int n = Math.min(cantidad, stack.getCount());
				stack.shrink(n);
				cantidad -= n;
			}
		}
	}
}
