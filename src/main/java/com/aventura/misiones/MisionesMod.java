package com.aventura.misiones;

import com.aventura.misiones.block.TablonDeMisionesBlock;
import com.aventura.misiones.item.LibretaDeMisionesItem;
import com.aventura.misiones.net.MisionesPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MisionesMod implements ModInitializer {
	public static final String MOD_ID = "misiones";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Block TABLON;
	public static Item TABLON_ITEM;
	public static Item LIBRETA;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// ------------------------------------------------------------ bloque e item
		ResourceKey<Block> tablonKey = ResourceKey.create(Registries.BLOCK, id("tablon_de_misiones"));
		TABLON = Blocks.register(tablonKey, TablonDeMisionesBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.5F).noOcclusion());
		ResourceKey<Item> tablonItemKey = ResourceKey.create(Registries.ITEM, id("tablon_de_misiones"));
		BlockItem tablonItem = new BlockItem(TABLON, new Item.Properties().setId(tablonItemKey).useBlockDescriptionPrefix());
		tablonItem.registerBlocks(Item.BY_BLOCK, tablonItem);
		TABLON_ITEM = Registry.register(BuiltInRegistries.ITEM, tablonItemKey, tablonItem);
		ResourceKey<Item> libretaKey = ResourceKey.create(Registries.ITEM, id("libreta_de_misiones"));
		LIBRETA = Registry.register(BuiltInRegistries.ITEM, libretaKey,
			new LibretaDeMisionesItem(new Item.Properties().setId(libretaKey).stacksTo(1)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> out.accept(TABLON_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> out.accept(LIBRETA));

		// ------------------------------------------------------------ red
		Misiones.DATOS.toString(); // carga la clase (registra el attachment)
		PayloadTypeRegistry.clientboundPlay().register(MisionesPayloads.Sync.TYPE, MisionesPayloads.Sync.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MisionesPayloads.Accion.TYPE, MisionesPayloads.Accion.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MisionesPayloads.Accion.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			switch (payload.accion()) {
				case MisionesPayloads.Accion.RECLAMAR -> Misiones.reclamar(player, payload.indice());
				case MisionesPayloads.Accion.CAMBIAR -> Misiones.cambiar(player, payload.indice());
				case MisionesPayloads.Accion.ABRIR -> Misiones.sincronizar(player, true);
				case MisionesPayloads.Accion.RECLAMAR_TODO -> Misiones.reclamarTodo(player);
				default -> Misiones.sincronizar(player, false);
			}
		});

		// ------------------------------------------------------------ avance de misiones
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer server) {
				Misiones.alRomper(server, state);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player) {
				Misiones.alMatar(player, entity);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					Misiones.cadaSegundo(player);
				}
			}
		});
		// al entrar: renovar si cambio el dia y mandar el estado (para el panel lateral del HUD)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Misiones.sincronizar(handler.getPlayer(), false));

		// ------------------------------------------------------------ comando /misiones
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("misiones").executes(ctx -> {
				Misiones.sincronizar(ctx.getSource().getPlayerOrException(), true);
				return 1;
			})));

		LOGGER.info("Misiones Diarias cargado");
	}
}
