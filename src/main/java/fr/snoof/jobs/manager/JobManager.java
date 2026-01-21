package fr.snoof.jobs.manager;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.snoof.jobs.config.ConfigManager;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import fr.snoof.jobs.hook.EconomyHook;
import fr.snoof.jobs.model.Job;
import fr.snoof.jobs.model.JobData;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JobManager {
    private final Map<UUID, JobPlayer> players = new ConcurrentHashMap<>();
    private final ConfigManager configManager;
    private final List<Job> jobs;

    public JobManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.jobs = new ArrayList<>();
        initJobs();
    }

    private void initJobs() {
        jobs.add(new Job(JobType.FARMER, "🌾", 0, 100, 10));
        jobs.add(new Job(JobType.HUNTER, "🏹", 0, 100, 10));
        jobs.add(new Job(JobType.CHAMPION, "⚔️", 0, 100, 10));
        jobs.add(new Job(JobType.MINER, "⛏️", 0, 100, 10));
        jobs.add(new Job(JobType.BLACKSMITH, "🔨", 0, 100, 10));
        jobs.add(new Job(JobType.LUMBERJACK, "🪓", 0, 100, 10));
    }

    public List<Job> getAllJobs() {
        return Collections.unmodifiableList(jobs);
    }

    public Job getJob(String id) {
        return jobs.stream()
                .filter(j -> j.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public int getPlayerCountForJob(String jobId) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return 0;

        return (int) players.values().stream()
                .filter(p -> p.hasJoinedJob(type))
                .count();
    }

    public boolean assignJob(PlayerRef playerRef, String jobId) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return false;

        JobPlayer player = getOrCreatePlayer(
                playerRef.getUuid(),
                playerRef.getUsername());

        return player.joinJob(type);
    }

    public boolean removeJob(@Nonnull PlayerRef playerRef, @Nonnull String jobId) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return false;

        JobPlayer player = getPlayers().get(playerRef.getUuid());
        return player != null && player.leaveJob(type);
    }

    public JobData getPlayerJobData(@Nonnull PlayerRef playerRef, @Nonnull String jobId) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return null;

        JobPlayer player = getPlayers().get(playerRef.getUuid());
        if (player == null || !player.hasJoinedJob(type))
            return null;

        return player.getJobData(type);
    }

    public int getPlayerLevel(@Nonnull PlayerRef playerRef, @Nonnull String jobId) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return 0;

        JobPlayer player = players.get(playerRef.getUuid());
        return player != null ? player.getLevel(type) : 0;
    }

    public Map<UUID, JobPlayer> getPlayers() {
        return players;
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
        return (long) level * 100;
    }

    public int getRequiredXPForNextLevel(int currentLevel) {
        return (int) getXpRequired(currentLevel);
    }

    public boolean addXp(UUID uuid, String playerName, JobType type, long xp, PlayerRef playerRef) {
        JobPlayer player = getOrCreatePlayer(uuid, playerName);
        player.addExperience(type, xp);
        player.getJobData(type).incrementActions();
        player.getJobData(type).updateLastAction();
        player.updateLastSeen();

        return checkLevelUp(player, type, playerRef);
    }

    public void giveReward(UUID uuid, String playerName, JobType type, JobReward reward, PlayerRef playerRef) {
        if (reward == null)
            return;

        JobPlayer jobPlayer = getOrCreatePlayer(uuid, playerName);
        if (!jobPlayer.hasJoinedJob(type)) {
            return;
        }

        boolean leveledUp = addXp(uuid, playerName, type, reward.getXp(), playerRef);

        if (reward.getMoney() > 0) {
            EconomyHook.addBalance(uuid, reward.getMoney());
            jobPlayer.getJobData(type).addEarnings((long) reward.getMoney());
        }

        if (playerRef != null && !leveledUp) {
            StringBuilder message = new StringBuilder();
            if (reward.getXp() > 0) {
                message.append("+").append(reward.getXp()).append(" XP");
            }
            if (reward.getMoney() > 0) {
                if (message.length() > 0)
                    message.append(" | ");
                message.append("+").append(EconomyHook.formatAmount(reward.getMoney()));
            }

            if (message.length() > 0) {
                sendNotification(playerRef, type.getDisplayName(), message.toString(), type.getIconItem());
            }
        }
    }

    public void sendNotification(PlayerRef playerRef, String title, String message, String iconItemName) {
        try {
            var packetHandler = playerRef.getPacketHandler();
            var primaryMessage = Message.raw(title).color("#2ecc71"); // Green title
            var secondaryMessage = Message.raw(message).color("#FFFFFF");

            var icon = new ItemStack(iconItemName != null ? iconItemName : "Weapon_Sword_Wood", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    (ItemWithAllMetadata) icon);
        } catch (Exception e) {
            System.err.println("[EcoJobs] Failed to send notification: " + e.getMessage());
        }
    }

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

    public List<Records.TopPlayerEntry> getTopPlayersForJob(String jobId, int limit) {
        JobType type = JobType.fromString(jobId);
        if (type == null)
            return Collections.emptyList();

        return players.values().stream()
                .filter(p -> p.hasJoinedJob(type))
                .sorted((a, b) -> {
                    int levelCompare = Integer.compare(b.getLevel(type), a.getLevel(type));
                    if (levelCompare != 0)
                        return levelCompare;
                    return Long.compare(b.getTotalExperience(type), a.getTotalExperience(type));
                })
                .limit(limit)
                .map(p -> new Records.TopPlayerEntry(p.getName(), p.getLevel(type), p.getTotalExperience(type)))
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

    public void reload() {
        try {
            configManager.loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Records {
        public record TopPlayerEntry(String playerName, int level, long totalXP) {
        }
    }
}
