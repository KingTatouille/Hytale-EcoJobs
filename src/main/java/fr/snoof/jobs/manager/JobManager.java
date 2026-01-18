package fr.snoof.jobs.manager;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.hook.EconomyHook;
import fr.snoof.jobs.model.JobData;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JobManager {
    private final Map<UUID, JobPlayer> players = new ConcurrentHashMap<>();
    private final ConfigManager configManager;

    public JobManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public JobPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public JobPlayer getOrCreatePlayer(UUID uuid, String name) {
        return players.computeIfAbsent(uuid, k -> new JobPlayer(uuid, name));
    }

    public void loadPlayers(Map<UUID, JobPlayer> loaded) {
        players.clear();
        players.putAll(loaded);
    }

    public Map<UUID, JobPlayer> getAllPlayers() {
        return new HashMap<>(players);
    }

    public int getLevel(UUID uuid, JobType type) {
        JobPlayer player = players.get(uuid);
        return player != null ? player.getLevel(type) : 1;
    }

    public long getExperience(UUID uuid, JobType type) {
        JobPlayer player = players.get(uuid);
        return player != null ? player.getExperience(type) : 0;
    }

    public long getXpRequired(int level) {
        // Formula: level * 100
        return (long) level * 100;
    }

    public boolean addXp(UUID uuid, String playerName, JobType type, long xp, PlayerRef playerRef) {
        JobPlayer player = getOrCreatePlayer(uuid, playerName);
        player.addExperience(type, xp);
        player.updateLastSeen();

        return checkLevelUp(player, type, playerRef);
    }

    public void giveReward(UUID uuid, String playerName, JobType type, JobReward reward, PlayerRef playerRef) {
        if (reward == null)
            return;

        // Check if player has joined this job
        JobPlayer player = getOrCreatePlayer(uuid, playerName);
        if (!player.hasJoinedJob(type)) {
            // Player hasn't joined this job, no reward
            return;
        }

        // Add XP
        boolean leveledUp = addXp(uuid, playerName, type, reward.getXp(), playerRef);

        // Add money
        if (reward.getMoney() > 0) {
            EconomyHook.addBalance(uuid, reward.getMoney());
        }

        // Show message if enabled
        if (configManager.getConfig().showRewardMessages && playerRef != null && !leveledUp) {
            if (reward.getXp() > 0) {
                playerRef.sendMessage(MessageUtil.raw(
                        String.format(configManager.getMessages().xpGained, reward.getXp(), type.getDisplayName()),
                        "#7f8c8d"));
            }
            if (reward.getMoney() > 0) {
                playerRef.sendMessage(MessageUtil.raw(
                        String.format(configManager.getMessages().moneyGained,
                                EconomyHook.formatAmount(reward.getMoney())),
                        SUCCESS_COLOR));
            }
        }
    }

    private static final String SUCCESS_COLOR = "#2ecc71";

    private boolean checkLevelUp(JobPlayer player, JobType type, PlayerRef playerRef) {
        JobData data = player.getJobData(type);
        int currentLevel = data.getLevel();
        long currentXp = data.getExperience();
        long required = getXpRequired(currentLevel);

        int maxLevel = configManager.getConfig().maxLevel;
        boolean leveledUp = false;

        while (currentXp >= required && currentLevel < maxLevel) {
            currentXp -= required;
            currentLevel++;
            required = getXpRequired(currentLevel);
            leveledUp = true;
        }

        if (leveledUp) {
            data.setLevel(currentLevel);
            data.setExperience(currentXp);

            if (playerRef != null) {
                playerRef.sendMessage(MessageUtil.success(String.format(
                        configManager.getMessages().levelUp, currentLevel, type.getDisplayName())));
            }
        }

        return leveledUp;
    }

    public void setLevel(UUID uuid, String name, JobType type, int level) {
        JobPlayer player = getOrCreatePlayer(uuid, name);
        player.setLevel(type, Math.max(1, Math.min(level, configManager.getConfig().maxLevel)));
        player.setExperience(type, 0);
    }

    public void setExperience(UUID uuid, String name, JobType type, long xp) {
        JobPlayer player = getOrCreatePlayer(uuid, name);
        player.setExperience(type, Math.max(0, xp));
    }

    public void resetPlayer(UUID uuid, JobType type) {
        JobPlayer player = players.get(uuid);
        if (player != null) {
            if (type != null) {
                player.resetJob(type);
            } else {
                player.resetAllJobs();
            }
        }
    }

    public List<JobPlayer> getTopPlayers(JobType type, int limit) {
        return players.values().stream()
                .sorted((a, b) -> {
                    int levelCompare = Integer.compare(b.getLevel(type), a.getLevel(type));
                    if (levelCompare != 0)
                        return levelCompare;
                    return Long.compare(b.getTotalExperience(type), a.getTotalExperience(type));
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public double getProgressPercent(UUID uuid, JobType type) {
        JobPlayer player = players.get(uuid);
        if (player == null)
            return 0;

        int level = player.getLevel(type);
        if (level >= configManager.getConfig().maxLevel)
            return 100.0;

        long xp = player.getExperience(type);
        long required = getXpRequired(level);

        return (xp * 100.0) / required;
    }
}
