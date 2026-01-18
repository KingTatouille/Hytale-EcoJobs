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
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Job Leaderboard GUI - Shows top players for each job
 */
public class JobLeaderboardGui extends InteractiveCustomUIPage<JobLeaderboardGui.LeaderboardGuiData> {

    private final JobManager jobManager;
    private final UUID playerUuid;
    private final String playerName;
    private JobType selectedJob;

    public JobLeaderboardGui(@Nonnull PlayerRef playerRef) {
        this(playerRef, JobType.MINER);
    }

    public JobLeaderboardGui(@Nonnull PlayerRef playerRef, @Nonnull JobType initialJob) {
        super(playerRef, CustomPageLifetime.CanDismiss, LeaderboardGuiData.CODEC);
        this.jobManager = EcoJobsPlugin.getInstance().getJobManager();
        this.playerUuid = playerRef.getUuid();
        this.playerName = playerRef.getUsername();
        this.selectedJob = initialJob;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

        // Main container
        uiBuilder.appendInline("", "Group #LeaderboardContainer { " +
                "Anchor: (Fill); " +
                "BackgroundColor: #1a1a2e; " +
                "Padding: 25; " +
                "}");

        // Header
        buildHeader(uiBuilder, eventBuilder);

        // Job selector tabs
        buildJobTabs(uiBuilder, eventBuilder);

        // Leaderboard content
        buildLeaderboard(uiBuilder);

        // Footer
        buildFooter(uiBuilder, eventBuilder);
    }

    private void buildHeader(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Header bar
        uiBuilder.appendInline("#LeaderboardContainer", "Group #Header { " +
                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 80); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 15; " +
                "}");

        // Back button
        uiBuilder.appendInline("#Header", "Button #BackBtn { " +
                "Anchor: (Left: 10, CenterY); " +
                "Dimensions: (Width: 40, Height: 40); " +
                "Text: '←'; " +
                "FontSize: 24; " +
                "BackgroundColor: #333355; " +
                "HoverColor: #4a4a6a; " +
                "RoundedCorners: 20; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Action", "back"));

        // Title with trophy icon
        uiBuilder.appendInline("#Header", "Text #Title { " +
                "Anchor: (Left: 70, CenterY); " +
                "Text: '🏆 Classement des Métiers'; " +
                "FontSize: 28; " +
                "FontWeight: Bold; " +
                "Color: #f9a825; " +
                "}");

        // Current player rank display
        JobPlayer currentPlayer = jobManager.getPlayer(playerUuid);
        int rank = getPlayerRank(selectedJob);
        String rankText = rank > 0 ? "#" + rank : "Non classé";

        uiBuilder.appendInline("#Header", "Text #YourRank { " +
                "Anchor: (Right: 20, CenterY); " +
                "Text: 'Votre rang: " + rankText + "'; " +
                "FontSize: 16; " +
                "Color: #ffffff; " +
                "BackgroundColor: " + selectedJob.getColor() + "; " +
                "Padding: 8 16; " +
                "RoundedCorners: 15; " +
                "}");
    }

    private void buildJobTabs(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Tabs container
        uiBuilder.appendInline("#LeaderboardContainer", "Group #TabsContainer { " +
                "Anchor: (Top: 100, Left: 0, Right: 0); " +
                "Dimensions: (Height: 50); " +
                "LayoutMode: Left; " +
                "LayoutGap: 10; " +
                "}");

        int index = 0;
        for (JobType type : JobType.values()) {
            boolean isSelected = type == selectedJob;
            String bgColor = isSelected ? type.getColor() : "#333355";
            String textColor = isSelected ? "#ffffff" : "#a0a0a0";
            String hoverColor = isSelected ? type.getColor() : "#4a4a6a";

            uiBuilder.appendInline("#TabsContainer", "Button #Tab" + index + " { " +
                    "Dimensions: (Width: 140, Height: 40); " +
                    "Text: '" + type.getDisplayName() + "'; " +
                    "FontSize: 12; " +
                    "Color: " + textColor + "; " +
                    "BackgroundColor: " + bgColor + "; " +
                    "HoverColor: " + hoverColor + "; " +
                    "RoundedCorners: 10; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Tab" + index,
                    EventData.of("Action", "select:" + type.name()));

            index++;
        }
    }

    private void buildLeaderboard(@Nonnull UICommandBuilder uiBuilder) {
        // Leaderboard container
        uiBuilder.appendInline("#LeaderboardContainer", "Group #LeaderboardContent { " +
                "Anchor: (Top: 170, Left: 0, Right: 0, Bottom: 80); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 20; " +
                "Overflow: Scroll; " +
                "}");

        // Selected job header
        uiBuilder.appendInline("#LeaderboardContent", "Group #JobHeader { " +
                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 60); " +
                "BackgroundColor: " + selectedJob.getColor() + "40; " +
                "RoundedCorners: 10; " +
                "Padding: 10; " +
                "}");

        uiBuilder.appendInline("#JobHeader", "Group #JobIcon { " +
                "Anchor: (Left: 10, CenterY); " +
                "Dimensions: (Width: 40, Height: 40); " +
                "BackgroundColor: " + selectedJob.getColor() + "; " +
                "RoundedCorners: 20; " +
                "}");

        uiBuilder.appendInline("#JobHeader", "Text #JobName { " +
                "Anchor: (Left: 65, Top: 8); " +
                "Text: '" + selectedJob.getDisplayName() + "'; " +
                "FontSize: 20; " +
                "FontWeight: Bold; " +
                "Color: " + selectedJob.getColor() + "; " +
                "}");

        uiBuilder.appendInline("#JobHeader", "Text #JobDesc { " +
                "Anchor: (Left: 65, Bottom: 8); " +
                "Text: '" + selectedJob.getDescription() + "'; " +
                "FontSize: 12; " +
                "Color: #a0a0a0; " +
                "}");

        // Top players list
        List<JobPlayer> topPlayers = jobManager.getTopPlayers(selectedJob, 10);

        if (topPlayers.isEmpty()) {
            uiBuilder.appendInline("#LeaderboardContent", "Text #NoPlayers { " +
                    "Anchor: (Top: 100, CenterX); " +
                    "Text: 'Aucun joueur classé dans ce métier'; " +
                    "FontSize: 16; " +
                    "Color: #666666; " +
                    "}");
            return;
        }

        // Column headers
        uiBuilder.appendInline("#LeaderboardContent", "Group #ColumnHeaders { " +
                "Anchor: (Top: 75, Left: 0, Right: 0); " +
                "Dimensions: (Height: 30); " +
                "BackgroundColor: #0f3460; " +
                "RoundedCorners: 6; " +
                "Padding: 5; " +
                "}");

        uiBuilder.appendInline("#ColumnHeaders",
                "Text { Anchor: (Left: 20, CenterY); Text: '#'; FontSize: 12; Color: #888888; }");
        uiBuilder.appendInline("#ColumnHeaders",
                "Text { Anchor: (Left: 80, CenterY); Text: 'Joueur'; FontSize: 12; Color: #888888; }");
        uiBuilder.appendInline("#ColumnHeaders",
                "Text { Anchor: (Left: 300, CenterY); Text: 'Niveau'; FontSize: 12; Color: #888888; }");
        uiBuilder.appendInline("#ColumnHeaders",
                "Text { Anchor: (Left: 400, CenterY); Text: 'XP Total'; FontSize: 12; Color: #888888; }");
        uiBuilder.appendInline("#ColumnHeaders",
                "Text { Anchor: (Right: 40, CenterY); Text: 'Progression'; FontSize: 12; Color: #888888; }");

        // Player rows
        int yOffset = 115;
        int rank = 1;

        for (JobPlayer player : topPlayers) {
            int level = player.getLevel(selectedJob);
            long totalXp = player.getTotalExperience(selectedJob);
            long xp = player.getExperience(selectedJob);
            long required = jobManager.getXpRequired(level);
            double progress = (xp * 100.0) / required;

            boolean isCurrentPlayer = player.getUuid().equals(playerUuid);
            String rowBg = isCurrentPlayer ? selectedJob.getColor() + "30" : (rank % 2 == 0 ? "#1a1a2e" : "#0f3460");

            // Medal colors for top 3
            String medalColor = switch (rank) {
                case 1 -> "#ffd700"; // Gold
                case 2 -> "#c0c0c0"; // Silver
                case 3 -> "#cd7f32"; // Bronze
                default -> "#666666";
            };

            String rankDisplay = switch (rank) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> String.valueOf(rank);
            };

            uiBuilder.appendInline("#LeaderboardContent", "Group #PlayerRow" + rank + " { " +
                    "Anchor: (Top: " + yOffset + ", Left: 0, Right: 0); " +
                    "Dimensions: (Height: 50); " +
                    "BackgroundColor: " + rowBg + "; " +
                    "RoundedCorners: 8; " +
                    "Padding: 5; " +
                    "}");

            // Rank
            uiBuilder.appendInline("#PlayerRow" + rank, "Text #Rank" + rank + " { " +
                    "Anchor: (Left: 20, CenterY); " +
                    "Text: '" + rankDisplay + "'; " +
                    "FontSize: " + (rank <= 3 ? "20" : "16") + "; " +
                    "Color: " + medalColor + "; " +
                    "}");

            // Player name
            String playerNameDisplay = player.getName();
            if (isCurrentPlayer) {
                playerNameDisplay += " (vous)";
            }
            uiBuilder.appendInline("#PlayerRow" + rank, "Text #Name" + rank + " { " +
                    "Anchor: (Left: 80, CenterY); " +
                    "Text: '" + playerNameDisplay + "'; " +
                    "FontSize: 16; " +
                    "FontWeight: " + (isCurrentPlayer ? "Bold" : "Normal") + "; " +
                    "Color: " + (isCurrentPlayer ? selectedJob.getColor() : "#ffffff") + "; " +
                    "}");

            // Level
            uiBuilder.appendInline("#PlayerRow" + rank, "Text #Level" + rank + " { " +
                    "Anchor: (Left: 300, CenterY); " +
                    "Text: '" + level + "'; " +
                    "FontSize: 20; " +
                    "FontWeight: Bold; " +
                    "Color: " + selectedJob.getColor() + "; " +
                    "}");

            // Total XP
            uiBuilder.appendInline("#PlayerRow" + rank, "Text #TotalXp" + rank + " { " +
                    "Anchor: (Left: 400, CenterY); " +
                    "Text: '" + formatNumber(totalXp) + "'; " +
                    "FontSize: 14; " +
                    "Color: #a0a0a0; " +
                    "}");

            // Progress bar
            uiBuilder.appendInline("#PlayerRow" + rank, "Group #ProgressBg" + rank + " { " +
                    "Anchor: (Right: 20, CenterY); " +
                    "Dimensions: (Width: 120, Height: 20); " +
                    "BackgroundColor: #333355; " +
                    "RoundedCorners: 10; " +
                    "}");

            int fillWidth = (int) Math.max(2, Math.min(120 * (progress / 100.0), 120));
            uiBuilder.appendInline("#ProgressBg" + rank, "Group #ProgressFill" + rank + " { " +
                    "Anchor: (Left: 0, Top: 0, Bottom: 0); " +
                    "Dimensions: (Width: " + fillWidth + "); " +
                    "BackgroundColor: " + selectedJob.getColor() + "; " +
                    "RoundedCorners: 10; " +
                    "}");

            uiBuilder.appendInline("#ProgressBg" + rank, "Text #ProgressText" + rank + " { " +
                    "Anchor: (Center); " +
                    "Text: '" + String.format("%.0f", progress) + "%'; " +
                    "FontSize: 11; " +
                    "Color: #ffffff; " +
                    "}");

            yOffset += 55;
            rank++;
        }
    }

    private void buildFooter(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Footer bar
        uiBuilder.appendInline("#LeaderboardContainer", "Group #Footer { " +
                "Anchor: (Bottom: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 60); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 10; " +
                "}");

        // Close button
        uiBuilder.appendInline("#Footer", "Button #CloseBtn { " +
                "Anchor: (Center); " +
                "Dimensions: (Width: 150, Height: 40); " +
                "Text: '✕ Fermer'; " +
                "FontSize: 16; " +
                "BackgroundColor: #e94560; " +
                "HoverColor: #ff6b8a; " +
                "RoundedCorners: 10; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
                EventData.of("Action", "close"));

        // Info text
        uiBuilder.appendInline("#Footer", "Text #Info { " +
                "Anchor: (Left: 20, CenterY); " +
                "Text: '📊 Top 10 joueurs affichés'; " +
                "FontSize: 12; " +
                "Color: #888888; " +
                "}");

        // Refresh button
        uiBuilder.appendInline("#Footer", "Button #RefreshBtn { " +
                "Anchor: (Right: 200, CenterY); " +
                "Dimensions: (Width: 120, Height: 36); " +
                "Text: '🔄 Actualiser'; " +
                "FontSize: 13; " +
                "BackgroundColor: #3498db; " +
                "HoverColor: #5dade2; " +
                "RoundedCorners: 8; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshBtn",
                EventData.of("Action", "refresh"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull LeaderboardGuiData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null || data.action.isEmpty()) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (data.action.equals("back")) {
            player.getPageManager().openCustomPage(ref, store, new JobsMainGui(playerRef));
            return;
        }

        if (data.action.equals("close")) {
            this.close();
            return;
        }

        if (data.action.equals("refresh")) {
            this.rebuild();
            return;
        }

        if (data.action.startsWith("select:")) {
            String jobName = data.action.substring(7);
            JobType type = JobType.fromString(jobName);
            if (type != null) {
                this.selectedJob = type;
                this.rebuild();
            }
        }
    }

    private int getPlayerRank(JobType type) {
        List<JobPlayer> topPlayers = jobManager.getTopPlayers(type, 100);
        int rank = 1;
        for (JobPlayer player : topPlayers) {
            if (player.getUuid().equals(playerUuid)) {
                return rank;
            }
            rank++;
        }
        return 0; // Not ranked
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public static class LeaderboardGuiData {
        static final String KEY_ACTION = "Action";

        public static final BuilderCodec<LeaderboardGuiData> CODEC = BuilderCodec.<LeaderboardGuiData>builder(
                LeaderboardGuiData.class, LeaderboardGuiData::new)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        (d, s) -> d.action = s, d -> d.action)
                .build();

        private String action;
    }
}
