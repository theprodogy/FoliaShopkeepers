package com.nisovin.shopkeepers.shopkeeper.visibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Hides shopkeeper entities from players that are further away than the configured
 * {@link Settings#shopkeeperViewDistance}.
 * <p>
 * The server's entity tracking range still limits how far shopkeeper entities are visible.
 */
public class ShopkeeperViewDistance {

	/**
	 * The setting value that disables the view distance limit.
	 */
	public static final int ALWAYS_VISIBLE = -1;

	private static final long UPDATE_PERIOD_TICKS = 20L;

	private final SKShopkeepersPlugin plugin;
	private @Nullable ScheduledTask task = null;

	public ShopkeeperViewDistance(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		task = SchedulerUtils.runTaskTimerOrOmit(
				plugin,
				this::updateAllPlayers,
				UPDATE_PERIOD_TICKS,
				UPDATE_PERIOD_TICKS
		);
	}

	public void onDisable() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void updateAllPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			assert player != null;
			// Changes to the player's visible entities run on the player's region:
			SchedulerUtils.runEntityTaskOrOmit(plugin, player, () -> this.updatePlayer(player));
		}
	}

	private void updatePlayer(Player player) {
		if (!player.isOnline()) return;

		int viewDistance = Settings.shopkeeperViewDistance;
		long maxDistanceSquared = (long) viewDistance * viewDistance;
		World world = player.getWorld();
		Location playerLocation = player.getLocation();
		for (AbstractShopkeeper shopkeeper : plugin.getShopkeeperRegistry().getActiveShopkeepers()) {
			if (!(shopkeeper.getShopObject() instanceof EntityShopObject shopObject)) continue;

			Entity entity = shopObject.getEntity();
			if (entity == null || !world.equals(entity.getWorld())) continue;

			boolean visible = (viewDistance == ALWAYS_VISIBLE)
					|| entity.getLocation().distanceSquared(playerLocation) <= maxDistanceSquared;
			if (visible == player.canSee(entity)) continue;

			if (visible) {
				player.showEntity(plugin, entity);
			} else {
				player.hideEntity(plugin, entity);
			}
		}
	}
}
