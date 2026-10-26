package com.nisovin.shopkeepers.shopkeeper.visibility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.logging.Log;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Applies {@link Settings#shopkeeperViewDistance} as the tracking range of shopkeeper entities.
 */
public class ShopkeeperViewDistance {

	public static final int ALWAYS_VISIBLE = -1;
	public static final int SERVER_DEFAULT = 0;

	// Above the max view distance of 32 chunks. Not larger, because the server scales the range.
	private static final int ALWAYS_VISIBLE_RANGE = 1024;

	private static final long UPDATE_PERIOD_TICKS = 20L;

	private final SKShopkeepersPlugin plugin;
	private @Nullable ScheduledTask task = null;

	private final Map<Object, Integer> defaultRanges = new WeakHashMap<>();

	private @Nullable Method getHandleMethod = null;
	private @Nullable Method getTrackedEntityMethod = null;
	private @Nullable Field rangeField = null;
	private boolean unsupported = false;

	public ShopkeeperViewDistance(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		task = SchedulerUtils.runTaskTimerOrOmit(
				plugin,
				this::updateAllShopkeepers,
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

	private void updateAllShopkeepers() {
		if (unsupported) return;

		for (AbstractShopkeeper shopkeeper : plugin.getShopkeeperRegistry().getActiveShopkeepers()) {
			if (!(shopkeeper.getShopObject() instanceof EntityShopObject shopObject)) continue;

			Entity entity = shopObject.getEntity();
			if (entity == null) continue;

			if (SchedulerUtils.isFolia()) {
				SchedulerUtils.runEntityTaskOrOmit(plugin, entity, () -> this.update(entity));
			} else {
				this.update(entity);
			}
		}
	}

	private void update(Entity entity) {
		if (unsupported || !entity.isValid()) return;

		try {
			Object trackedEntity = this.getTrackedEntity(entity);
			if (trackedEntity == null) return; // Not tracked yet

			Field rangeField = this.getRangeField(trackedEntity);
			Integer defaultRange = defaultRanges.get(trackedEntity);
			if (defaultRange == null) {
				defaultRange = rangeField.getInt(trackedEntity);
				defaultRanges.put(trackedEntity, defaultRange);
			}

			int range = this.getRange(Settings.shopkeeperViewDistance, defaultRange);
			if (rangeField.getInt(trackedEntity) != range) {
				rangeField.setInt(trackedEntity, range);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			unsupported = true;
			Log.warning("Shopkeeper view distance is not supported on this server version.", e);
		}
	}

	private int getRange(int viewDistance, int defaultRange) {
		if (viewDistance == SERVER_DEFAULT) return defaultRange;
		if (viewDistance == ALWAYS_VISIBLE) return ALWAYS_VISIBLE_RANGE;
		return viewDistance;
	}

	private @Nullable Object getTrackedEntity(Entity entity) throws ReflectiveOperationException {
		Method getHandleMethod = this.getHandleMethod;
		if (getHandleMethod == null) {
			// Subclasses override this method for their entity type, so we use the base class:
			Class<?> craftEntityClass = Class.forName(
					"org.bukkit.craftbukkit.entity.CraftEntity",
					false,
					entity.getClass().getClassLoader()
			);
			getHandleMethod = craftEntityClass.getMethod("getHandle");
			this.getHandleMethod = getHandleMethod;
		}

		Object handle = getHandleMethod.invoke(entity);
		if (handle == null) return null;

		Method getTrackedEntityMethod = this.getTrackedEntityMethod;
		if (getTrackedEntityMethod == null) {
			getTrackedEntityMethod = handle.getClass().getMethod("moonrise$getTrackedEntity");
			this.getTrackedEntityMethod = getTrackedEntityMethod;
		}

		return getTrackedEntityMethod.invoke(handle);
	}

	private Field getRangeField(Object trackedEntity) throws ReflectiveOperationException {
		Field rangeField = this.rangeField;
		if (rangeField == null) {
			rangeField = trackedEntity.getClass().getDeclaredField("range");
			rangeField.setAccessible(true);
			this.rangeField = rangeField;
		}
		return rangeField;
	}
}
