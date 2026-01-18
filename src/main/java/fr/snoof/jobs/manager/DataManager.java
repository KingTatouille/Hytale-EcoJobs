package fr.snoof.jobs.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.snoof.jobs.model.JobPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PLAYER_MAP_TYPE = new TypeToken<Map<UUID, JobPlayer>>() {
    }.getType();

    private final Path dataFolder;
    private final HytaleLogger logger;
    private final JobManager jobManager;
    private final ScheduledExecutorService scheduler;
    private boolean running;

    public DataManager(Path dataFolder, HytaleLogger logger, JobManager jobManager) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.jobManager = jobManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void init(int saveIntervalSeconds) {
        load();
        running = true;
        scheduler.scheduleAtFixedRate(this::save, saveIntervalSeconds, saveIntervalSeconds, TimeUnit.SECONDS);
        logger.at(Level.INFO).log("DataManager initialized with auto-save every " + saveIntervalSeconds + "s");
    }

    public void shutdown() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        save();
        logger.at(Level.INFO).log("DataManager shutdown complete.");
    }

    public void load() {
        try {
            Files.createDirectories(dataFolder);
            loadPlayers();
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to load data");
        }
    }

    private void loadPlayers() throws IOException {
        Path playersFile = dataFolder.resolve("players.json");
        if (Files.exists(playersFile)) {
            try (Reader reader = Files.newBufferedReader(playersFile)) {
                Map<UUID, JobPlayer> loaded = GSON.fromJson(reader, PLAYER_MAP_TYPE);
                if (loaded != null) {
                    jobManager.loadPlayers(loaded);
                    logger.at(Level.INFO).log("Loaded " + loaded.size() + " player profiles.");
                }
            }
        } else {
            logger.at(Level.INFO).log("No existing player data found, starting fresh.");
        }
    }

    public void save() {
        try {
            savePlayers();
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to save data");
        }
    }

    private void savePlayers() throws IOException {
        Path playersFile = dataFolder.resolve("players.json");
        Map<UUID, JobPlayer> players = jobManager.getAllPlayers();
        try (Writer writer = Files.newBufferedWriter(playersFile)) {
            GSON.toJson(players, writer);
        }
    }
}
