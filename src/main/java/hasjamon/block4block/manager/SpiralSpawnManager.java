package hasjamon.block4block.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class SpiralSpawnManager {

    private final JavaPlugin plugin;
    private int radius = 1;
    private int angle = 0;

    public SpiralSpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleSpiralSpawnUpdate(World world, long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> updateSpawnWithSpiral(world), 0L, interval);
    }

    public void loadCurrentSpawnPosition() {
        radius = plugin.getConfig().getInt("spiral.radius", 1);  // Default to 1 if not set
        angle = plugin.getConfig().getInt("spiral.angle", 0);
        plugin.getLogger().info("Loaded spiral spawn position: Radius=" + radius + ", Angle=" + angle);
    }

    public void updateSpawnWithSpiral(World world) {
        double radians = Math.toRadians(angle);
        int x = (int) (radius * Math.cos(radians));
        int z = (int) (radius * Math.sin(radians));
        Location newSpawn = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        world.setSpawnLocation(newSpawn.getBlockX(), newSpawn.getBlockY(), newSpawn.getBlockZ());

        angle += 30; // Increase by 30° each update
        if (angle >= 360) {
            angle = 0;
            radius += 5; // Increase radius after a full circle
        }
        // Save updated position
        saveCurrentSpawnPosition(x, z);
    }

    public void saveCurrentSpawnPosition(int x, int z) {
        plugin.getConfig().set("spiral.currentRadius", x);
        plugin.getConfig().set("spiral.currentAngle", z);
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
