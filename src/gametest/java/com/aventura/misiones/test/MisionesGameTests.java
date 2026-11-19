package com.aventura.misiones.test;

import com.aventura.misiones.Misiones;
import com.aventura.misiones.data.GeneradorDeMisiones;
import com.aventura.misiones.data.Mision;
import com.aventura.misiones.data.MisionesJugador;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class MisionesGameTests {
	/** Jugador real en supervivencia (el de GameTestHelper es creativo y no cuenta bloques). */
	static ServerPlayer jugador(GameTestHelper helper) {
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
			new com.mojang.authlib.GameProfile(UUID.randomUUID(), "aventurero"), false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return player;
	}

	private static ServerPlayer jugadorCon(GameTestHelper helper, Mision... misiones) {
		ServerPlayer player = jugador(helper);
		MisionesJugador datos = new MisionesJugador(Misiones.hoy(), List.of(misiones), 0, "", false, false, 0, "", Optional.empty());
		player.setAttached(Misiones.DATOS, datos);
		return player;
	}

	private static Mision mision(Mision.Tipo tipo, String objetivo, int meta, BlockPos destino) {
		return new Mision(tipo, objetivo, "Prueba " + objetivo, "minecraft:stone", meta, 10, 0, Optional.ofNullable(destino), "");
	}

	@GameTest
	public void generacionDeterminista(GameTestHelper helper) {
		UUID id = UUID.randomUUID();
		List<Mision> a = GeneradorDeMisiones.delDia(id, "2026-10-06", BlockPos.ZERO);
		List<Mision> b = GeneradorDeMisiones.delDia(id, "2026-10-06", BlockPos.ZERO);
		helper.assertTrue(a.size() == 3, "Deben ser 3 misiones");
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(a.get(i).nombre.equals(b.get(i).nombre), "Las misiones del dia no deben cambiar al reabrir");
			helper.assertTrue(a.get(i).dificultad == i, "Dificultades 0, 1, 2");
		}
		List<Mision> otroDia = GeneradorDeMisiones.delDia(id, "2026-10-07", BlockPos.ZERO);
		helper.assertTrue(!(a.get(0).nombre.equals(otroDia.get(0).nombre) && a.get(1).nombre.equals(otroDia.get(1).nombre)
			&& a.get(2).nombre.equals(otroDia.get(2).nombre)), "Otro dia debe traer misiones distintas");
		helper.succeed();
	}

	@GameTest
	public void minarCompletaYPaga(GameTestHelper helper) {
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.MINAR, "#c:ores/diamond", 2, null));
		Misiones.alRomper(player, Blocks.STONE.defaultBlockState());
		Misiones.alRomper(player, Blocks.DIAMOND_ORE.defaultBlockState());
		Misiones.alRomper(player, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState());
		Mision m = Misiones.de(player).misiones.get(0);
		helper.assertTrue(m.completa(), "2 menas de diamante deben completar la mision (progreso " + m.progreso + ")");
		Misiones.reclamar(player, 0);
		helper.assertTrue(Misiones.de(player).misiones.get(0).reclamada, "Debe quedar reclamada");
		int premio = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			premio += player.getInventory().getItem(i).getCount();
		}
		helper.assertTrue(premio > 0, "Debe recibir la recompensa (y el bono, al ser la unica mision)");
		helper.assertTrue(Misiones.de(player).racha == 1, "Completar todas sube la racha");
		helper.succeed();
	}

	@GameTest
	public void cazarZombis(GameTestHelper helper) {
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.CAZAR, "minecraft:zombie", 2, null));
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		Misiones.alMatar(player, zombie);
		Misiones.alMatar(player, zombie);
		helper.assertTrue(Misiones.de(player).misiones.get(0).completa(), "Debe contar las muertes de zombis");
		helper.succeed();
	}

	@GameTest
	public void entregarEnElLugar(GameTestHelper helper) {
		ServerPlayer player = jugador(helper);
		BlockPos aqui = player.blockPosition();
		player.setAttached(Misiones.DATOS, new MisionesJugador(Misiones.hoy(),
			List.of(mision(Mision.Tipo.ENTREGAR, "minecraft:torch", 8, aqui.offset(3, 0, 2))), 0, "", false, false, 0, "", Optional.empty()));
		player.getInventory().add(new ItemStack(Items.TORCH, 10));
		Misiones.cadaSegundo(player);
		helper.assertTrue(Misiones.de(player).misiones.get(0).completa(), "Estando en el lugar con las antorchas, se entrega");
		helper.assertTrue(player.getInventory().countItem(Items.TORCH) == 2, "Debe consumir 8 antorchas");
		helper.succeed();
	}

	/** Fabricar cuenta por estadistica, desde que empezo la mision. */
	@GameTest
	public void fabricarCuenta(GameTestHelper helper) {
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.FABRICAR, "minecraft:torch", 32, null));
		player.awardStat(Stats.ITEM_CRAFTED.get(Items.TORCH), 20);
		Misiones.cadaSegundo(player);
		helper.assertTrue(Misiones.de(player).misiones.get(0).progreso == 20, "20 antorchas fabricadas");
		player.awardStat(Stats.ITEM_CRAFTED.get(Items.TORCH), 16);
		Misiones.cadaSegundo(player);
		helper.assertTrue(Misiones.de(player).misiones.get(0).completa(), "36 >= 32: completa");
		helper.succeed();
	}

	/** "Junta X": el progreso sigue al inventario y los items se entregan al reclamar. */
	@GameTest
	public void conseguirSeEntregaAlReclamar(GameTestHelper helper) {
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.CONSEGUIR, "minecraft:diamond", 5, null));
		player.getInventory().add(new ItemStack(Items.DIAMOND, 7));
		Misiones.cadaSegundo(player);
		helper.assertTrue(Misiones.de(player).misiones.get(0).completa(), "Con 7 diamantes esta completa");
		helper.assertTrue(Misiones.reclamar(player, 0), "Se puede reclamar");
		helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 2, "Entrega 5 diamantes y se queda con 2");
		helper.succeed();
	}

	/** "#monstruo" cuenta cualquier monstruo pero no animales. */
	@GameTest
	public void monstruoCualquiera(GameTestHelper helper) {
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.CAZAR, "#monstruo", 2, null));
		var level = helper.getLevel();
		Misiones.alMatar(player, net.minecraft.world.entity.EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND));
		helper.assertTrue(Misiones.de(player).misiones.get(0).progreso == 0, "Una vaca no es un monstruo");
		Misiones.alMatar(player, net.minecraft.world.entity.EntityTypes.SKELETON.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND));
		Misiones.alMatar(player, net.minecraft.world.entity.EntityTypes.SPIDER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND));
		helper.assertTrue(Misiones.de(player).misiones.get(0).completa(), "Esqueleto y arana cuentan");
		helper.succeed();
	}

	/** Cada jugador tiene un desafio semanal, determinista, con premio extra al reclamar. */
	@GameTest
	public void desafioSemanal(GameTestHelper helper) {
		UUID id = UUID.randomUUID();
		Mision a = GeneradorDeMisiones.deLaSemana(id, "2026-W41");
		Mision b = GeneradorDeMisiones.deLaSemana(id, "2026-W41");
		helper.assertTrue(a.nombre.equals(b.nombre) && a.dificultad == Mision.SEMANAL, "Semanal determinista");
		helper.assertTrue(a.recompensa >= 150 && !a.botin.isEmpty(), "Premio grande y botin");
		ServerPlayer player = jugadorCon(helper, mision(Mision.Tipo.MINAR, "minecraft:stone", 1, null));
		MisionesJugador datos = Misiones.de(player);
		helper.assertTrue(datos.semanal != null, "Se crea el semanal al entrar");
		datos.semanal = new Mision(Mision.Tipo.CAZAR, "minecraft:zombie", "Semanal de prueba", "minecraft:stone", 1, 150, Mision.SEMANAL,
			Optional.empty(), "minecraft:diamond*6");
		Misiones.alMatar(player, net.minecraft.world.entity.EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND));
		helper.assertTrue(Misiones.reclamar(player, Misiones.SEMANAL), "Se reclama el semanal");
		helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 6, "Botin de 6 diamantes");
		helper.succeed();
	}
}
