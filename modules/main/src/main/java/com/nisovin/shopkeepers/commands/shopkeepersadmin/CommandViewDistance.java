package com.nisovin.shopkeepers.commands.shopkeepersadmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.arguments.BoundedIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.visibility.ShopkeeperViewDistance;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandViewDistance extends Command {

	private static final String ARGUMENT_DISTANCE = "distance";
	private static final String ARGUMENT_ALWAYS_VISIBLE = "ALWAYS_VISIBLE";
	private static final String ARGUMENT_BLOCKS = "blocks";

	private static final int MAX_DISTANCE = 512;
	private static final List<String> SUGGESTED_DISTANCES = Arrays.asList(
			"16", "32", "48", "64", "96", "128"
	);

	CommandViewDistance(SKShopkeepersPlugin plugin) {
		super("viewdistance");
		this.setPermission(ShopkeepersAdminCommand.PERMISSION);
		this.setDescription(Text.of(
				"Sets how far shopkeepers are visible, in blocks or ALWAYS_VISIBLE."
		));

		this.addArgument(new FirstOfArgument(ARGUMENT_DISTANCE, Arrays.asList(
				new LiteralArgument(ARGUMENT_ALWAYS_VISIBLE),
				new BoundedIntegerArgument(ARGUMENT_BLOCKS, 1, MAX_DISTANCE) {
					@Override
					public List<? extends String> complete(
							CommandInput input,
							CommandContextView context,
							ArgumentsReader argsReader
					) {
						if (argsReader.getRemainingSize() != 1) {
							return Collections.emptyList();
						}

						String partialArg = argsReader.next();
						List<String> suggestions = new ArrayList<>();
						for (String distance : SUGGESTED_DISTANCES) {
							if (distance.startsWith(partialArg)) {
								suggestions.add(distance);
							}
						}
						return suggestions;
					}
				}
		), true).optional());
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		if (!context.has(ARGUMENT_DISTANCE)) {
			TextUtils.sendMessage(sender, TextUtils.colorize("&eShopkeeper view distance: &a"
					+ format(Settings.shopkeeperViewDistance)));
			return;
		}

		int distance = context.has(ARGUMENT_ALWAYS_VISIBLE)
				? ShopkeeperViewDistance.ALWAYS_VISIBLE
				: context.<Integer>get(ARGUMENT_BLOCKS);
		Settings.shopkeeperViewDistance = distance;
		Settings.saveConfig();
		TextUtils.sendMessage(sender, TextUtils.colorize(
				"&aShopkeeper view distance set to &e" + format(distance)
		));
	}

	private static String format(int distance) {
		if (distance == ShopkeeperViewDistance.ALWAYS_VISIBLE) return ARGUMENT_ALWAYS_VISIBLE;
		return distance + " blocks";
	}
}
