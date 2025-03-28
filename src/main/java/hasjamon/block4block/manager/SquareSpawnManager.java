package hasjamon.block4block.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.Bukkit.getLogger;

public class SquareSpawnManager {

    private final JavaPlugin plugin;
    private int currentX = 0;
    private int currentZ = 0;
    private boolean directionX = true; // true for X, false for Z

    public SquareSpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Schedule periodic spawn updates based on update_interval
    public void scheduleSquareSpawnUpdate(World world, long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> updateSpawnWithSquare(world), 0L, interval);
    }

    public void loadCurrentSpawnPosition() {
        currentX = plugin.getConfig().getInt("square.currentX", 0);  // Default to 0 if not set
        currentZ = plugin.getConfig().getInt("square.currentZ", 0);
        directionX = plugin.getConfig().getBoolean("square.directionX", true);
        plugin.getLogger().info("Loaded square spawn position: X=" + currentX + ", Z=" + currentZ);
    }

    // Update spawn based on square_step config
    public void updateSpawnWithSquare(World world) {
        String squareStepConfig = plugin.getConfig().getString("square_step", "chunkcenter");

        int multiplier;
        boolean chunkCenter = false;

        // Determine if square_step is "chunkcenter" or a numeric value
        if (squareStepConfig.equalsIgnoreCase("chunkcenter")) {
            multiplier = 16; // Default chunk size for centering
            chunkCenter = true;
        } else {
            try {
                multiplier = Integer.parseInt(squareStepConfig);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid value for square_step in config. Defaulting to chunkcenter.");
                multiplier = 16;
                chunkCenter = true;
            }
        }

        // Calculate new spawn coordinates
        int x = currentX * multiplier;
        int z = currentZ * multiplier;

        // Apply chunk center offset if chunkcenter is enabled
        if (chunkCenter) {
            int centerOffset = 8; // Center of a 16x16 chunk
            x += centerOffset;
            z += centerOffset;
        }

        // Get the highest Y-coordinate and set the new spawn location
        Location newSpawn = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        world.setSpawnLocation(newSpawn.getBlockX(), newSpawn.getBlockY(), newSpawn.getBlockZ());

        // Move to the next square coordinate
        if (directionX) {
            currentX++;
        } else {
            currentZ++;
        }
        directionX = !directionX; // Alternate between X and Z
        // Save updated position
        saveCurrentSpawnPosition(currentX, currentZ);
        getLogger().info("New spawn location: X=" + x + ", Z=" + z);
    }

    public void saveCurrentSpawnPosition(int x, int z) {
        plugin.getConfig().set("square.currentX", x);
        plugin.getConfig().set("square.currentZ", z);
        plugin.saveConfig();
    }

    // Reset square cycle back to the starting point
    public void resetSquareCycle() {
        currentX = 0;
        currentZ = 0;
        directionX = true;
    }

    public int getCurrentX() {
        return currentX;
    }

    public int getCurrentZ() {
        return currentZ;
    }
}
