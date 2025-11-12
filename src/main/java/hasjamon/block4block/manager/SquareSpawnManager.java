package hasjamon.block4block.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SquareSpawnManager {
    private final JavaPlugin plugin;
    private final int centerX;
    private final int centerZ;
    private int layer = 0;
    private int stepIndex = 0;
    private List<Location> currentLayerPositions = new ArrayList<>();

    public SquareSpawnManager(JavaPlugin plugin, int centerX, int centerZ) {
        this.plugin = plugin;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void loadCurrentSpawnPosition() {
        layer = plugin.getConfig().getInt("square.layer", 0);
        stepIndex = plugin.getConfig().getInt("square.stepIndex", 0);
        plugin.getLogger().info("Loaded square spawn position: Layer=" + layer + ", StepIndex=" + stepIndex);
        generateLayerPositions(); // Regenerate positions after loading
    }

    public Location updateSpawnWithSquare(World world) {
        String squareStepConfig = plugin.getConfig().getString("square_step", "chunkcenter");
        int multiplier;
        boolean chunkCenter = false;

        if (squareStepConfig.equalsIgnoreCase("chunkcenter")) {
            multiplier = 16;
            chunkCenter = true;
        } else {
            try {
                multiplier = Integer.parseInt(squareStepConfig);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid square_step. Defaulting to chunkcenter.");
                multiplier = 16;
                chunkCenter = true;
            }
        }

        if (currentLayerPositions.isEmpty()) {
            generateLayerPositions();
        }

        if (stepIndex >= currentLayerPositions.size()) {
            plugin.getLogger().warning("StepIndex out of bounds. Forcing layer increment.");
            layer++;
            stepIndex = 0;
            generateLayerPositions();
        }

        Location relLoc = currentLayerPositions.get(stepIndex);
        int x = centerX + relLoc.getBlockX() * multiplier;
        int z = centerZ + relLoc.getBlockZ() * multiplier;

        if (chunkCenter) {
            x += 8;
            z += 8;
        }

        Location newSpawn = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        world.setSpawnLocation(newSpawn.getBlockX(), newSpawn.getBlockY(), newSpawn.getBlockZ());

        plugin.getLogger().info("New spawn location: X=" + x + ", Z=" + z);
        stepIndex++;
        saveCurrentSpawnPosition();

        return newSpawn;
    }

    private void generateLayerPositions() {
        currentLayerPositions.clear();

        if (layer == 0) {
            currentLayerPositions.add(new Location(null, 0, 0, 0));
            return;
        }

        int r = layer;

        // Top edge (left to right)
        for (int x = -r + 1; x <= r; x++) {
            currentLayerPositions.add(new Location(null, x, 0, -r)); // Fixed: x, y, z order
        }

        // Right edge (top to bottom)
        for (int z = -r + 1; z <= r; z++) {
            currentLayerPositions.add(new Location(null, r, 0, z)); // Fixed: x, y, z order
        }

        // Bottom edge (right to left)
        for (int x = r - 1; x >= -r; x--) {
            currentLayerPositions.add(new Location(null, x, 0, r)); // Fixed: x, y, z order
        }

        // Left edge (bottom to top)
        for (int z = r - 1; z > -r; z--) {
            currentLayerPositions.add(new Location(null, -r, 0, z)); // Fixed: x, y, z order
        }
    }

    public void saveCurrentSpawnPosition() {
        plugin.getConfig().set("square.layer", layer);
        plugin.getConfig().set("square.stepIndex", stepIndex);
        plugin.saveConfig();
    }

    public void resetSquareCycle() {
        layer = 0;
        stepIndex = 0;
        currentLayerPositions.clear();
    }

    public int getCurrentX() {
        return 0; // No longer used, kept for compatibility
    }

    public int getCurrentZ() {
        return 0; // No longer used, kept for compatibility
    }
}
