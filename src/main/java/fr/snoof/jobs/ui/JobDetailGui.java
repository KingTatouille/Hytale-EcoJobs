package fr.snoof.jobs.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.EcoJobsPlugin;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobData;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

/**
 * Job Detail GUI - Shows detailed information about a specific job including
 * rewards, progression milestones, and actions.
 */
public class JobDetailGui extends InteractiveCustomUIPage<JobDetailGui.DetailGuiData> {

    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final UUID playerUuid;
    private final String playerName;
    private final JobType jobType;

    public JobDetailGui(@Nonnull PlayerRef playerRef, @Nonnull JobType jobType) {
        super(playerRef, CustomPageLifetime.CanDismiss, DetailGuiData.CODEC);
        this.jobManager = EcoJobsPlugin.getInstance().getJobManager();
        this.configManager = EcoJobsPlugin.getInstance().getConfigManager();
        this.playerUuid = playerRef.getUuid();
        this.playerName = playerRef.getUsername();
        this.jobType = jobType;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

        JobPlayer player = jobManager.getOrCreatePlayer(playerUuid, playerName);
        JobData data = player.getJobData(jobType);
        boolean isJoined = player.hasJoinedJob(jobType);

        int level = data.getLevel();
        long xp = data.getExperience();
        long required = jobManager.getXpRequired(level);
        long totalXp = data.getTotalExperience();
        double progress = (xp * 100.0) / required;
        int maxLevel = configManager.getConfig().maxLevel;

        // Main container
        uiBuilder.appendInline("", "Group #JobDetailContainer { " +
                "Anchor: (Fill); " +
                "BackgroundColor: #1a1a2e; " +
                "Padding: 25; " +
                "}");

        // Header section
        buildHeader(uiBuilder, eventBuilder, isJoined);

        // Stats section
        buildStatsSection(uiBuilder, level, xp, required, totalXp, progress, maxLevel);

        // Progress milestones
        buildMilestones(uiBuilder, level);

        // Rewards section
        buildRewardsSection(uiBuilder);

        // Action buttons
        buildActions(uiBuilder, eventBuilder, isJoined);
    }

    private void buildHeader(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder,
            boolean isJoined) {
        // Header bar
        uiBuilder.appendInline("#JobDetailContainer", "Group #Header { " +
                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 100); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 20; " +
                "}");

        // Job icon
        uiBuilder.appendInline("#Header", "Group #JobIcon { " +
                "Anchor: (Left: 10, CenterY); " +
                "Dimensions: (Width: 60, Height: 60); " +
                "BackgroundColor: " + jobType.getColor() + "; " +
                "RoundedCorners: 30; " +
                "}");

        // Job name
        uiBuilder.appendInline("#Header", "Text #JobTitle { " +
                "Anchor: (Left: 90, Top: 15); " +
                "Text: '" + jobType.getDisplayName() + "'; " +
                "FontSize: 28; " +
                "FontWeight: Bold; " +
                "Color: " + jobType.getColor() + "; " +
                "}");

        // Job description
        uiBuilder.appendInline("#Header", "Text #JobDesc { " +
                "Anchor: (Left: 90, Top: 50); " +
                "Text: '" + jobType.getDescription() + "'; " +
                "FontSize: 14; " +
                "Color: #a0a0a0; " +
                "}");

        // Status
        String statusText = isJoined ? "✓ Métier actif" : "○ Non rejoint";
        String statusColor = isJoined ? "#2ecc71" : "#e74c3c";
        uiBuilder.appendInline("#Header", "Text #Status { " +
                "Anchor: (Right: 20, CenterY); " +
                "Text: '" + statusText + "'; " +
                "FontSize: 16; " +
                "FontWeight: Bold; " +
                "Color: " + statusColor + "; " +
                "BackgroundColor: #00000040; " +
                "Padding: 8 16; " +
                "RoundedCorners: 15; " +
                "}");

        // Back button
        uiBuilder.appendInline("#Header", "Button #BackBtn { " +
                "Anchor: (Right: 200, CenterY); " +
                "Dimensions: (Width: 40, Height: 40); " +
                "Text: '←'; " +
                "FontSize: 24; " +
                "BackgroundColor: #333355; " +
                "HoverColor: #4a4a6a; " +
                "RoundedCorners: 20; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Action", "back"));
    }

    private void buildStatsSection(@Nonnull UICommandBuilder uiBuilder, int level, long xp,
            long required, long totalXp, double progress, int maxLevel) {
        // Stats container
        uiBuilder.appendInline("#JobDetailContainer", "Group #StatsSection { " +
                "Anchor: (Top: 120, Left: 0, Right: 0); " +
                "Dimensions: (Height: 180); " +
                "BackgroundColor: #0f3460; " +
                "RoundedCorners: 12; " +
                "Padding: 20; " +
                "}");

        // Section title
        uiBuilder.appendInline("#StatsSection", "Text #StatsTitle { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '📊 Statistiques'; " +
                "FontSize: 18; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Level display (big)
        uiBuilder.appendInline("#StatsSection", "Group #LevelBox { " +
                "Anchor: (Top: 40, Left: 0); " +
                "Dimensions: (Width: 150, Height: 100); " +
                "BackgroundColor: " + jobType.getColor() + "40; " +
                "RoundedCorners: 12; " +
                "Padding: 10; " +
                "}");

        uiBuilder.appendInline("#LevelBox", "Text #LevelLabel { " +
                "Anchor: (Top: 5, CenterX); " +
                "Text: 'NIVEAU'; " +
                "FontSize: 12; " +
                "Color: #a0a0a0; " +
                "}");

        uiBuilder.appendInline("#LevelBox", "Text #LevelValue { " +
                "Anchor: (Center); " +
                "Text: '" + level + "'; " +
                "FontSize: 48; " +
                "FontWeight: Bold; " +
                "Color: " + jobType.getColor() + "; " +
                "}");

        uiBuilder.appendInline("#LevelBox", "Text #MaxLevel { " +
                "Anchor: (Bottom: 5, CenterX); " +
                "Text: '/ " + maxLevel + "'; " +
                "FontSize: 14; " +
                "Color: #666666; " +
                "}");

        // XP Progress bar
        uiBuilder.appendInline("#StatsSection", "Group #XpSection { " +
                "Anchor: (Top: 40, Left: 170, Right: 0); " +
                "Dimensions: (Height: 100); " +
                "}");

        uiBuilder.appendInline("#XpSection", "Text #XpLabel { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: 'Expérience'; " +
                "FontSize: 14; " +
                "Color: #a0a0a0; " +
                "}");

        uiBuilder.appendInline("#XpSection", "Text #XpValue { " +
                "Anchor: (Top: 0, Right: 0); " +
                "Text: '" + xp + " / " + required + " XP'; " +
                "FontSize: 14; " +
                "Color: #ffffff; " +
                "}");

        // Progress bar
        uiBuilder.appendInline("#XpSection", "Group #ProgressBarBg { " +
                "Anchor: (Top: 25, Left: 0, Right: 0); " +
                "Dimensions: (Height: 25); " +
                "BackgroundColor: #1a1a2e; " +
                "RoundedCorners: 12; " +
                "}");

        String fillWidth = String.format("%.1f%%", Math.min(100, progress));
        uiBuilder.appendInline("#ProgressBarBg", "Group #ProgressBarFill { " +
                "Anchor: (Left: 0, Top: 0, Bottom: 0); " +
                "Dimensions: (Width: " + fillWidth + "); " +
                "BackgroundColor: " + jobType.getColor() + "; " +
                "RoundedCorners: 12; " +
                "}");

        uiBuilder.appendInline("#ProgressBarBg", "Text #ProgressPercent { " +
                "Anchor: (Center); " +
                "Text: '" + String.format("%.1f", progress) + "%'; " +
                "FontSize: 14; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Total XP
        uiBuilder.appendInline("#XpSection", "Text #TotalXp { " +
                "Anchor: (Top: 60, Left: 0); " +
                "Text: 'XP Total: " + formatNumber(totalXp) + "'; " +
                "FontSize: 14; " +
                "Color: #888888; " +
                "}");

        // XP to next level
        long xpToNextLevel = required - xp;
        uiBuilder.appendInline("#XpSection", "Text #XpToNext { " +
                "Anchor: (Top: 60, Right: 0); " +
                "Text: '" + formatNumber(xpToNextLevel) + " XP pour niveau suivant'; " +
                "FontSize: 14; " +
                "Color: " + jobType.getColor() + "; " +
                "}");
    }

    private void buildMilestones(@Nonnull UICommandBuilder uiBuilder, int currentLevel) {
        // Milestones container
        uiBuilder.appendInline("#JobDetailContainer", "Group #MilestonesSection { " +
                "Anchor: (Top: 320, Left: 0); " +
                "Dimensions: (Width: 300, Height: 200); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 15; " +
                "}");

        uiBuilder.appendInline("#MilestonesSection", "Text #MilestoneTitle { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '🎯 Jalons'; " +
                "FontSize: 16; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Milestones list
        int[] milestones = { 5, 10, 25, 50, 75, 100 };
        int yOffset = 35;

        for (int milestone : milestones) {
            boolean achieved = currentLevel >= milestone;
            String checkmark = achieved ? "✓" : "○";
            String color = achieved ? "#2ecc71" : "#666666";
            String bgColor = achieved ? "#2ecc7120" : "#00000020";

            String reward = switch (milestone) {
                case 5 -> "+5% d'XP";
                case 10 -> "Déblocage Tier 2";
                case 25 -> "+10% Argent";
                case 50 -> "Déblocage Tier 3";
                case 75 -> "+15% d'XP";
                case 100 -> "Maître du métier!";
                default -> "";
            };

            uiBuilder.appendInline("#MilestonesSection", "Group #Milestone" + milestone + " { " +
                    "Anchor: (Top: " + yOffset + ", Left: 0, Right: 0); " +
                    "Dimensions: (Height: 24); " +
                    "BackgroundColor: " + bgColor + "; " +
                    "RoundedCorners: 6; " +
                    "Padding: 4; " +
                    "}");

            uiBuilder.appendInline("#Milestone" + milestone, "Text { " +
                    "Anchor: (Left: 5, CenterY); " +
                    "Text: '" + checkmark + " Niveau " + milestone + " - " + reward + "'; " +
                    "FontSize: 11; " +
                    "Color: " + color + "; " +
                    "}");

            yOffset += 28;
        }
    }

    private void buildRewardsSection(@Nonnull UICommandBuilder uiBuilder) {
        // Rewards container
        uiBuilder.appendInline("#JobDetailContainer", "Group #RewardsSection { " +
                "Anchor: (Top: 320, Left: 320, Right: 0); " +
                "Dimensions: (Height: 200); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 15; " +
                "Overflow: Scroll; " +
                "}");

        uiBuilder.appendInline("#RewardsSection", "Text #RewardTitle { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '💰 Récompenses'; " +
                "FontSize: 16; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Get rewards based on job type
        Map<String, JobReward> rewards = getRewardsForJob(jobType);

        int yOffset = 35;
        int count = 0;

        for (Map.Entry<String, JobReward> entry : rewards.entrySet()) {
            if (count >= 6)
                break; // Limit displayed rewards

            String itemName = formatItemName(entry.getKey());
            JobReward reward = entry.getValue();

            uiBuilder.appendInline("#RewardsSection", "Group #Reward" + count + " { " +
                    "Anchor: (Top: " + yOffset + ", Left: 0, Right: 0); " +
                    "Dimensions: (Height: 24); " +
                    "BackgroundColor: #00000020; " +
                    "RoundedCorners: 6; " +
                    "Padding: 4; " +
                    "}");

            uiBuilder.appendInline("#Reward" + count, "Text { " +
                    "Anchor: (Left: 5, CenterY); " +
                    "Text: '" + itemName + "'; " +
                    "FontSize: 11; " +
                    "Color: #cccccc; " +
                    "}");

            uiBuilder.appendInline("#Reward" + count, "Text { " +
                    "Anchor: (Right: 5, CenterY); " +
                    "Text: '+" + reward.getXp() + " XP | +$" + String.format("%.2f", reward.getMoney()) + "'; " +
                    "FontSize: 11; " +
                    "Color: " + jobType.getColor() + "; " +
                    "}");

            yOffset += 28;
            count++;
        }

        if (rewards.isEmpty()) {
            uiBuilder.appendInline("#RewardsSection", "Text #NoRewards { " +
                    "Anchor: (Top: 60, CenterX); " +
                    "Text: 'Aucune récompense configurée'; " +
                    "FontSize: 12; " +
                    "Color: #666666; " +
                    "}");
        }
    }

    private void buildActions(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder,
            boolean isJoined) {
        // Action bar
        uiBuilder.appendInline("#JobDetailContainer", "Group #ActionBar { " +
                "Anchor: (Bottom: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 60); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 10; " +
                "}");

        if (isJoined) {
            // Leave button
            uiBuilder.appendInline("#ActionBar", "Button #LeaveBtn { " +
                    "Anchor: (Left: 20, CenterY); " +
                    "Dimensions: (Width: 180, Height: 40); " +
                    "Text: '✕ Quitter ce métier'; " +
                    "FontSize: 14; " +
                    "BackgroundColor: #c0392b; " +
                    "HoverColor: #e74c3c; " +
                    "RoundedCorners: 10; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LeaveBtn",
                    EventData.of("Action", "leave"));
        } else {
            // Join button
            uiBuilder.appendInline("#ActionBar", "Button #JoinBtn { " +
                    "Anchor: (Left: 20, CenterY); " +
                    "Dimensions: (Width: 180, Height: 40); " +
                    "Text: '✓ Rejoindre ce métier'; " +
                    "FontSize: 14; " +
                    "BackgroundColor: #27ae60; " +
                    "HoverColor: #2ecc71; " +
                    "RoundedCorners: 10; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#JoinBtn",
                    EventData.of("Action", "join"));
        }

        // Close button
        uiBuilder.appendInline("#ActionBar", "Button #CloseBtn { " +
                "Anchor: (Right: 20, CenterY); " +
                "Dimensions: (Width: 120, Height: 40); " +
                "Text: 'Fermer'; " +
                "FontSize: 14; " +
                "BackgroundColor: #333355; " +
                "HoverColor: #4a4a6a; " +
                "RoundedCorners: 10; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
                EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull DetailGuiData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null || data.action.isEmpty()) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        JobPlayer jobPlayer = jobManager.getOrCreatePlayer(playerUuid, playerName);

        switch (data.action) {
            case "back" -> {
                player.getPageManager().openCustomPage(ref, store, new JobsMainGui(playerRef));
            }
            case "close" -> {
                this.close();
            }
            case "join" -> {
                if (!jobPlayer.hasJoinedJob(jobType)) {
                    int maxJobs = configManager.getConfig().maxJobs;
                    if (jobPlayer.getJoinedJobCount() < maxJobs) {
                        jobPlayer.joinJob(jobType);
                        playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                "§a✓ Vous avez rejoint le métier " + jobType.getDisplayName() + "!"));
                        this.rebuild();
                    } else {
                        playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                "§c✕ Limite de " + maxJobs + " métiers atteinte!"));
                    }
                }
            }
            case "leave" -> {
                if (jobPlayer.hasJoinedJob(jobType)) {
                    jobPlayer.leaveJob(jobType);
                    playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                            "§e○ Vous avez quitté le métier " + jobType.getDisplayName()));
                    this.rebuild();
                }
            }
        }
    }

    private Map<String, JobReward> getRewardsForJob(JobType type) {
        ConfigManager.Config config = configManager.getConfig();
        return switch (type) {
            case MINER -> config.blockRewards;
            case LUMBERJACK -> config.woodRewards;
            case FARMER -> config.cropRewards;
            case HUNTER -> config.mobRewards;
            case CHAMPION -> config.legendMobRewards;
            case BLACKSMITH -> config.craftRewards;
        };
    }

    private String formatItemName(String rawName) {
        return rawName.replace("_", " ")
                .substring(0, 1).toUpperCase() +
                rawName.replace("_", " ").substring(1);
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public static class DetailGuiData {
        static final String KEY_ACTION = "Action";

        public static final BuilderCodec<DetailGuiData> CODEC = BuilderCodec.<DetailGuiData>builder(
                DetailGuiData.class, DetailGuiData::new)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        (d, s) -> d.action = s, d -> d.action)
                .build();

        private String action;
    }
}
