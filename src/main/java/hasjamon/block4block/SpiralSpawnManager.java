package hasjamon.block4block;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class SpiralSpawnManager {

    private final SpiralSpawnPlugin plugin;
    // Step counter to determine the next position in the spiral.
    private int spiralStep = 1;

    public SpiralSpawnManager(SpiralSpawnPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the next spiral location based on the current spawn.
     *
     * @param world  The world to update.
     * @param center The current spawn point.
     * @param step   The current step in the spiral.
     * @return The new location for the spawn.
     */
    public Location getNextSpiralLocation(World world, Location center, int step) {
        // Get step distance and angle divisor from config with defaults.
        int distancePerStep = plugin.getConfig().getInt("spiral_step_distance", 4);
        int angleDivisor = plugin.getConfig().getInt("spiral_angle_divisor", 16);

        // Define a tighter step distance based on config.
        int distance = step * distancePerStep;

        // Calculate a tighter angle increment based on config.
        double angle = step * Math.PI / angleDivisor;

        // Calculate the new X and Z positions using trigonometry.
        int newX = center.getBlockX() + (int) (distance * Math.cos(angle));
        int newZ = center.getBlockZ() + (int) (distance * Math.sin(angle));

        // Get the highest block Y-coordinate at the new X/Z location.
        int newY = world.getHighestBlockYAt(newX, newZ);

        // Return the new location with adjusted coordinates.
        return new Location(world, newX, newY, newZ);
    }


    /**
     * Updates the world spawn using the spiral pattern.
     *
     * @param world The world to update.
     */
    public void updateSpawnWithSpiral(World world) {
        Location currentSpawn = world.getSpawnLocation();
        Location newSpawn = getNextSpiralLocation(world, currentSpawn, spiralStep++);
        world.setSpawnLocation(newSpawn);
        Bukkit.broadcastMessage(ChatColor.AQUA + "Spawn moved in spiral pattern to: " +
                newSpawn.getBlockX() + ", " + newSpawn.getBlockY() + ", " + newSpawn.getBlockZ());
    }

    /**
     * Schedules a repeating task to update the spawn using the spiral pattern.
     *
     * @param world          The world to update.
     * @param updateInterval The interval between updates in ticks.
     */
    public void scheduleSpiralSpawnUpdate(World world, long updateInterval) {
        // Cancel any existing tasks to avoid multiple timers running
        Bukkit.getScheduler().cancelTasks(plugin);

        // Schedule the spiral update task at the desired interval.
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSpawnWithSpiral(world);
            }
        }.runTaskTimer(plugin, updateInterval, updateInterval);

        plugin.getLogger().info("Spiral spawn update scheduled every " + updateInterval + " ticks.");
    }
}
