package com.aventura.misiones.item;

import com.aventura.misiones.Misiones;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Libreta de misiones: abre las misiones del dia desde cualquier lugar. */
public class LibretaDeMisionesItem extends Item {
	public LibretaDeMisionesItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			Misiones.sincronizar(server, true);
		}
		return InteractionResult.SUCCESS;
	}
}
