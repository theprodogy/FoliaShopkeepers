package com.nisovin.shopkeepers.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.BaseCommand;
import com.nisovin.shopkeepers.commands.shopkeepers.ShopkeepersCommand;
import com.nisovin.shopkeepers.commands.shopkeepersadmin.ShopkeepersAdminCommand;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.java.Validate;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class Commands {

	private static final String SHOPKEEPERS_COMMAND_DESCRIPTION = "Base for all shopkeeper commands.";
	private static final String SHOPKEEPERS_ADMIN_COMMAND_DESCRIPTION = "Shopkeepers admin commands.";

	private final SKShopkeepersPlugin plugin;
	private final Confirmations confirmations;

	private @Nullable ShopkeepersCommand shopkeepersCommand;
	private @Nullable ShopkeepersAdminCommand shopkeepersAdminCommand;
	private boolean registered = false;

	public Commands(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.confirmations = new Confirmations(plugin);
	}

	public void onEnable() {
		confirmations.onEnable();
		ShopkeepersCommand shopkeepersCommand = new ShopkeepersCommand(plugin, confirmations);
		this.shopkeepersCommand = shopkeepersCommand;
		ShopkeepersAdminCommand shopkeepersAdminCommand = new ShopkeepersAdminCommand(plugin);
		this.shopkeepersAdminCommand = shopkeepersAdminCommand;

		// Paper only allows lifecycle registrations during the initial plugin enable: Plugin reloads
		// keep the registered commands, which delegate to the current command instances.
		if (!registered) {
			registered = true;
			plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
				var registrar = event.registrar();
				registrar.register(
						shopkeepersCommand.getName(),
						SHOPKEEPERS_COMMAND_DESCRIPTION,
						List.copyOf(shopkeepersCommand.getAliases()),
						delegate(this::getShopkeepersCommand, null)
				);
				registrar.register(
						shopkeepersAdminCommand.getName(),
						SHOPKEEPERS_ADMIN_COMMAND_DESCRIPTION,
						List.copyOf(shopkeepersAdminCommand.getAliases()),
						delegate(
								this::getShopkeepersAdminCommand,
								ShopkeepersAdminCommand.PERMISSION
						)
				);
			});
		}
	}

	// The permission hides the command from senders that lack it. Null shows it to everyone.
	private static BasicCommand delegate(
			Supplier<? extends BaseCommand> command,
			@Nullable String permission
	) {
		return new BasicCommand() {
			@Override
			public void execute(CommandSourceStack source, @NonNull String[] args) {
				SchedulerUtils.runLocked(() -> command.get().execute(source, args));
			}

			@Override
			public Collection<String> suggest(CommandSourceStack source, @NonNull String[] args) {
				List<Collection<String>> result = new ArrayList<>(1);
				SchedulerUtils.runLocked(() -> {
					result.add(command.get().suggest(source, args));
				});
				return result.get(0);
			}

			@Override
			public @Nullable String permission() {
				return permission;
			}
		};
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

	public ShopkeepersAdminCommand getShopkeepersAdminCommand() {
		return Validate.State.notNull(
				shopkeepersAdminCommand,
				"The commands have not yet been set up!"
		);
	}
}
