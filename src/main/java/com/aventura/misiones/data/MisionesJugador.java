package com.aventura.misiones.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Estado de misiones de un jugador (se guarda en el jugador y sobrevive a la muerte). */
public class MisionesJugador {
	public static final Codec<MisionesJugador> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("dia").forGetter(m -> m.dia),
		Mision.CODEC.listOf().fieldOf("misiones").forGetter(m -> m.misiones),
		Codec.INT.fieldOf("racha").forGetter(m -> m.racha),
		Codec.STRING.fieldOf("ultimo_dia_completo").forGetter(m -> m.ultimoDiaCompleto),
		Codec.BOOL.fieldOf("cambio_usado").forGetter(m -> m.cambioUsado),
		Codec.BOOL.fieldOf("bono_cobrado").forGetter(m -> m.bonoCobrado),
		Codec.INT.fieldOf("total").forGetter(m -> m.total),
		Codec.STRING.optionalFieldOf("semana", "").forGetter(m -> m.semana),
		Mision.CODEC.optionalFieldOf("semanal").forGetter(m -> Optional.ofNullable(m.semanal))
	).apply(i, MisionesJugador::new));

	public String dia;
	public List<Mision> misiones;
	public int racha;
	public String ultimoDiaCompleto;
	public boolean cambioUsado;
	public boolean bonoCobrado;
	public int total;
	/** Semana ISO de la mision semanal, por ejemplo "2026-W41". */
	public String semana;
	public @Nullable Mision semanal;

	public MisionesJugador(String dia, List<Mision> misiones, int racha, String ultimoDiaCompleto, boolean cambioUsado,
		boolean bonoCobrado, int total, String semana, Optional<Mision> semanal) {
		this.dia = dia;
		this.misiones = new ArrayList<>(misiones);
		this.racha = racha;
		this.ultimoDiaCompleto = ultimoDiaCompleto;
		this.cambioUsado = cambioUsado;
		this.bonoCobrado = bonoCobrado;
		this.total = total;
		this.semana = semana;
		this.semanal = semanal.orElse(null);
	}

	public static MisionesJugador vacio() {
		return new MisionesJugador("", List.of(), 0, "", false, false, 0, "", Optional.empty());
	}

	public boolean todasReclamadas() {
		return !this.misiones.isEmpty() && this.misiones.stream().allMatch(m -> m.reclamada);
	}

	/** Las 3 diarias y, al final, la semanal (indice 3). */
	public List<Mision> todas() {
		List<Mision> out = new ArrayList<>(this.misiones);
		if (this.semanal != null) {
			out.add(this.semanal);
		}
		return out;
	}
}
