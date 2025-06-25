package hasjamon.block4block.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class SpiralSpawnManager {

    private final JavaPlugin plugin;
    private final int centerX;
    private final int centerZ;
    private int radius = 1;
    private int angle = 0;

    public SpiralSpawnManager(JavaPlugin plugin, int centerX, int centerZ) {
        this.plugin = plugin;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void scheduleSpiralSpawnUpdate(World world, long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> updateSpawnWithSpiral(world), 0L, interval);
    }

    public void loadCurrentSpawnPosition() {
        radius = plugin.getConfig().getInt("spiral.radius", 1);  // Default to 1 if not set
        angle = plugin.getConfig().getInt("spiral.angle", 0);
        plugin.getLogger().info("Loaded spiral spawn position: Radius=" + radius + ", Angle=" + angle);
    }

    public Location updateSpawnWithSpiral(World world) {
        double radians = Math.toRadians(angle);
        int x = centerX + (int) (radius * Math.cos(Math.toRadians(angle)));
        int z = centerZ + (int) (radius * Math.sin(Math.toRadians(angle)));
        Location newSpawn = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        world.setSpawnLocation(newSpawn.getBlockX(), newSpawn.getBlockY(), newSpawn.getBlockZ());

        angle += 30; // Increase by 30° each update
        if (angle >= 360) {
            angle = 0;
            radius += 5; // Increase radius after a full circle
        }
        // Save updated position
        saveCurrentSpawnPosition(radius, angle);
        return newSpawn;
    }

    public void saveCurrentSpawnPosition(int radius, int angle) {
        plugin.getConfig().set("spiral.radius", radius);
        plugin.getConfig().set("spiral.angle", angle);
        plugin.saveConfig();
    }

    public void resetSpiralCycle() {
        radius = 1;
        angle = 0;
    }
    public int getRadius() {
        return radius;
    }

    public int getAngle() {
        return angle;
    }
}
