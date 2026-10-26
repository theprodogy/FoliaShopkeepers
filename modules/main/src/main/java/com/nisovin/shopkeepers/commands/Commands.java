package com.nisovin.shopkeepers.commands;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.shopkeepers.ShopkeepersCommand;
import com.nisovin.shopkeepers.util.java.Validate;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class Commands {

	private static final String SHOPKEEPERS_COMMAND_DESCRIPTION = "Base for all shopkeeper commands.";

	private final SKShopkeepersPlugin plugin;
	private final Confirmations confirmations;

	private @Nullable ShopkeepersCommand shopkeepersCommand;
	private boolean registered = false;

	public Commands(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.confirmations = new Confirmations(plugin);
	}

	public void onEnable() {
		confirmations.onEnable();
		ShopkeepersCommand shopkeepersCommand = new ShopkeepersCommand(plugin, confirmations);
		this.shopkeepersCommand = shopkeepersCommand;

		// Paper only allows lifecycle registrations during the initial plugin enable: Plugin reloads
		// keep the registered command, which delegates to the current command instance.
		if (!registered) {
			registered = true;
			BasicCommand delegate = new BasicCommand() {
				@Override
				public void execute(CommandSourceStack source, @NonNull String[] args) {
					getShopkeepersCommand().execute(source, args);
				}

				@Override
				public Collection<String> suggest(CommandSourceStack source, @NonNull String[] args) {
					return getShopkeepersCommand().suggest(source, args);
				}
			};
			plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
				event.registrar().register(
						shopkeepersCommand.getName(),
						SHOPKEEPERS_COMMAND_DESCRIPTION,
						List.copyOf(shopkeepersCommand.getAliases()),
						delegate
				);
			});
		}
	}

	public void onDisable() {
		confirmations.onDisable();
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		confirmations.onPlayerQuit(player);
	}

	public ShopkeepersCommand getShopkeepersCommand() {
		return Validate.State.notNull(shopkeepersCommand, "The commands have not yet been set up!");
	}
}
