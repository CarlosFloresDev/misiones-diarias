package com.aventura.misiones.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

/** Una mision (diaria o semanal). Es mutable: el progreso avanza mientras el jugador juega. */
public class Mision {
	public enum Tipo implements StringRepresentable {
		/** Romper bloques (id o #tag). */
		MINAR,
		/** Matar entidades (id, o "#monstruo" para cualquier monstruo). */
		CAZAR,
		/** Tener los items; se entregan al reclamar. */
		CONSEGUIR,
		/** Llevar items a unas coordenadas. */
		ENTREGAR,
		/** Entrar a un bioma. */
		EXPLORAR,
		/** Fabricar o cocinar un item (cuenta la estadistica "fabricado"). */
		FABRICAR,
		/** Estadistica de Minecraft: peces pescados, animales criados, comercio, distancia recorrida... */
		ESTADISTICA;

		public static final Codec<Tipo> CODEC = StringRepresentable.fromEnum(Tipo::values);

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
	}

	public static final Codec<Mision> CODEC = RecordCodecBuilder.create(i -> i.group(
		Tipo.CODEC.fieldOf("tipo").forGetter(m -> m.tipo),
		Codec.STRING.fieldOf("objetivo").forGetter(m -> m.objetivo),
		Codec.STRING.fieldOf("nombre").forGetter(m -> m.nombre),
		Codec.STRING.fieldOf("icono").forGetter(m -> m.icono),
		Codec.INT.fieldOf("meta").forGetter(m -> m.meta),
		Codec.INT.fieldOf("progreso").forGetter(m -> m.progreso),
		Codec.INT.fieldOf("recompensa").forGetter(m -> m.recompensa),
		Codec.INT.fieldOf("dificultad").forGetter(m -> m.dificultad),
		Codec.BOOL.fieldOf("reclamada").forGetter(m -> m.reclamada),
		BlockPos.CODEC.optionalFieldOf("destino").forGetter(m -> Optional.ofNullable(m.destino)),
		Codec.INT.optionalFieldOf("base", 0).forGetter(m -> m.base),
		Codec.STRING.optionalFieldOf("botin", "").forGetter(m -> m.botin),
		Codec.BOOL.optionalFieldOf("avisada", false).forGetter(m -> m.avisada)
	).apply(i, Mision::new));

	/** Dificultad de la mision semanal (se muestra aparte). */
	public static final int SEMANAL = 3;

	public final Tipo tipo;
	/** Id de bloque/entidad/item/bioma/estadistica, o "#tag". */
	public final String objetivo;
	/** Texto para mostrar, por ejemplo "Mina 8 menas de diamante". */
	public final String nombre;
	/** Id del item que se muestra como icono. */
	public final String icono;
	public final int meta;
	public int progreso;
	/** Fichas que paga. */
	public final int recompensa;
	/** 0 facil, 1 media, 2 dificil, 3 semanal. */
	public final int dificultad;
	public boolean reclamada;
	public final BlockPos destino;
	/** Valor de la estadistica cuando empezo la mision (FABRICAR / ESTADISTICA). */
	public int base;
	/** Premio extra en items, "id*cantidad" ("" = ninguno). */
	public final String botin;
	/** Ya se aviso al jugador que estaba completa. */
	public boolean avisada;

	public Mision(Tipo tipo, String objetivo, String nombre, String icono, int meta, int progreso, int recompensa, int dificultad,
		boolean reclamada, Optional<BlockPos> destino, int base, String botin, boolean avisada) {
		this.tipo = tipo;
		this.objetivo = objetivo;
		this.nombre = nombre;
		this.icono = icono;
		this.meta = meta;
		this.progreso = progreso;
		this.recompensa = recompensa;
		this.dificultad = dificultad;
		this.reclamada = reclamada;
		this.destino = destino.orElse(null);
		this.base = base;
		this.botin = botin;
		this.avisada = avisada;
	}

	public Mision(Tipo tipo, String objetivo, String nombre, String icono, int meta, int recompensa, int dificultad,
		Optional<BlockPos> destino, String botin) {
		this(tipo, objetivo, nombre, icono, meta, 0, recompensa, dificultad, false, destino, 0, botin, false);
	}

	public boolean completa() {
		return this.progreso >= this.meta;
	}

	/** Las que se cuentan con estadisticas guardan el valor inicial. */
	public boolean usaEstadistica() {
		return this.tipo == Tipo.FABRICAR || this.tipo == Tipo.ESTADISTICA;
	}
}
