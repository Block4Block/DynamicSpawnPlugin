package hasjamon.block4block;

import hasjamon.block4block.manager.SpiralSpawnManager;
import hasjamon.block4block.manager.SquareSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DynamicSpawnPlugin extends JavaPlugin implements Listener {

    private boolean isUpdatingSpawn = false;
    private SpiralSpawnManager spiralManager;
    private SquareSpawnManager squareManager;
    private String mode;
    private String respawnUpdateMode; // "none", "respawn-no-bed", or "respawn-all"
    private long tickInterval;       // Tick interval from config

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Save default config if none exists

        // Initialize managers AFTER saving the config
        spiralManager = new SpiralSpawnManager(this);
        squareManager = new SquareSpawnManager(this);

        // Load saved spawn coordinates from config
        squareManager.loadCurrentSpawnPosition();
        spiralManager.loadCurrentSpawnPosition();

        // Register event listeners for respawn and spawn change triggers
        Bukkit.getPluginManager().registerEvents(this, this);

        // Get the target world from config (default: "world")
        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            getLogger().severe("World not found! Please check your config.yml");
            return;
        }

        // Read mode, update interval, and respawn settings
        mode = getConfig().getString("mode", "spiral").toLowerCase();
        String tickIntervalConfig = getConfig().getString("update_interval", "daily").toLowerCase();
        tickInterval = parseTickInterval(tickIntervalConfig);
        respawnUpdateMode = getConfig().getString("update_on_respawn", "none").toLowerCase();

        // Schedule updates based on tick interval
        if (tickInterval > 0) {
            scheduleTickBasedUpdates(world);
        }

        if (!respawnUpdateMode.equals("none")) {
            getLogger().info("Respawn-based update enabled with mode: " + respawnUpdateMode);
        }
    }

    @Override
    public void onDisable() {
        // Save current spawn positions on disable
        squareManager.saveCurrentSpawnPosition(squareManager.getCurrentX(), squareManager.getCurrentZ());
        spiralManager.saveCurrentSpawnPosition(spiralManager.getRadius(), spiralManager.getAngle());
        getLogger().info("DynamicSpawnPlugin disabled and spawn cycle saved!");
    }

    private void scheduleTickBasedUpdates(World world) {
        if (mode.equals("spiral")) {
            spiralManager.scheduleSpiralSpawnUpdate(world, tickInterval);
            getLogger().info("Spiral mode enabled with tick interval: " + tickInterval + " ticks.");
        } else if (mode.equals("square")) {
            squareManager.scheduleSquareSpawnUpdate(world, tickInterval);
            getLogger().info("Square mode enabled with tick interval: " + tickInterval + " ticks.");
        } else {
            getLogger().warning("Invalid mode! Defaulting to spiral mode.");
            spiralManager.scheduleSpiralSpawnUpdate(world, tickInterval);
        }
    }

    // Parse tick-based update interval
    private long parseTickInterval(String interval) {
        switch (interval.toLowerCase()) {
            case "daily":
                return 24L * 60 * 60 * 20; // 1728000 ticks
            case "weekly":
                return 7L * 24 * 60 * 60 * 20; // 12096000 ticks
            case "monthly":
                return 30L * 24 * 60 * 60 * 20; // 51840000 ticks
            case "yearly":
                return 365L * 24 * 60 * 60 * 20; // 631152000 ticks
            case "0":
                return 0; // Disable tick-based updates
            default:
                try {
                    return Long.parseLong(interval);
                } catch (NumberFormatException e) {
                    getLogger().warning("Invalid update_interval in config. Using default (daily).");
                    return 24L * 60 * 60 * 20;
                }
        }
    }

    // Listen for player respawn events if respawn updates are enabled.
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (respawnUpdateMode.equals("none")) return;

        Player player = event.getPlayer();

        // For "respawn-no-bed", only update if the player has no bed spawn.
        if (respawnUpdateMode.equals("respawn-no-bed") && player.getBedSpawnLocation() != null) {
            return;
        }

        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            getLogger().warning("World not found on respawn event.");
            return;
        }

        // Prevent multiple spawn updates if we're already updating
        if (isUpdatingSpawn) return;

        isUpdatingSpawn = true;  // Lock updates temporarily

        // Trigger the spawn update based on the current mode.
        if (mode.equals("spiral")) {
            spiralManager.updateSpawnWithSpiral(world);
            getLogger().info("Spawn updated via respawn trigger (spiral mode).");
        } else if (mode.equals("square")) {
            squareManager.updateSpawnWithSquare(world);
            getLogger().info("Spawn updated via respawn trigger (square mode).");
        }

        isUpdatingSpawn = false;  // Unlock updates
    }

    // Prevent duplicate updates on spawn change
    @EventHandler
    public void onSpawnChange(SpawnChangeEvent event) {
        if (isUpdatingSpawn) return;  // Ignore if we're already modifying the spawn

        World world = event.getWorld();
        getLogger().info("World spawn changed. Resetting spawn cycle.");

        // Reset the spawn cycle based on mode
        spiralManager.resetSpiralCycle();
        squareManager.resetSquareCycle();

        // Reschedule the tick-based updates from the new spawn
        if (tickInterval > 0) {
            scheduleTickBasedUpdates(world);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            sender.sendMessage("World not found.");
            return true;
        }

        if ("forcespawnmove".equalsIgnoreCase(command.getName())) {
            if (mode.equals("spiral")) {
                spiralManager.updateSpawnWithSpiral(world);
                sender.sendMessage("Spawn updated using spiral mode!");
            } else if (mode.equals("square")) {
                squareManager.updateSpawnWithSquare(world);
                sender.sendMessage("Spawn updated using square mode!");
            }
            return true;
        }

        if ("checkspawn".equalsIgnoreCase(command.getName())) {
            int x = world.getSpawnLocation().getBlockX();
            int y = world.getSpawnLocation().getBlockY();
            int z = world.getSpawnLocation().getBlockZ();
            sender.sendMessage("§aCurrent world spawn coordinates: §e" + x + ", " + y + ", " + z);
            return true;
        }
        return false;
    }
}
