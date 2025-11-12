package hasjamon.block4block;

import hasjamon.block4block.manager.SpiralSpawnManager;
import hasjamon.block4block.manager.SquareSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private long lastUpdateMillis = 0;
    private boolean requireBothConditions;
    private boolean updateOnNewPlayerJoin;
    public int centerX = 0;
    public int centerZ = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Save default config if none exists

        centerX = getConfig().getInt("spawn_center.x", 0);
        centerZ = getConfig().getInt("spawn_center.z", 0);

        // Initialize managers AFTER saving the config
        spiralManager = new SpiralSpawnManager(this, centerX, centerZ);
        squareManager = new SquareSpawnManager(this, centerX, centerZ);

        lastUpdateMillis = System.currentTimeMillis(); // Initialize to current tick

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
        requireBothConditions = getConfig().getBoolean("require_both_conditions");
        updateOnNewPlayerJoin = getConfig().getBoolean("update_on_new_player_join", true);

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
        squareManager.saveCurrentSpawnPosition();
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

    public void moveSpawn(World world, String triggerReason) {
        if (isUpdatingSpawn) return;
        isUpdatingSpawn = true;

        Location newSpawn;

        if (mode.equals("spiral")) {
            newSpawn = spiralManager.updateSpawnWithSpiral(world);
        } else if (mode.equals("square")) {
            newSpawn = squareManager.updateSpawnWithSquare(world);
        } else {
            getLogger().warning("Unknown spawn mode: " + mode);
            isUpdatingSpawn = false;
            return;
        }

        lastUpdateMillis = System.currentTimeMillis();

        String message = getConfig().getString("broadcast_message_template",
                "§6Spawn moved via §e{reason}§6 ({mode} mode) §7(@ {x}, {y}, {z})");

        message = message
                .replace("{reason}", triggerReason)
                .replace("{mode}", mode)
                .replace("{x}", String.valueOf(newSpawn.getBlockX()))
                .replace("{y}", String.valueOf(newSpawn.getBlockY()))
                .replace("{z}", String.valueOf(newSpawn.getBlockZ()));

        Bukkit.broadcastMessage(message);
        getLogger().info("Spawn moved to X=" + newSpawn.getBlockX() +
                ", Y=" + newSpawn.getBlockY() +
                ", Z=" + newSpawn.getBlockZ());

        isUpdatingSpawn = false;
    }

    // Listen for player respawn events if respawn updates are enabled.
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (respawnUpdateMode.equals("none"))
            return;

        Player player = event.getPlayer();

        // Evaluate respawn condition
        boolean shouldUpdate = switch (respawnUpdateMode) {
            case "respawn-no-bed" -> player.getBedSpawnLocation() == null;
            case "respawn-all" -> true;
            default -> false;
        };

        if (!shouldUpdate)
            return;

        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            getLogger().warning("World not found on respawn event.");
            return;
        }

        long currentMillis = System.currentTimeMillis();
        long intervalMillis = tickInterval * 50;

        if (requireBothConditions && tickInterval > 0 && (currentMillis - lastUpdateMillis) < intervalMillis) {
            getLogger().info("Spawn update skipped — interval not yet passed.");
            return;
        }

        moveSpawn(world, "player respawn");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!updateOnNewPlayerJoin) return;

        Player player = event.getPlayer();

        // Only proceed if this is the player's first time joining
        if (player.hasPlayedBefore()) return;

        World world = Bukkit.getWorld(getConfig().getString("world-name", "world"));
        if (world == null) {
            getLogger().warning("World not found on join event.");
            return;
        }

        long currentMillis = System.currentTimeMillis();
        long intervalMillis = tickInterval * 50;

        if (requireBothConditions && tickInterval > 0 && (currentMillis - lastUpdateMillis) < intervalMillis) {
            getLogger().info("Spawn update skipped on first join — interval not yet passed.");
            return;
        }

        moveSpawn(world, "new player join");
    }

    // FIXED: Only react to EXTERNAL spawn changes (not our own)
    @EventHandler
    public void onSpawnChange(SpawnChangeEvent event) {
        // CRITICAL FIX: Ignore spawn changes we caused ourselves
        if (isUpdatingSpawn) {
            return;
        }

        // This spawn change was caused by something else (command, plugin, etc.)
        World world = event.getWorld();
        getLogger().info("External spawn change detected. Resetting spawn cycle to match new location.");

        // Reset the spawn cycle based on mode
        spiralManager.resetSpiralCycle();
        squareManager.resetSquareCycle();

        // Cancel and reschedule the tick-based updates from the new spawn
        Bukkit.getScheduler().cancelTasks(this);

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

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§e/forcespawnmove§7 - force a spawn move immediately");
            sender.sendMessage("§e/forcespawnmove reset§7 - reset spawn cycle to center");
            sender.sendMessage("§e/checkspawn§7 - view current spawn coordinates");
            sender.sendMessage("§e/reloadcenter§7 - reload spawn center from config");
            return true;
        }

        if ("forcespawnmove".equalsIgnoreCase(command.getName())) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
                spiralManager.resetSpiralCycle();
                squareManager.resetSquareCycle();

                // Move spawn to center (first position in the cycle)
                moveSpawn(world, "manual reset");

                sender.sendMessage("§cSpawn cycle reset and moved to center.");
                return true;
            }

            moveSpawn(world, "manual command");
            sender.sendMessage("§aSpawn updated using §e" + mode + "§a mode!");
            return true;
        }

        if ("checkspawn".equalsIgnoreCase(command.getName())) {
            int x = world.getSpawnLocation().getBlockX();
            int y = world.getSpawnLocation().getBlockY();
            int z = world.getSpawnLocation().getBlockZ();
            sender.sendMessage("§aCurrent world spawn coordinates: §e" + x + ", " + y + ", " + z);
            return true;
        }

        if ("reloadcenter".equalsIgnoreCase(command.getName())) {
            centerX = getConfig().getInt("spawn_center.x", 0);
            centerZ = getConfig().getInt("spawn_center.z", 0);

            // Reinitialize managers with new center values
            spiralManager = new SpiralSpawnManager(this, centerX, centerZ);
            squareManager = new SquareSpawnManager(this, centerX, centerZ);

            // Load their last known state
            spiralManager.loadCurrentSpawnPosition();
            squareManager.loadCurrentSpawnPosition();

            sender.sendMessage("§aCenter reloaded to §eX=" + centerX + ", Z=" + centerZ);
            return true;
        }

        return false;
    }

}