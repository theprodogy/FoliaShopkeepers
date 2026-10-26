package com.nisovin.shopkeepers.util.bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Scheduler related utilities.
 * <p>
 * All tasks are scheduled via Paper's region aware schedulers, which also work on Folia:
 * <ul>
 * <li>"Main thread" tasks run on the global region.
 * <li>Entity tasks run on the region that owns the entity.
 * <li>Chunk tasks run on the region that owns the chunk.
 * </ul>
 */
public final class SchedulerUtils {

	private static final long MILLIS_PER_TICK = 50L;

	// Paper's async scheduler does not expose its running tasks, so we count them ourselves:
	private static final Map<Plugin, AtomicInteger> ACTIVE_ASYNC_TASKS = new ConcurrentHashMap<>();

	/**
	 * Creates an {@link Executor} that executes tasks on the global region using
	 * {@link #runOnMainThreadOrOmit(Plugin, Runnable)}.
	 * <p>
	 * If the thread registering the task is already a server tick thread, the task is run
	 * immediately. Otherwise, it is scheduled on the global region. If the plugin is not enabled at
	 * the time of task registration, the task is omitted.
	 *
	 * @param plugin
	 *            the plugin
	 * @return the executor
	 */
	public static Executor createSyncExecutor(Plugin plugin) {
		return (runnable) -> runOnMainThreadOrOmit(plugin, runnable);
	}

	/**
	 * Creates an {@link Executor} that executes tasks using
	 * {@link #runAsyncTaskOrOmit(Plugin, Runnable)}.
	 *
	 * @param plugin
	 *            the plugin
	 * @return the executor
	 */
	public static Executor createAsyncExecutor(Plugin plugin) {
		return (runnable) -> runAsyncTaskOrOmit(plugin, runnable);
	}

	public static int getActiveAsyncTasks(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		AtomicInteger counter = ACTIVE_ASYNC_TASKS.get(plugin);
		return (counter == null) ? 0 : counter.get();
	}

	private static void validatePluginTask(Plugin plugin, Runnable task) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(task, "task is null");
	}

	/**
	 * Checks if the current thread is a server tick thread.
	 * <p>
	 * On Folia, this is the case for the global region thread and all region threads.
	 *
	 * @return <code>true</code> if currently running on a server tick thread
	 */
	public static boolean isMainThread() {
		return Bukkit.isPrimaryThread();
	}

	/**
	 * Schedules the given task to be run on the global region if required.
	 * <p>
	 * If the current thread is already a server tick thread, the task will be run immediately.
	 * Otherwise, it attempts to schedule the task to run on the global region. However, if the
	 * plugin is disabled, the task won't be scheduled.
	 *
	 * @param plugin
	 *            the plugin to use for scheduling, not <code>null</code>
	 * @param task
	 *            the task, not <code>null</code>
	 * @return <code>true</code> if the task was run or successfully scheduled to be run,
	 *         <code>false</code> otherwise
	 */
	public static boolean runOnMainThreadOrOmit(Plugin plugin, Runnable task) {
		validatePluginTask(plugin, task);
		if (isMainThread()) {
			task.run();
			return true;
		} else {
			return (runTaskOrOmit(plugin, task) != null);
		}
	}

	public static @Nullable ScheduledTask runTaskOrOmit(Plugin plugin, Runnable task) {
		return runTaskLaterOrOmit(plugin, task, 0L);
	}

	/**
	 * Runs the given task on the global region after the given delay.
	 *
	 * @param plugin
	 *            the plugin
	 * @param task
	 *            the task
	 * @param delay
	 *            the delay in ticks, values below <code>1</code> run the task during the next tick
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled
	 */
	public static @Nullable ScheduledTask runTaskLaterOrOmit(
			Plugin plugin,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		// Tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			try {
				var scheduler = Bukkit.getGlobalRegionScheduler();
				if (delay < 1L) {
					return scheduler.run(plugin, scheduledTask -> task.run());
				} else {
					return scheduler.runDelayed(plugin, scheduledTask -> task.run(), delay);
				}
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	/**
	 * Runs the given task repeatedly on the global region.
	 *
	 * @param plugin
	 *            the plugin
	 * @param task
	 *            the task
	 * @param delay
	 *            the initial delay in ticks, values below <code>1</code> are treated as
	 *            <code>1</code>
	 * @param period
	 *            the period in ticks, values below <code>1</code> are treated as <code>1</code>
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled
	 */
	public static @Nullable ScheduledTask runTaskTimerOrOmit(
			Plugin plugin,
			Runnable task,
			long delay,
			long period
	) {
		validatePluginTask(plugin, task);
		if (plugin.isEnabled()) {
			try {
				return Bukkit.getGlobalRegionScheduler().runAtFixedRate(
						plugin,
						scheduledTask -> task.run(),
						Math.max(1L, delay),
						Math.max(1L, period)
				);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	/**
	 * Runs the given task on the region that owns the given entity after the given delay.
	 * <p>
	 * The task is not run if the entity is removed before the task executes.
	 *
	 * @param plugin
	 *            the plugin
	 * @param entity
	 *            the entity
	 * @param task
	 *            the task
	 * @param delay
	 *            the delay in ticks, values below <code>1</code> are treated as <code>1</code>
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled or the entity
	 *         has already been removed
	 */
	public static @Nullable ScheduledTask runEntityTaskLaterOrOmit(
			Plugin plugin,
			Entity entity,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		Validate.notNull(entity, "entity is null");
		if (plugin.isEnabled()) {
			try {
				return entity.getScheduler().runDelayed(
						plugin,
						scheduledTask -> task.run(),
						null,
						Math.max(1L, delay)
				);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	public static @Nullable ScheduledTask runEntityTaskOrOmit(
			Plugin plugin,
			Entity entity,
			Runnable task
	) {
		return runEntityTaskLaterOrOmit(plugin, entity, task, 1L);
	}

	/**
	 * Runs the given task repeatedly on the region that owns the given entity.
	 * <p>
	 * The task stops if the entity is removed.
	 *
	 * @param plugin
	 *            the plugin
	 * @param entity
	 *            the entity
	 * @param task
	 *            the task
	 * @param delay
	 *            the initial delay in ticks, values below <code>1</code> are treated as
	 *            <code>1</code>
	 * @param period
	 *            the period in ticks, values below <code>1</code> are treated as <code>1</code>
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled or the entity
	 *         has already been removed
	 */
	public static @Nullable ScheduledTask runEntityTaskTimerOrOmit(
			Plugin plugin,
			Entity entity,
			Runnable task,
			long delay,
			long period
	) {
		validatePluginTask(plugin, task);
		Validate.notNull(entity, "entity is null");
		if (plugin.isEnabled()) {
			try {
				return entity.getScheduler().runAtFixedRate(
						plugin,
						scheduledTask -> task.run(),
						null,
						Math.max(1L, delay),
						Math.max(1L, period)
				);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	/**
	 * Runs the given task on the region that owns the given chunk after the given delay.
	 * 
	 * @param plugin
	 *            the plugin
	 * @param world
	 *            the world
	 * @param chunkX
	 *            the chunk x coordinate
	 * @param chunkZ
	 *            the chunk z coordinate
	 * @param task
	 *            the task
	 * @param delay
	 *            the delay in ticks, values below <code>1</code> run the task during the next tick
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled
	 */
	public static @Nullable ScheduledTask runRegionTaskLaterOrOmit(
			Plugin plugin,
			World world,
			int chunkX,
			int chunkZ,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		Validate.notNull(world, "world is null");
		if (plugin.isEnabled()) {
			try {
				var scheduler = Bukkit.getRegionScheduler();
				if (delay < 1L) {
					return scheduler.run(plugin, world, chunkX, chunkZ, scheduledTask -> task.run());
				} else {
					return scheduler.runDelayed(
							plugin,
							world,
							chunkX,
							chunkZ,
							scheduledTask -> task.run(),
							delay
					);
				}
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	public static @Nullable ScheduledTask runAsyncTaskOrOmit(Plugin plugin, Runnable task) {
		return runAsyncTaskLaterOrOmit(plugin, task, 0L);
	}

	public static @Nullable ScheduledTask runAsyncTaskLaterOrOmit(
			Plugin plugin,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		return runAsyncTaskLaterOrOmit(plugin, scheduledTask -> task.run(), delay);
	}

	/**
	 * Runs the given task asynchronously after the given delay.
	 * <p>
	 * The task receives its own {@link ScheduledTask}, because the task may already start running
	 * before this method returns.
	 * 
	 * @param plugin
	 *            the plugin
	 * @param task
	 *            the task
	 * @param delay
	 *            the delay in ticks
	 * @return the scheduled task, or <code>null</code> if the plugin is not enabled
	 */
	public static @Nullable ScheduledTask runAsyncTaskLaterOrOmit(
			Plugin plugin,
			Consumer<ScheduledTask> task,
			long delay
	) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(task, "task is null");
		// Tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			AtomicInteger counter = ACTIVE_ASYNC_TASKS.computeIfAbsent(
					plugin,
					key -> new AtomicInteger()
			);
			// Counted from scheduling until completion, so that pending tasks are awaited as well:
			counter.incrementAndGet();
			try {
				var scheduler = Bukkit.getAsyncScheduler();
				Consumer<ScheduledTask> asyncTask = scheduledTask -> {
					try {
						task.accept(scheduledTask);
					} finally {
						counter.decrementAndGet();
					}
				};
				ScheduledTask scheduledTask;
				if (delay < 1L) {
					scheduledTask = scheduler.runNow(plugin, asyncTask);
				} else {
					scheduledTask = scheduler.runDelayed(
							plugin,
							asyncTask,
							delay * MILLIS_PER_TICK,
							TimeUnit.MILLISECONDS
					);
				}
				return new CountedAsyncTask(scheduledTask, counter);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
				counter.decrementAndGet();
			}
		}
		return null;
	}

	// Decrements the async task counter if the task gets cancelled before it started executing.
	private static final class CountedAsyncTask implements ScheduledTask {

		private final ScheduledTask task;
		private final AtomicInteger counter;

		CountedAsyncTask(ScheduledTask task, AtomicInteger counter) {
			this.task = task;
			this.counter = counter;
		}

		@Override
		public Plugin getOwningPlugin() {
			return task.getOwningPlugin();
		}

		@Override
		public boolean isRepeatingTask() {
			return task.isRepeatingTask();
		}

		@Override
		public CancelledState cancel() {
			CancelledState state = task.cancel();
			if (state == CancelledState.CANCELLED_BY_CALLER) {
				counter.decrementAndGet();
			}
			return state;
		}

		@Override
		public ExecutionState getExecutionState() {
			return task.getExecutionState();
		}
	}

	/**
	 * Cancels all global region and async tasks of the given plugin.
	 * <p>
	 * Entity tasks are retired together with their entities.
	 *
	 * @param plugin
	 *            the plugin
	 */
	public static void cancelTasks(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
		Bukkit.getAsyncScheduler().cancelTasks(plugin);
		ACTIVE_ASYNC_TASKS.remove(plugin);
	}

	/**
	 * Awaits the completion of async tasks of the specified plugin.
	 * <p>
	 * If a logger is specified, it will be used to print informational messages suited to the
	 * context of this method being called during disabling of the plugin.
	 *
	 * @param plugin
	 *            the plugin
	 * @param asyncTasksTimeoutSeconds
	 *            the duration to wait for async tasks to finish in seconds (can be <code>0</code>)
	 * @param logger
	 *            the logger used for printing informational messages, can be <code>null</code>
	 * @return the number of remaining async tasks that are still running after waiting for the
	 *         specified duration
	 */
	public static int awaitAsyncTasksCompletion(
			Plugin plugin,
			int asyncTasksTimeoutSeconds,
			@Nullable Logger logger
	) {
		Validate.notNull(plugin, "plugin is null");
		Validate.isTrue(asyncTasksTimeoutSeconds >= 0, "asyncTasksTimeoutSeconds cannot be negative");

		int activeAsyncTasks = getActiveAsyncTasks(plugin);
		if (activeAsyncTasks > 0 && asyncTasksTimeoutSeconds > 0) {
			if (logger != null) {
				logger.info("Waiting up to " + asyncTasksTimeoutSeconds + " seconds for "
						+ activeAsyncTasks + " remaining async tasks to finish ...");
			}

			final long asyncTasksTimeoutMillis = TimeUnit.SECONDS.toMillis(asyncTasksTimeoutSeconds);
			final long waitStartNanos = System.nanoTime();
			long waitDurationMillis = 0L;
			do {
				// Periodically check again:
				try {
					Thread.sleep(25L);
				} catch (InterruptedException e) {
					// Ignore, but reset interrupt flag:
					Thread.currentThread().interrupt();
				}
				// Update the number of active async task before breaking from loop:
				activeAsyncTasks = getActiveAsyncTasks(plugin);

				// Update waiting duration and compare to timeout:
				waitDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStartNanos);
				if (waitDurationMillis > asyncTasksTimeoutMillis) {
					// Timeout reached, abort waiting..
					break;
				}
			} while (activeAsyncTasks > 0);

			if (waitDurationMillis > 1 && logger != null) {
				logger.info("Waited " + waitDurationMillis + " ms for async tasks to finish.");
			}
		}

		if (activeAsyncTasks > 0 && logger != null) {
			// Severe, since this can potentially result in data loss, depending on what the tasks
			// are doing:
			logger.severe("There are still " + activeAsyncTasks
					+ " remaining async tasks active! Disabling anyway now.");
		}
		return activeAsyncTasks;
	}

	private SchedulerUtils() {
	}
}
