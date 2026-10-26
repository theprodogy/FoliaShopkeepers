package com.nisovin.shopkeepers.villagers;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTransformEvent.TransformReason;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Prevents curing of zombie villagers.
 */
public class BlockZombieVillagerCuringListener implements Listener {

	public BlockZombieVillagerCuringListener() {
	}

	private boolean isZombieVillagerCuringDisabled(World world) {
		return Settings.disableZombieVillagerCuring
				&& (Settings.disableZombieVillagerCuringWorlds.isEmpty()
						|| Settings.disableZombieVillagerCuringWorlds.contains(world.getName()));
	}

	// Try to prevent curing as early as possible, so that the player doesn't waste his golden
	// apple.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onZombieVillagerCureStarted(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ZombieVillager)) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack itemInHand = player.getInventory().getItem(event.getHand());
		if (itemInHand.getType() != Material.GOLDEN_APPLE) {
			return;
		}

		if (!this.isZombieVillagerCuringDisabled(player.getWorld())) {
			return;
		}

		// Prevent curing:
		Log.debug(() -> "Preventing zombie villager curing at "
				+ TextUtils.getLocationString(player.getLocation()));
		event.setCancelled(true);
		TextUtils.sendMessage(player, Messages.zombieVillagerCuringDisabled);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onZombieVillagerCured(EntityTransformEvent event) {
		if (event.getTransformReason() != TransformReason.CURED) {
			return;
		}

		if (!(event.getEntity() instanceof ZombieVillager zombieVillager)) {
			return;
		}

		if (!this.isZombieVillagerCuringDisabled(zombieVillager.getWorld())) {
			return;
		}

		Log.debug(() -> "Preventing zombie villager curing (transform) at "
				+ TextUtils.getLocationString(zombieVillager.getLocation()));
		event.setCancelled(true);

		// Inform the player who initiated the curing:
		OfflinePlayer conversionOfflinePlayer = zombieVillager.getConversionPlayer();
		if (conversionOfflinePlayer != null) {
			Player conversionPlayer = conversionOfflinePlayer.getPlayer();
			if (conversionPlayer != null) {
				TextUtils.sendMessage(conversionPlayer, Messages.zombieVillagerCuringDisabled);
			}
		}
	}
}
