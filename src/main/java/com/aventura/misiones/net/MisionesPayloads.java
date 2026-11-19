package com.aventura.misiones.net;

import com.aventura.misiones.MisionesMod;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Paquetes entre servidor y cliente para la pantalla de misiones. */
public final class MisionesPayloads {
	private MisionesPayloads() {
	}

	/** Lo que el cliente necesita para dibujar una mision. */
	public record Vista(String nombre, String icono, int progreso, int meta, int recompensa, int dificultad, boolean reclamada, String extra,
		int tipo, String botin) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Vista> CODEC = StreamCodec.of(
			(buf, v) -> {
				buf.writeUtf(v.nombre);
				buf.writeUtf(v.icono);
				buf.writeVarInt(v.progreso);
				buf.writeVarInt(v.meta);
				buf.writeVarInt(v.recompensa);
				buf.writeVarInt(v.dificultad);
				buf.writeBoolean(v.reclamada);
				buf.writeUtf(v.extra);
				buf.writeVarInt(v.tipo);
				buf.writeUtf(v.botin);
			},
			buf -> new Vista(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readBoolean(), buf.readUtf(), buf.readVarInt(), buf.readUtf()));

		public boolean completa() {
			return this.progreso >= this.meta;
		}
	}

	/** Servidor -> cliente: estado de las misiones (y si hay que abrir la pantalla). */
	public record Sync(boolean abrir, String dia, int racha, boolean cambioUsado, boolean bonoCobrado, int bono, List<Vista> misiones,
		int segundosDia, int diasSemana, int total) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(MisionesMod.id("sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
			(buf, s) -> {
				buf.writeBoolean(s.abrir);
				buf.writeUtf(s.dia);
				buf.writeVarInt(s.racha);
				buf.writeBoolean(s.cambioUsado);
				buf.writeBoolean(s.bonoCobrado);
				buf.writeVarInt(s.bono);
				Vista.CODEC.apply(ByteBufCodecs.list()).encode(buf, s.misiones);
				buf.writeVarInt(s.segundosDia);
				buf.writeVarInt(s.diasSemana);
				buf.writeVarInt(s.total);
			},
			buf -> new Sync(buf.readBoolean(), buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(),
				Vista.CODEC.apply(ByteBufCodecs.list()).decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: 0 = reclamar, 1 = cambiar, 2 = refrescar, 3 = abrir el panel, 4 = reclamar todo. */
	public record Accion(int accion, int indice) implements CustomPacketPayload {
		public static final int RECLAMAR = 0;
		public static final int CAMBIAR = 1;
		public static final int REFRESCAR = 2;
		public static final int ABRIR = 3;
		public static final int RECLAMAR_TODO = 4;

		public static final Type<Accion> TYPE = new Type<>(MisionesMod.id("accion"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Accion> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Accion::accion, ByteBufCodecs.VAR_INT, Accion::indice, Accion::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
