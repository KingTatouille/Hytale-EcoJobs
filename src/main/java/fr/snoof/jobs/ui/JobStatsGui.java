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
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

/**
 * Job Stats GUI - Shows personal statistics across all jobs
 */
public class JobStatsGui extends InteractiveCustomUIPage<JobStatsGui.StatsGuiData> {

        private final JobManager jobManager;
        private final ConfigManager configManager;
        private final UUID playerUuid;
        private final String playerName;

        public JobStatsGui(@Nonnull PlayerRef playerRef) {
                super(playerRef, CustomPageLifetime.CanDismiss, StatsGuiData.CODEC);
                this.jobManager = EcoJobsPlugin.getInstance().getJobManager();
                this.configManager = EcoJobsPlugin.getInstance().getConfigManager();
                this.playerUuid = playerRef.getUuid();
                this.playerName = playerRef.getUsername();
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
                        @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

                JobPlayer player = jobManager.getOrCreatePlayer(playerUuid, playerName);

                // Main container
                uiBuilder.appendInline("", "Group #StatsContainer { " +
                                "Anchor: (Fill); " +
                                "BackgroundColor: #1a1a2e; " +
                                "Padding: 25; " +
                                "}");

                // Header
                buildHeader(uiBuilder, eventBuilder, player);

                // Summary cards
                buildSummaryCards(uiBuilder, player);

                // Job breakdown
                buildJobBreakdown(uiBuilder, eventBuilder, player);

                // Footer
                buildFooter(uiBuilder, eventBuilder);
        }

        private void buildHeader(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder,
                        JobPlayer player) {
                // Header bar
                uiBuilder.appendInline("#StatsContainer", "Group #Header { " +
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

                // Title
                uiBuilder.appendInline("#Header", "Text #Title { " +
                                "Anchor: (Left: 70, CenterY); " +
                                "Text: '📊 Mes Statistiques'; " +
                                "FontSize: 28; " +
                                "FontWeight: Bold; " +
                                "Color: #e94560; " +
                                "}");

                // Player name
                uiBuilder.appendInline("#Header", "Text #PlayerName { " +
                                "Anchor: (Right: 20, CenterY); " +
                                "Text: '" + playerName + "'; " +
                                "FontSize: 18; " +
                                "Color: #ffffff; " +
                                "BackgroundColor: #533483; " +
                                "Padding: 8 16; " +
                                "RoundedCorners: 15; " +
                                "}");
        }

        private void buildSummaryCards(@Nonnull UICommandBuilder uiBuilder, JobPlayer player) {
                // Summary container
                uiBuilder.appendInline("#StatsContainer", "Group #SummarySection { " +
                                "Anchor: (Top: 100, Left: 0, Right: 0); " +
                                "Dimensions: (Height: 120); " +
                                "LayoutMode: Left; " +
                                "LayoutGap: 20; " +
                                "}");

                // Calculate stats
                int totalLevel = player.getTotalLevel();
                long totalXp = 0;
                for (JobType type : JobType.values()) {
                        totalXp += player.getTotalExperience(type);
                }
                int activeJobs = player.getJoinedJobCount();
                int maxJobs = configManager.getConfig().maxJobs;
                int maxLevel = configManager.getConfig().maxLevel;

                // Find highest level job
                JobType highestJob = null;
                int highestLevel = 0;
                for (JobType type : JobType.values()) {
                        int level = player.getLevel(type);
                        if (level > highestLevel) {
                                highestLevel = level;
                                highestJob = type;
                        }
                }

                // Total Level Card
                buildSummaryCard(uiBuilder, "TotalLevel", "NIVEAU TOTAL",
                                String.valueOf(totalLevel), "/" + (JobType.values().length * maxLevel),
                                "#3498db", 0);

                // Total XP Card
                buildSummaryCard(uiBuilder, "TotalXp", "XP TOTAL",
                                formatNumber(totalXp), "points",
                                "#9b59b6", 1);

                // Active Jobs Card
                buildSummaryCard(uiBuilder, "ActiveJobs", "MÉTIERS ACTIFS",
                                String.valueOf(activeJobs), "/" + maxJobs,
                                "#2ecc71", 2);

                // Best Job Card
                String bestJobName = highestJob != null ? highestJob.getDisplayName() : "Aucun";
                String bestJobLevel = highestJob != null ? "Nv." + highestLevel : "-";
                String bestJobColor = highestJob != null ? highestJob.getColor() : "#666666";
                buildSummaryCard(uiBuilder, "BestJob", "MEILLEUR MÉTIER",
                                bestJobName, bestJobLevel,
                                bestJobColor, 3);
        }

        private void buildSummaryCard(@Nonnull UICommandBuilder uiBuilder, String id, String label,
                        String value, String subValue, String color, int index) {
                uiBuilder.appendInline("#SummarySection", "Group #Card" + id + " { " +
                                "Dimensions: (Width: 200, Height: 100); " +
                                "BackgroundColor: #16213e; " +
                                "BorderColor: " + color + "; " +
                                "BorderWidth: 2; " +
                                "RoundedCorners: 12; " +
                                "Padding: 15; " +
                                "}");

                // Label
                uiBuilder.appendInline("#Card" + id, "Text #Label" + id + " { " +
                                "Anchor: (Top: 5, Left: 5); " +
                                "Text: '" + label + "'; " +
                                "FontSize: 11; " +
                                "Color: #888888; " +
                                "}");

                // Value
                uiBuilder.appendInline("#Card" + id, "Text #Value" + id + " { " +
                                "Anchor: (Center); " +
                                "Text: '" + value + "'; " +
                                "FontSize: 28; " +
                                "FontWeight: Bold; " +
                                "Color: " + color + "; " +
                                "}");

                // Sub value
                uiBuilder.appendInline("#Card" + id, "Text #SubValue" + id + " { " +
                                "Anchor: (Bottom: 5, Right: 5); " +
                                "Text: '" + subValue + "'; " +
                                "FontSize: 12; " +
                                "Color: #666666; " +
                                "}");
        }

        private void buildJobBreakdown(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder,
                        JobPlayer player) {
                Set<JobType> joinedJobs = player.getJoinedJobs();

                // Breakdown container
                uiBuilder.appendInline("#StatsContainer", "Group #BreakdownSection { " +
                                "Anchor: (Top: 240, Left: 0, Right: 0, Bottom: 80); " +
                                "BackgroundColor: #16213e; " +
                                "RoundedCorners: 12; " +
                                "Padding: 20; " +
                                "Overflow: Scroll; " +
                                "}");

                // Section title
                uiBuilder.appendInline("#BreakdownSection", "Text #BreakdownTitle { " +
                                "Anchor: (Top: 0, Left: 0); " +
                                "Text: '📋 Détail par Métier'; " +
                                "FontSize: 18; " +
                                "FontWeight: Bold; " +
                                "Color: #ffffff; " +
                                "}");

                // Headers
                uiBuilder.appendInline("#BreakdownSection", "Group #TableHeader { " +
                                "Anchor: (Top: 35, Left: 0, Right: 0); " +
                                "Dimensions: (Height: 30); " +
                                "BackgroundColor: #0f3460; " +
                                "RoundedCorners: 6; " +
                                "Padding: 5; " +
                                "}");

                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Left: 10, CenterY); Text: 'Métier'; FontSize: 12; Color: #a0a0a0; }");
                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Left: 200, CenterY); Text: 'Statut'; FontSize: 12; Color: #a0a0a0; }");
                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Left: 300, CenterY); Text: 'Niveau'; FontSize: 12; Color: #a0a0a0; }");
                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Left: 380, CenterY); Text: 'XP Actuel'; FontSize: 12; Color: #a0a0a0; }");
                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Left: 500, CenterY); Text: 'XP Total'; FontSize: 12; Color: #a0a0a0; }");
                uiBuilder.appendInline("#TableHeader",
                                "Text { Anchor: (Right: 20, CenterY); Text: 'Progression'; FontSize: 12; Color: #a0a0a0; }");

                // Job rows
                int yOffset = 75;
                int index = 0;

                for (JobType type : JobType.values()) {
                        boolean isJoined = joinedJobs.contains(type);
                        JobData data = player.getJobData(type);
                        int level = data.getLevel();
                        long xp = data.getExperience();
                        long required = jobManager.getXpRequired(level);
                        long totalXp = data.getTotalExperience();
                        double progress = (xp * 100.0) / required;

                        String rowBg = index % 2 == 0 ? "#1a1a2e" : "#0f3460";
                        String statusText = isJoined ? "✓ Actif" : "○ Inactif";
                        String statusColor = isJoined ? "#2ecc71" : "#e74c3c";

                        uiBuilder.appendInline("#BreakdownSection", "Group #Row" + index + " { " +
                                        "Anchor: (Top: " + yOffset + ", Left: 0, Right: 0); " +
                                        "Dimensions: (Height: 40); " +
                                        "BackgroundColor: " + rowBg + "; " +
                                        "RoundedCorners: 6; " +
                                        "Padding: 5; " +
                                        "}");

                        // Job name with color indicator
                        uiBuilder.appendInline("#Row" + index, "Group #ColorDot" + index + " { " +
                                        "Anchor: (Left: 10, CenterY); " +
                                        "Dimensions: (Width: 12, Height: 12); " +
                                        "BackgroundColor: " + type.getColor() + "; " +
                                        "RoundedCorners: 6; " +
                                        "}");

                        uiBuilder.appendInline("#Row" + index, "Text { " +
                                        "Anchor: (Left: 30, CenterY); " +
                                        "Text: '" + type.getDisplayName() + "'; " +
                                        "FontSize: 13; " +
                                        "Color: " + type.getColor() + "; " +
                                        "}");

                        // Status
                        uiBuilder.appendInline("#Row" + index, "Text { " +
                                        "Anchor: (Left: 200, CenterY); " +
                                        "Text: '" + statusText + "'; " +
                                        "FontSize: 12; " +
                                        "Color: " + statusColor + "; " +
                                        "}");

                        // Level
                        uiBuilder.appendInline("#Row" + index, "Text { " +
                                        "Anchor: (Left: 300, CenterY); " +
                                        "Text: '" + level + "'; " +
                                        "FontSize: 14; " +
                                        "FontWeight: Bold; " +
                                        "Color: #ffffff; " +
                                        "}");

                        // Current XP
                        uiBuilder.appendInline("#Row" + index, "Text { " +
                                        "Anchor: (Left: 380, CenterY); " +
                                        "Text: '" + formatNumber(xp) + "/" + formatNumber(required) + "'; " +
                                        "FontSize: 12; " +
                                        "Color: #a0a0a0; " +
                                        "}");

                        // Total XP
                        uiBuilder.appendInline("#Row" + index, "Text { " +
                                        "Anchor: (Left: 500, CenterY); " +
                                        "Text: '" + formatNumber(totalXp) + "'; " +
                                        "FontSize: 12; " +
                                        "Color: #ffffff; " +
                                        "}");

                        // Progress bar
                        uiBuilder.appendInline("#Row" + index, "Group #ProgressBg" + index + " { " +
                                        "Anchor: (Right: 20, CenterY); " +
                                        "Dimensions: (Width: 100, Height: 16); " +
                                        "BackgroundColor: #333355; " +
                                        "RoundedCorners: 8; " +
                                        "}");

                        int fillWidth = (int) Math.max(2, Math.min(100 * (progress / 100.0), 100));
                        uiBuilder.appendInline("#ProgressBg" + index, "Group { " +
                                        "Anchor: (Left: 0, Top: 0, Bottom: 0); " +
                                        "Dimensions: (Width: " + fillWidth + "); " +
                                        "BackgroundColor: " + type.getColor() + "; " +
                                        "RoundedCorners: 8; " +
                                        "}");

                        uiBuilder.appendInline("#ProgressBg" + index, "Text { " +
                                        "Anchor: (Center); " +
                                        "Text: '" + String.format(Locale.US, "%.0f", progress) + "%'; " +
                                        "FontSize: 10; " +
                                        "Color: #ffffff; " +
                                        "}");

                        // Click to view details
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Row" + index,
                                        EventData.of("Action", "detail:" + type.name()));

                        yOffset += 45;
                        index++;
                }
        }

        private void buildFooter(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
                // Footer bar
                uiBuilder.appendInline("#StatsContainer", "Group #Footer { " +
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

                // Navigation hint
                uiBuilder.appendInline("#Footer", "Text #Hint { " +
                                "Anchor: (Left: 20, CenterY); " +
                                "Text: '💡 Cliquez sur une ligne pour voir les détails'; " +
                                "FontSize: 12; " +
                                "Color: #888888; " +
                                "}");
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                        @Nonnull StatsGuiData data) {
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

                if (data.action.startsWith("detail:")) {
                        String jobName = data.action.substring(7);
                        JobType type = JobType.fromString(jobName);
                        if (type != null) {
                                player.getPageManager().openCustomPage(ref, store, new JobDetailGui(playerRef, type));
                        }
                }
        }

        private String formatNumber(long number) {
                if (number >= 1_000_000) {
                        return String.format(Locale.US, "%.1fM", number / 1_000_000.0);
                } else if (number >= 1_000) {
                        return String.format(Locale.US, "%.1fK", number / 1_000.0);
                }
                return String.valueOf(number);
        }

        public static class StatsGuiData {
                static final String KEY_ACTION = "Action";

                public static final BuilderCodec<StatsGuiData> CODEC = BuilderCodec.<StatsGuiData>builder(
                                StatsGuiData.class, StatsGuiData::new)
                                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                                                (d, s) -> d.action = s, d -> d.action)
                                .build();

                private String action;
        }
}
