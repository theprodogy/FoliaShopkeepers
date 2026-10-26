package com.nisovin.shopkeepers.commands.shopkeepersadmin;

import java.util.Arrays;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.BaseCommand;
import com.nisovin.shopkeepers.commands.lib.CommandRegistry;
import com.nisovin.shopkeepers.commands.lib.commands.HelpCommand;
import com.nisovin.shopkeepers.text.Text;

public class ShopkeepersAdminCommand extends BaseCommand {

	private static final String COMMAND_NAME = "shopkeepersadmin";
	private static final String COMMAND_ALIAS = "skadmin";

	public static final String PERMISSION = "shopkeeper.admin.commands";

	public ShopkeepersAdminCommand(SKShopkeepersPlugin plugin) {
		super(COMMAND_NAME, Arrays.asList(COMMAND_ALIAS));
		this.setPermission(PERMISSION);
		this.setDescription(Text.of("Shopkeepers admin commands."));

		CommandRegistry childCommands = this.getChildCommands();
		childCommands.register(new HelpCommand("help", Unsafe.initialized(this)));
		childCommands.register(new CommandViewDistance(plugin));
	}
}
