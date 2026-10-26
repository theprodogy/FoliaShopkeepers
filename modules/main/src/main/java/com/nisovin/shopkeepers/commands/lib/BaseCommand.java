package com.nisovin.shopkeepers.commands.lib;

import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Implements a Paper {@link BasicCommand} by invoking our command handling.
 * <p>
 * The command still has to be registered via Paper's lifecycle API.
 */
public abstract class BaseCommand extends Command implements BasicCommand {

	/**
	 * Creates a new {@link BaseCommand}.
	 *
	 * @param name
	 *            the command name
	 * @param aliases
	 *            the command aliases
	 */
	public BaseCommand(String name, List<? extends String> aliases) {
		super(name, aliases);
	}

	@Override
	public void execute(CommandSourceStack source, @NonNull String[] args) {
		// Paper does not provide the used alias: We use the command name instead.
		CommandInput input = new CommandInput(source.getSender(), this, this.getName(), args);
		// We completely handle the command, including printing usage or help on syntax failure:
		this.handleCommand(input);
	}

	@Override
	public Collection<String> suggest(CommandSourceStack source, @NonNull String[] args) {
		CommandInput input = new CommandInput(source.getSender(), this, this.getName(), args);
		return Unsafe.cast(this.handleTabCompletion(input));
	}
}
