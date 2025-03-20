package hasjamon.block4block;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SpiralSpawnPlugin extends JavaPlugin {

    private SpiralSpawnManager spawnManager;

    @Override
    public void onEnable() {
        // Save the default config if one doesn't exist.
        saveDefaultConfig();

        // Initialize the SpiralSpawnManager.
        spawnManager = new SpiralSpawnManager(this);

        // Register an optional command to force a spawn update.
        getCommand("forcespawnmove").setExecutor(this);

        // Get the target world from config (default: "world").
        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            getLogger().severe("World not found! Please check your config.yml");
            return;
        }

        // Get the update interval from config (supports keywords and manual ticks)
        long updateInterval = parseUpdateInterval(getConfig().getString("update_interval", "daily"));

        // Schedule the spiral update task.
        spawnManager.scheduleSpiralSpawnUpdate(world, updateInterval);

        getLogger().info("SpiralSpawnPlugin enabled with update interval: " + updateInterval + " ticks.");
    }

    // Parse interval from keyword or manual ticks
    private long parseUpdateInterval(String interval) {
        switch (interval.toLowerCase()) {
            case "daily":
                return 24L * 60 * 60 * 20; // 1728000 ticks
            case "weekly":
                return 7L * 24 * 60 * 60 * 20; // 12096000 ticks
            case "monthly":
                return 30L * 24 * 60 * 60 * 20; // 51840000 ticks
            case "yearly":
                return 365L * 24 * 60 * 60 * 20; // 631152000 ticks
            default:
                try {
                    return Long.parseLong(interval);
                } catch (NumberFormatException e) {
                    getLogger().warning("Invalid update interval in config. Using default (daily).");
                    return 24L * 60 * 60 * 20; // Default to daily if invalid input
                }
        }
    }


    @Override
    public void onDisable() {
        getLogger().info("SpiralSpawnPlugin disabled!");
    }

    // Optional commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("forcespawnmove".equalsIgnoreCase(command.getName())) {
            World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
            if (world == null) {
                sender.sendMessage("World not found.");
                return true;
            }
            spawnManager.updateSpawnWithSpiral(world);
            sender.sendMessage("Spawn updated using spiral mode!");
            return true;
        }

        // Handle /checkspawn command
        if (command.getName().equalsIgnoreCase("checkspawn")) {
            World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
            if (world == null) {
                sender.sendMessage("World not found.");
                return true;
            }

            // Get the current spawn location
            Location spawn = world.getSpawnLocation();
            int x = spawn.getBlockX();
            int y = spawn.getBlockY();
            int z = spawn.getBlockZ();

            // Send the coordinates to the player or console
            sender.sendMessage("§aCurrent world spawn coordinates: §e" + x + ", " + y + ", " + z);
            return true;
        }

        return false;
    }
}
