package fr.snoof.jobs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataFolder;
    private final HytaleLogger logger;
    private Config config;
    private Messages messages;

    public ConfigManager(Path dataFolder, HytaleLogger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void load() {
        try {
            Files.createDirectories(dataFolder);
            loadConfig();
            loadMessages();
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to load configuration");
        }
    }

    private void loadConfig() throws IOException {
        Path configFile = dataFolder.resolve("config.json");
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                config = GSON.fromJson(reader, Config.class);
            }
        } else {
            config = createDefaultConfig();
            saveConfig();
        }
    }

    private void loadMessages() throws IOException {
        Path messagesFile = dataFolder.resolve("messages.json");
        if (Files.exists(messagesFile)) {
            try (Reader reader = Files.newBufferedReader(messagesFile)) {
                messages = GSON.fromJson(reader, Messages.class);
            }
        } else {
            messages = new Messages();
            saveMessages();
        }
    }

    public void save() {
        try {
            saveConfig();
            saveMessages();
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to save configuration");
        }
    }

    private void saveConfig() throws IOException {
        Path configFile = dataFolder.resolve("config.json");
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        }
    }

    private void saveMessages() throws IOException {
        Path messagesFile = dataFolder.resolve("messages.json");
        try (Writer writer = Files.newBufferedWriter(messagesFile)) {
            GSON.toJson(messages, writer);
        }
    }

    private Config createDefaultConfig() {
        Config cfg = new Config();
        cfg.version = "1.0.0";
        cfg.xpFormula = "level * 100";
        cfg.maxLevel = 100;
        cfg.showRewardMessages = true;
        cfg.autoSaveInterval = 300;

        // Block rewards (MINER)
        cfg.blockRewards = new HashMap<>();
        cfg.blockRewards.put("stone", new JobReward(5, 0.5));
        cfg.blockRewards.put("coal_ore", new JobReward(10, 1.0));
        cfg.blockRewards.put("iron_ore", new JobReward(15, 2.0));
        cfg.blockRewards.put("gold_ore", new JobReward(25, 5.0));
        cfg.blockRewards.put("diamond_ore", new JobReward(50, 15.0));
        cfg.blockRewards.put("emerald_ore", new JobReward(75, 25.0));

        // Wood rewards (LUMBERJACK)
        cfg.woodRewards = new HashMap<>();
        cfg.woodRewards.put("oak_log", new JobReward(8, 0.8));
        cfg.woodRewards.put("birch_log", new JobReward(8, 0.8));
        cfg.woodRewards.put("spruce_log", new JobReward(8, 0.8));
        cfg.woodRewards.put("dark_oak_log", new JobReward(12, 1.2));
        cfg.woodRewards.put("jungle_log", new JobReward(10, 1.0));

        // Crop rewards (FARMER)
        cfg.cropRewards = new HashMap<>();
        cfg.cropRewards.put("wheat", new JobReward(5, 0.3));
        cfg.cropRewards.put("carrot", new JobReward(5, 0.3));
        cfg.cropRewards.put("potato", new JobReward(5, 0.3));
        cfg.cropRewards.put("beetroot", new JobReward(6, 0.4));
        cfg.cropRewards.put("melon", new JobReward(8, 0.5));
        cfg.cropRewards.put("pumpkin", new JobReward(8, 0.5));

        // Mob rewards (HUNTER)
        cfg.mobRewards = new HashMap<>();
        cfg.mobRewards.put("pig", new JobReward(10, 1.0));
        cfg.mobRewards.put("cow", new JobReward(10, 1.0));
        cfg.mobRewards.put("sheep", new JobReward(10, 1.0));
        cfg.mobRewards.put("chicken", new JobReward(5, 0.5));
        cfg.mobRewards.put("rabbit", new JobReward(8, 0.8));
        cfg.mobRewards.put("wolf", new JobReward(15, 2.0));
        cfg.mobRewards.put("zombie", new JobReward(12, 1.5));
        cfg.mobRewards.put("skeleton", new JobReward(12, 1.5));
        cfg.mobRewards.put("spider", new JobReward(10, 1.2));
        cfg.mobRewards.put("creeper", new JobReward(20, 3.0));

        // Boss/Legend mob rewards (CHAMPION)
        cfg.legendMobRewards = new HashMap<>();
        cfg.legendMobRewards.put("ender_dragon", new JobReward(1000, 500.0));
        cfg.legendMobRewards.put("wither", new JobReward(750, 350.0));
        cfg.legendMobRewards.put("elder_guardian", new JobReward(300, 100.0));
        cfg.legendMobRewards.put("warden", new JobReward(500, 200.0));

        // PvP kill reward (CHAMPION)
        cfg.pvpKillXp = 100;
        cfg.pvpKillMoney = 25.0;

        // Craft rewards (BLACKSMITH)
        cfg.craftRewards = new HashMap<>();
        cfg.craftRewards.put("iron_sword", new JobReward(20, 3.0));
        cfg.craftRewards.put("iron_pickaxe", new JobReward(25, 4.0));
        cfg.craftRewards.put("iron_axe", new JobReward(25, 4.0));
        cfg.craftRewards.put("iron_chestplate", new JobReward(50, 10.0));
        cfg.craftRewards.put("diamond_sword", new JobReward(50, 15.0));
        cfg.craftRewards.put("diamond_pickaxe", new JobReward(60, 20.0));
        cfg.craftRewards.put("diamond_chestplate", new JobReward(100, 50.0));

        // Furniture rewards (LUMBERJACK crafting)
        cfg.furnitureRewards = new HashMap<>();
        cfg.furnitureRewards.put("crafting_table", new JobReward(10, 1.0));
        cfg.furnitureRewards.put("chest", new JobReward(15, 2.0));
        cfg.furnitureRewards.put("bookshelf", new JobReward(25, 5.0));
        cfg.furnitureRewards.put("bed", new JobReward(20, 3.0));

        return cfg;
    }

    public Config getConfig() {
        return config;
    }

    public Messages getMessages() {
        return messages;
    }

    public long getXpRequired(int level) {
        // Simple formula: level * 100
        return (long) level * config.maxLevel;
    }

    public JobReward getBlockReward(String blockId) {
        return config.blockRewards.getOrDefault(blockId.toLowerCase(), null);
    }

    public JobReward getWoodReward(String blockId) {
        return config.woodRewards.getOrDefault(blockId.toLowerCase(), null);
    }

    public JobReward getCropReward(String blockId) {
        return config.cropRewards.getOrDefault(blockId.toLowerCase(), null);
    }

    public JobReward getMobReward(String mobId) {
        return config.mobRewards.getOrDefault(mobId.toLowerCase(), null);
    }

    public JobReward getLegendMobReward(String mobId) {
        return config.legendMobRewards.getOrDefault(mobId.toLowerCase(), null);
    }

    public JobReward getCraftReward(String itemId) {
        return config.craftRewards.getOrDefault(itemId.toLowerCase(), null);
    }

    public JobReward getFurnitureReward(String itemId) {
        return config.furnitureRewards.getOrDefault(itemId.toLowerCase(), null);
    }

    public static class Config {
        public String version = "1.0.0";
        public String xpFormula = "level * 100";
        public int maxLevel = 100;
        public int maxJobs = 3; // Max jobs a player can have at once
        public boolean showRewardMessages = true;
        public int autoSaveInterval = 300;
        public long pvpKillXp = 100;
        public double pvpKillMoney = 25.0;

        public Map<String, JobReward> blockRewards = new HashMap<>();
        public Map<String, JobReward> woodRewards = new HashMap<>();
        public Map<String, JobReward> cropRewards = new HashMap<>();
        public Map<String, JobReward> mobRewards = new HashMap<>();
        public Map<String, JobReward> legendMobRewards = new HashMap<>();
        public Map<String, JobReward> craftRewards = new HashMap<>();
        public Map<String, JobReward> furnitureRewards = new HashMap<>();
    }

    public static class Messages {
        public String noPermission = "Vous n'avez pas la permission d'exécuter cette commande.";
        public String playerNotFound = "Joueur non trouvé: %s";
        public String invalidJob = "Métier invalide: %s";
        public String invalidNumber = "Nombre invalide: %s";

        public String jobInfo = "§6=== Vos Métiers ===";
        public String jobEntry = "  %s §7- Niveau §e%d §7(§b%d§7/§b%d §7XP)";

        public String levelUp = "§a✦ Félicitations! Vous avez atteint le niveau §e%d §aen §6%s§a!";
        public String xpGained = "§7+%d XP §8(%s)";
        public String moneyGained = "§a+%s";

        public String statsHeader = "§6=== Statistiques %s ===";
        public String statsLevel = "§7Niveau: §e%d";
        public String statsXp = "§7XP: §b%d§7/§b%d";
        public String statsTotal = "§7XP Total: §b%d";
        public String statsProgress = "§7Progression: §a%.1f%%";

        public String topHeader = "§6=== Top %s ===";
        public String topEntry = "§e%d. §f%s §7- Niveau §e%d";

        public String adminSetLevel = "§aNiveau de %s en %s défini à %d.";
        public String adminSetXp = "§aXP de %s en %s défini à %d.";
        public String adminAddXp = "§aAjouté %d XP à %s en %s.";
        public String adminReset = "§aMétier(s) de %s réinitialisé(s).";
        public String adminReload = "§aConfiguration rechargée.";
        public String adminInfo = "§6=== Stats de %s ===";
        // Job join/leave messages
        public String jobJoined = "§aVous avez rejoint le métier §e%s§a!";
        public String jobLeft = "§eVous avez quitté le métier §6%s§e.";
        public String jobAlreadyJoined = "§cVous avez déjà rejoint ce métier.";
        public String jobNotJoined = "§cVous n'avez pas rejoint ce métier.";
        public String jobLimitReached = "§cVous avez atteint la limite de %d métiers. Quittez un métier avec §e/job leave <métier>§c.";
        public String noJobsJoined = "§7Vous n'avez rejoint aucun métier.";
        public String yourJobs = "§6=== Vos Métiers Actifs ===";
    }
}
