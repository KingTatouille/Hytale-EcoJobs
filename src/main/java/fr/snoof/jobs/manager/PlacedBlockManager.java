package fr.snoof.jobs.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlacedBlockManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // CHANGED: We now store Strings (data) instead of just UUID
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private final Path dataFolder;
    private final HytaleLogger logger;
    // Map<LocationKey, "UUID:BlockID">
    private final Map<String, String> placedBlocks = new ConcurrentHashMap<>();

    public PlacedBlockManager(Path dataFolder, HytaleLogger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void load() {
        Path file = dataFolder.resolve("placed_blocks.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
                if (loaded != null) {
                    placedBlocks.putAll(loaded);
                    logger.at(Level.INFO).log("Loaded " + loaded.size() + " placed blocks.");
                }
            } catch (IOException e) {
                logger.at(Level.SEVERE).withCause(e).log("Failed to load placed blocks.");
            }
        }
    }

    public void save() {
        Path file = dataFolder.resolve("placed_blocks.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(placedBlocks, writer);
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to save placed blocks.");
        }
    }

    public void addBlock(int x, int y, int z, UUID playerUuid, String blockId) {
        String key = getLocationKey(x, y, z);
        placedBlocks.put(key, playerUuid.toString() + ":" + blockId);
    }

    public void removeBlock(int x, int y, int z) {
        placedBlocks.remove(getLocationKey(x, y, z));
    }

    public boolean isPlacedByPlayer(int x, int y, int z) {
        return placedBlocks.containsKey(getLocationKey(x, y, z));
    }

    public UUID getPlacer(int x, int y, int z) {
        String data = placedBlocks.get(getLocationKey(x, y, z));
        if (data == null)
            return null;
        try {
            // Check if it's old format (just UUID) or new format
            if (!data.contains(":")) {
                return UUID.fromString(data);
            }
            return UUID.fromString(data.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public String getPlacedBlockId(int x, int y, int z) {
        String data = placedBlocks.get(getLocationKey(x, y, z));
        if (data == null || !data.contains(":"))
            return null;
        return data.split(":")[1];
    }

    private String getLocationKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
