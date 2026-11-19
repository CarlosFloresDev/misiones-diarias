package com.aventura.misiones.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

/**
 * Crea las 3 misiones del dia (facil, media, dificil) y el desafio semanal. Es determinista por jugador y dia/semana,
 * asi que no cambian aunque el jugador salga y entre. Si estan instalados Arequipa, Tecsup o Mazmorras del Cielo,
 * agrega misiones de esos mods.
 */
public final class GeneradorDeMisiones {
	/** Estadistica especial: suma de todas las formas de moverse, en bloques. */
	public static final String RECORRER = "recorrer";

	private GeneradorDeMisiones() {
	}

	private record Plantilla(Mision.Tipo tipo, String objetivo, String texto, String icono, int min, int max, String mod) {
		Plantilla(Mision.Tipo tipo, String objetivo, String texto, String icono, int min, int max) {
			this(tipo, objetivo, texto, icono, min, max, null);
		}

		boolean disponible() {
			return this.mod == null || FabricLoader.getInstance().isModLoaded(this.mod);
		}
	}

	private static Plantilla minar(String bloque, String texto, String icono, int min, int max) {
		return new Plantilla(Mision.Tipo.MINAR, bloque, texto, icono, min, max);
	}

	private static Plantilla cazar(String entidad, String texto, String icono, int min, int max) {
		return new Plantilla(Mision.Tipo.CAZAR, entidad, texto, icono, min, max);
	}

	private static Plantilla fabricar(String item, String texto, int min, int max) {
		return new Plantilla(Mision.Tipo.FABRICAR, item, texto, item, min, max);
	}

	private static Plantilla stat(String stat, String texto, String icono, int min, int max) {
		return new Plantilla(Mision.Tipo.ESTADISTICA, stat, texto, icono, min, max);
	}

	private static Plantilla deMod(String mod, Mision.Tipo tipo, String objetivo, String texto, String icono, int min, int max) {
		return new Plantilla(tipo, objetivo, texto, icono, min, max, mod);
	}

	private static final List<Plantilla> FACILES = List.of(
		minar("#minecraft:logs", "Tala %d troncos", "minecraft:oak_log", 32, 64),
		minar("#c:ores/coal", "Mina %d menas de carbón", "minecraft:coal", 12, 24),
		minar("#c:ores/copper", "Mina %d menas de cobre", "minecraft:raw_copper", 10, 20),
		minar("#minecraft:base_stone_overworld", "Pica %d bloques de piedra", "minecraft:cobblestone", 96, 192),
		cazar("minecraft:zombie", "Elimina %d zombis", "minecraft:rotten_flesh", 8, 15),
		cazar("minecraft:skeleton", "Elimina %d esqueletos", "minecraft:bone", 6, 12),
		cazar("minecraft:spider", "Elimina %d arañas", "minecraft:string", 6, 12),
		cazar("minecraft:creeper", "Elimina %d creepers", "minecraft:gunpowder", 4, 8),
		fabricar("minecraft:torch", "Fabrica %d antorchas", 32, 64),
		fabricar("minecraft:bread", "Hornea %d panes", 8, 16),
		stat("minecraft:fish_caught", "Pesca %d peces", "minecraft:fishing_rod", 3, 6),
		stat("minecraft:animals_bred", "Cría %d animales", "minecraft:wheat", 4, 8),
		stat(RECORRER, "Recorre %d bloques", "minecraft:leather_boots", 600, 1200),
		new Plantilla(Mision.Tipo.CONSEGUIR, "minecraft:cooked_beef", "Junta %d filetes cocidos", "minecraft:cooked_beef", 10, 20),
		deMod("arequipa", Mision.Tipo.FABRICAR, "arequipa:chicha_morada", "Prepara %d chichas moradas", "arequipa:chicha_morada", 3, 6),
		deMod("tecsup", Mision.Tipo.FABRICAR, "tecsup:arduino", "Arma %d placas Arduino", "tecsup:arduino", 1, 2)
	);

	private static final List<Plantilla> MEDIAS = List.of(
		minar("#c:ores/iron", "Mina %d menas de hierro", "minecraft:raw_iron", 16, 32),
		minar("#c:ores/gold", "Mina %d menas de oro", "minecraft:raw_gold", 8, 16),
		minar("#c:ores/redstone", "Mina %d menas de redstone", "minecraft:redstone", 10, 20),
		minar("#c:ores/lapis", "Mina %d menas de lapislázuli", "minecraft:lapis_lazuli", 6, 12),
		cazar("#monstruo", "Elimina %d monstruos (cualquiera)", "minecraft:iron_sword", 20, 35),
		cazar("minecraft:enderman", "Elimina %d endermans", "minecraft:ender_pearl", 4, 8),
		cazar("minecraft:witch", "Elimina %d brujas", "minecraft:glass_bottle", 2, 4),
		cazar("minecraft:drowned", "Elimina %d ahogados", "minecraft:trident", 6, 10),
		cazar("minecraft:pillager", "Elimina %d saqueadores", "minecraft:crossbow", 4, 8),
		fabricar("minecraft:cooked_beef", "Cocina %d filetes en el horno", 10, 20),
		fabricar("minecraft:iron_ingot", "Funde %d lingotes de hierro", 16, 32),
		stat("minecraft:traded_with_villager", "Comercia %d veces con aldeanos", "minecraft:emerald", 4, 8),
		stat("minecraft:enchant_item", "Encanta %d objetos", "minecraft:enchanting_table", 2, 3),
		stat("minecraft:fish_caught", "Pesca %d peces", "minecraft:fishing_rod", 8, 12),
		new Plantilla(Mision.Tipo.CONSEGUIR, "minecraft:gold_ingot", "Junta %d lingotes de oro", "minecraft:gold_ingot", 12, 24),
		deMod("arequipa", Mision.Tipo.FABRICAR, "arequipa:pisco_sour", "Prepara %d Pisco Sour", "arequipa:pisco_sour", 3, 5),
		deMod("arequipa", Mision.Tipo.FABRICAR, "arequipa:rocoto_relleno", "Cocina %d rocotos rellenos", "arequipa:rocoto_relleno", 3, 5),
		deMod("tecsup", Mision.Tipo.FABRICAR, "tecsup:plc", "Programa %d PLC", "tecsup:plc", 1, 2),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:pirata_del_cielo", "Derrota %d piratas del cielo", "minecraft:feather", 4, 8)
	);

	private static final List<Plantilla> DIFICILES = List.of(
		minar("#c:ores/diamond", "Mina %d menas de diamante", "minecraft:diamond", 5, 12),
		minar("#c:ores/emerald", "Mina %d menas de esmeralda", "minecraft:emerald", 2, 4),
		minar("minecraft:ancient_debris", "Mina %d escombros ancestrales", "minecraft:netherite_scrap", 2, 4),
		cazar("minecraft:blaze", "Elimina %d blazes", "minecraft:blaze_rod", 8, 12),
		cazar("minecraft:wither_skeleton", "Elimina %d esqueletos wither", "minecraft:wither_skeleton_skull", 4, 6),
		cazar("minecraft:ghast", "Elimina %d ghasts", "minecraft:ghast_tear", 2, 3),
		cazar("minecraft:guardian", "Elimina %d guardianes", "minecraft:prismarine_shard", 5, 8),
		cazar("#monstruo", "Elimina %d monstruos (cualquiera)", "minecraft:diamond_sword", 45, 60),
		fabricar("minecraft:golden_apple", "Fabrica %d manzanas doradas", 2, 3),
		stat(RECORRER, "Recorre %d bloques", "minecraft:diamond_boots", 2500, 4000),
		new Plantilla(Mision.Tipo.CONSEGUIR, "minecraft:diamond", "Junta %d diamantes", "minecraft:diamond", 6, 10),
		deMod("tecsup", Mision.Tipo.FABRICAR, "tecsup:lingote_de_acero_industrial", "Funde %d lingotes de acero industrial",
			"tecsup:lingote_de_acero_industrial", 3, 6),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:mecanico_del_dirigible", "Derrota %d mecánicos de dirigible", "minecraft:iron_ingot", 4, 8),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:guardian_celeste", "Derrota %d guardianes celestes", "minecraft:shield", 3, 6),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:brujo_del_viento", "Derrota %d brujos del viento", "minecraft:wind_charge", 3, 6)
	);

	/** Desafios semanales: grandes, con premio grande. */
	private static final List<Plantilla> SEMANALES = List.of(
		cazar("#monstruo", "Elimina %d monstruos esta semana", "minecraft:netherite_sword", 120, 180),
		minar("#c:ores/diamond", "Mina %d menas de diamante esta semana", "minecraft:diamond_block", 20, 30),
		stat(RECORRER, "Recorre %d bloques esta semana", "minecraft:elytra", 8000, 12000),
		stat("minecraft:fish_caught", "Pesca %d peces esta semana", "minecraft:fishing_rod", 30, 40),
		stat("minecraft:animals_bred", "Cría %d animales esta semana", "minecraft:hay_block", 30, 40),
		stat("minecraft:traded_with_villager", "Comercia %d veces con aldeanos", "minecraft:emerald_block", 25, 35),
		stat("minecraft:raid_win", "Gana %d asalto (raid) de saqueadores", "minecraft:totem_of_undying", 1, 1),
		cazar("minecraft:elder_guardian", "Derrota a %d guardián anciano", "minecraft:sponge", 1, 1),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:capitan_barbanube", "Derrota %d vez al Capitán Barbanube", "minecraft:spyglass", 1, 1),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:condor_de_acero", "Derrota %d vez al Cóndor de Acero", "minecraft:feather", 1, 1),
		deMod("cielo", Mision.Tipo.CAZAR, "cielo:guardian_celeste", "Derrota %d guardianes celestes esta semana", "minecraft:shield", 15, 25),
		deMod("tecsup", Mision.Tipo.FABRICAR, "tecsup:pico_industrial", "Fabrica %d Pico Industrial", "tecsup:pico_industrial", 1, 1),
		deMod("arequipa", Mision.Tipo.FABRICAR, "arequipa:pisco_sour", "Prepara %d Pisco Sour para la fiesta", "arequipa:pisco_sour", 16, 24)
	);

	/** Premios extra en items: dificil (indice 0) y semanal (indice 1). */
	private static final String[][] BOTIN = {
		{"minecraft:diamond*2", "minecraft:emerald*6", "minecraft:golden_apple*2", "minecraft:experience_bottle*12", "minecraft:gold_ingot*8",
			"minecraft:name_tag*1", "minecraft:saddle*1"},
		{"minecraft:diamond*6", "minecraft:netherite_scrap*2", "minecraft:enchanted_golden_apple*1", "minecraft:totem_of_undying*1",
			"minecraft:experience_bottle*32", "minecraft:emerald_block*3", "minecraft:shulker_shell*2"},
	};

	private record Bioma(String id, String nombre, String icono, int dificultad) {
	}

	private static final List<Bioma> BIOMAS = List.of(
		new Bioma("minecraft:desert", "un desierto", "minecraft:sand", 1),
		new Bioma("minecraft:jungle", "una jungla", "minecraft:jungle_sapling", 1),
		new Bioma("minecraft:badlands", "unas badlands (mesa)", "minecraft:terracotta", 1),
		new Bioma("minecraft:swamp", "un pantano", "minecraft:lily_pad", 1),
		new Bioma("minecraft:savanna", "una sabana", "minecraft:acacia_sapling", 1),
		new Bioma("minecraft:cherry_grove", "un bosque de cerezos", "minecraft:cherry_sapling", 1),
		new Bioma("minecraft:snowy_plains", "unas llanuras nevadas", "minecraft:snowball", 1),
		new Bioma("minecraft:ice_spikes", "los picos de hielo", "minecraft:packed_ice", 2),
		new Bioma("minecraft:mushroom_fields", "una isla champiñón", "minecraft:red_mushroom", 2),
		new Bioma("minecraft:deep_dark", "la oscuridad profunda", "minecraft:sculk", 2)
	);

	private record Encargo(String item, String nombre, int cantidad) {
	}

	private static final List<Encargo> ENCARGOS = List.of(
		new Encargo("minecraft:torch", "antorchas", 32),
		new Encargo("minecraft:cooked_beef", "filetes cocidos", 12),
		new Encargo("minecraft:iron_ingot", "lingotes de hierro", 8),
		new Encargo("minecraft:bread", "panes", 16),
		new Encargo("minecraft:oak_planks", "tablones de roble", 64),
		new Encargo("minecraft:glass", "bloques de vidrio", 24)
	);

	public static long semilla(UUID jugador, String dia, int salt) {
		return jugador.getMostSignificantBits() ^ (jugador.getLeastSignificantBits() * 31) ^ ((long) dia.hashCode() << 16) ^ salt * 0x9E3779B97F4A7C15L;
	}

	/** Las misiones del dia: una facil, una media y una dificil. */
	public static List<Mision> delDia(UUID jugador, String dia, BlockPos posicion) {
		Random random = new Random(semilla(jugador, dia, 0));
		List<Mision> out = new ArrayList<>();
		for (int dificultad = 0; dificultad < 3; dificultad++) {
			out.add(nueva(random, dificultad, posicion));
		}
		return out;
	}

	/** Mision de reemplazo (boton "Cambiar"). */
	public static Mision reemplazo(UUID jugador, String dia, int dificultad, BlockPos posicion, int intento) {
		return nueva(new Random(semilla(jugador, dia, 1000 + intento)), dificultad, posicion);
	}

	/** El desafio de la semana: 150-220 fichas y un premio grande. */
	public static Mision deLaSemana(UUID jugador, String semana) {
		Random random = new Random(semilla(jugador, semana, 77));
		List<Plantilla> pool = SEMANALES.stream().filter(Plantilla::disponible).toList();
		Plantilla p = pool.get(random.nextInt(pool.size()));
		int meta = p.min() + random.nextInt(p.max() - p.min() + 1);
		return new Mision(p.tipo(), p.objetivo(), String.format(p.texto(), meta), p.icono(), meta, 150 + random.nextInt(71),
			Mision.SEMANAL, Optional.empty(), BOTIN[1][random.nextInt(BOTIN[1].length)]);
	}

	private static Mision nueva(Random random, int dificultad, BlockPos posicion) {
		int[] base = {12, 25, 50};
		int[] extra = {6, 12, 25};
		int recompensa = base[dificultad] + random.nextInt(extra[dificultad] + 1);
		String botin = dificultad == 2 ? BOTIN[0][random.nextInt(BOTIN[0].length)] : "";
		// La mision media a veces es de exploracion o de entrega
		if (dificultad == 1 && random.nextInt(4) == 0) {
			Encargo e = ENCARGOS.get(random.nextInt(ENCARGOS.size()));
			double angulo = random.nextDouble() * Math.PI * 2;
			int distancia = 150 + random.nextInt(250);
			BlockPos destino = new BlockPos(posicion.getX() + (int) (Math.cos(angulo) * distancia), 0,
				posicion.getZ() + (int) (Math.sin(angulo) * distancia));
			return new Mision(Mision.Tipo.ENTREGAR, e.item(), "Lleva " + e.cantidad() + " " + e.nombre() + " al punto marcado",
				e.item(), e.cantidad(), recompensa + 10, dificultad, Optional.of(destino), botin);
		}
		if (dificultad >= 1 && random.nextInt(5) == 0) {
			List<Bioma> opciones = BIOMAS.stream().filter(b -> b.dificultad() <= dificultad).toList();
			Bioma b = opciones.get(random.nextInt(opciones.size()));
			return new Mision(Mision.Tipo.EXPLORAR, b.id(), "Explora " + b.nombre(), b.icono(), 1,
				recompensa + (b.dificultad() == 2 ? 15 : 0), dificultad, Optional.empty(), botin);
		}
		List<Plantilla> pool = (dificultad == 0 ? FACILES : dificultad == 1 ? MEDIAS : DIFICILES).stream()
			.filter(Plantilla::disponible).toList();
		Plantilla p = pool.get(random.nextInt(pool.size()));
		int meta = p.min() + random.nextInt(p.max() - p.min() + 1);
		return new Mision(p.tipo(), p.objetivo(), String.format(p.texto(), meta), p.icono(), meta, recompensa, dificultad,
			Optional.empty(), botin);
	}
}
