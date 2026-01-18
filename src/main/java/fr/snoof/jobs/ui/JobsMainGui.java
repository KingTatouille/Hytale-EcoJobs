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
 * Main Jobs GUI - Shows all available jobs with their status, progression etc.
 * Allows players to join/leave jobs and see their stats.
 */
public class JobsMainGui extends InteractiveCustomUIPage<JobsMainGui.JobsGuiData> {

        private final JobManager jobManager;
        private final ConfigManager configManager;
        private final UUID playerUuid;
        private final String playerName;

        public JobsMainGui(@Nonnull PlayerRef playerRef) {
                super(playerRef, CustomPageLifetime.CanDismiss, JobsGuiData.CODEC);
                this.jobManager = EcoJobsPlugin.getInstance().getJobManager();
                this.configManager = EcoJobsPlugin.getInstance().getConfigManager();
                this.playerUuid = playerRef.getUuid();
                this.playerName = playerRef.getUsername();
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
                        @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

                // Main container with title
                uiBuilder.appendInline("", "Group #JobsMainContainer { " +
                                "Anchor: (Fill); " +
                                "BackgroundColor: #1a1a2e; " +
                                "Padding: 20; " +
                                "}");

                // Header
                buildHeader(uiBuilder, eventBuilder);

                // Jobs grid
                buildJobsGrid(uiBuilder, eventBuilder);

                // Footer with navigation
                buildFooter(uiBuilder, eventBuilder);
        }

        private void buildHeader(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
                JobPlayer player = jobManager.getOrCreatePlayer(playerUuid, playerName);
                int maxJobs = configManager.getConfig().maxJobs;
                int currentJobs = player.getJoinedJobCount();

                // Title bar
                uiBuilder.appendInline("#JobsMainContainer", "Group #Header { " +
                                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                                "Dimensions: (Height: 80); " +
                                "BackgroundColor: #16213e; " +
                                "RoundedCorners: 10; " +
                                "Padding: 15; " +
                                "}");

                // Title text
                uiBuilder.appendInline("#Header", "Text #Title { " +
                                "Text: '⚒ MÉTIERS'; " +
                                "FontSize: 32; " +
                                "Color: #e94560; " +
                                "Anchor: (Left: 0, CenterY); " +
                                "FontWeight: Bold; " +
                                "}");

                // Job counter
                String counterText = String.format("Métiers actifs: %d/%d", currentJobs, maxJobs);
                uiBuilder.appendInline("#Header", "Text #JobCounter { " +
                                "Text: '" + counterText + "'; " +
                                "FontSize: 18; " +
                                "Color: #0f3460; " +
                                "Anchor: (Right: 20, CenterY); " +
                                "BackgroundColor: #e94560; " +
                                "Padding: 8 15; " +
                                "RoundedCorners: 15; " +
                                "}");

                // Navigation buttons
                uiBuilder.appendInline("#Header", "Button #StatsBtn { " +
                                "Anchor: (Right: 200, CenterY); " +
                                "Dimensions: (Width: 100, Height: 36); " +
                                "Text: '📊 Stats'; " +
                                "FontSize: 14; " +
                                "BackgroundColor: #533483; " +
                                "HoverColor: #7952a3; " +
                                "RoundedCorners: 8; " +
                                "}");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#StatsBtn",
                                EventData.of("Action", "stats"));

                uiBuilder.appendInline("#Header", "Button #TopBtn { " +
                                "Anchor: (Right: 310, CenterY); " +
                                "Dimensions: (Width: 100, Height: 36); " +
                                "Text: '🏆 Top'; " +
                                "FontSize: 14; " +
                                "BackgroundColor: #f9a825; " +
                                "HoverColor: #fbc02d; " +
                                "RoundedCorners: 8; " +
                                "}");
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TopBtn",
                                EventData.of("Action", "top"));
        }

        private void buildJobsGrid(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
                JobPlayer player = jobManager.getOrCreatePlayer(playerUuid, playerName);
                Set<JobType> joinedJobs = player.getJoinedJobs();

                // Jobs container grid
                uiBuilder.appendInline("#JobsMainContainer", "Group #JobsGrid { " +
                                "Anchor: (Top: 100, Left: 0, Right: 0, Bottom: 80); " +
                                "LayoutMode: Left; " +
                                "LayoutWrap: true; " +
                                "LayoutGap: 15; " +
                                "Padding: 10; " +
                                "Overflow: Scroll; " +
                                "}");

                int index = 0;
                for (JobType type : JobType.values()) {
                        boolean isJoined = joinedJobs.contains(type);
                        JobData data = player.getJobData(type);
                        int level = data.getLevel();
                        long xp = data.getExperience();
                        long required = jobManager.getXpRequired(level);
                        double progress = (xp * 100.0) / required;

                        String cardBgColor = isJoined ? "#0f3460" : "#1f1f3d";
                        String borderColor = isJoined ? type.getColor() : "#333355";
                        String statusText = isJoined ? "✓ Rejoint" : "Disponible";
                        String statusColor = isJoined ? "#2ecc71" : "#95a5a6";

                        // Job card
                        uiBuilder.appendInline("#JobsGrid", "Group #JobCard" + index + " { " +
                                        "Dimensions: (Width: 280, Height: 200); " +
                                        "BackgroundColor: " + cardBgColor + "; " +
                                        "BorderColor: " + borderColor + "; " +
                                        "BorderWidth: 2; " +
                                        "RoundedCorners: 12; " +
                                        "Padding: 15; " +
                                        "}");

                        // Job icon placeholder (colored circle)
                        uiBuilder.appendInline("#JobCard" + index, "Group #JobIcon" + index + " { " +
                                        "Anchor: (Top: 10, Left: 10); " +
                                        "Dimensions: (Width: 50, Height: 50); " +
                                        "BackgroundColor: " + type.getColor() + "; " +
                                        "RoundedCorners: 25; " +
                                        "}");

                        // Job name
                        uiBuilder.appendInline("#JobCard" + index, "Text #JobName" + index + " { " +
                                        "Anchor: (Top: 15, Left: 70); " +
                                        "Text: '" + type.getDisplayName() + "'; " +
                                        "FontSize: 18; " +
                                        "FontWeight: Bold; " +
                                        "Color: " + type.getColor() + "; " +
                                        "}");

                        // Job description
                        uiBuilder.appendInline("#JobCard" + index, "Text #JobDesc" + index + " { " +
                                        "Anchor: (Top: 40, Left: 70, Right: 10); " +
                                        "Text: '" + type.getDescription() + "'; " +
                                        "FontSize: 12; " +
                                        "Color: #a0a0a0; " +
                                        "}");

                        // Status badge
                        uiBuilder.appendInline("#JobCard" + index, "Text #Status" + index + " { " +
                                        "Anchor: (Top: 10, Right: 10); " +
                                        "Text: '" + statusText + "'; " +
                                        "FontSize: 11; " +
                                        "Color: " + statusColor + "; " +
                                        "BackgroundColor: #00000040; " +
                                        "Padding: 4 8; " +
                                        "RoundedCorners: 10; " +
                                        "}");

                        // Level display
                        uiBuilder.appendInline("#JobCard" + index, "Text #Level" + index + " { " +
                                        "Anchor: (Top: 75, Left: 15); " +
                                        "Text: 'Niveau " + level + "'; " +
                                        "FontSize: 22; " +
                                        "FontWeight: Bold; " +
                                        "Color: #ffffff; " +
                                        "}");

                        // XP bar background
                        uiBuilder.appendInline("#JobCard" + index, "Group #XpBarBg" + index + " { " +
                                        "Anchor: (Top: 110, Left: 15, Right: 15); " +
                                        "Dimensions: (Height: 12); " +
                                        "BackgroundColor: #333355; " +
                                        "RoundedCorners: 6; " +
                                        "}");

                        // XP bar fill
                        double barWidth = Math.max(5, Math.min(250 * (progress / 100.0), 250));
                        uiBuilder.appendInline("#XpBarBg" + index, "Group #XpBarFill" + index + " { " +
                                        "Anchor: (Left: 0, Top: 0, Bottom: 0); " +
                                        "Dimensions: (Width: " + (int) barWidth + "); " +
                                        "BackgroundColor: " + type.getColor() + "; " +
                                        "RoundedCorners: 6; " +
                                        "}");

                        // XP text
                        uiBuilder.appendInline("#JobCard" + index, "Text #XpText" + index + " { " +
                                        "Anchor: (Top: 128, Left: 15); " +
                                        "Text: '" + xp + " / " + required + " XP ("
                                        + String.format(Locale.US, "%.1f", progress) + "%)'; " +
                                        "FontSize: 11; " +
                                        "Color: #888888; " +
                                        "}");

                        // Action button (Join/Leave/Details)
                        String btnText;
                        String btnColor;
                        String action;

                        if (isJoined) {
                                btnText = "📋 Détails";
                                btnColor = "#3498db";
                                action = "details:" + type.name();
                        } else {
                                btnText = "➕ Rejoindre";
                                btnColor = "#27ae60";
                                action = "join:" + type.name();
                        }

                        uiBuilder.appendInline("#JobCard" + index, "Button #ActionBtn" + index + " { " +
                                        "Anchor: (Bottom: 10, Left: 15); " +
                                        "Dimensions: (Width: 120, Height: 32); " +
                                        "Text: '" + btnText + "'; " +
                                        "FontSize: 13; " +
                                        "BackgroundColor: " + btnColor + "; " +
                                        "HoverColor: " + adjustColor(btnColor, 20) + "; " +
                                        "RoundedCorners: 8; " +
                                        "}");
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ActionBtn" + index,
                                        EventData.of("Action", action));

                        // Leave button (only if joined)
                        if (isJoined) {
                                uiBuilder.appendInline("#JobCard" + index, "Button #LeaveBtn" + index + " { " +
                                                "Anchor: (Bottom: 10, Right: 15); " +
                                                "Dimensions: (Width: 100, Height: 32); " +
                                                "Text: '✕ Quitter'; " +
                                                "FontSize: 13; " +
                                                "BackgroundColor: #c0392b; " +
                                                "HoverColor: #e74c3c; " +
                                                "RoundedCorners: 8; " +
                                                "}");
                                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LeaveBtn" + index,
                                                EventData.of("Action", "leave:" + type.name()));
                        }

                        index++;
                }
        }

        private void buildFooter(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
                // Footer bar
                uiBuilder.appendInline("#JobsMainContainer", "Group #Footer { " +
                                "Anchor: (Bottom: 0, Left: 0, Right: 0); " +
                                "Dimensions: (Height: 60); " +
                                "BackgroundColor: #16213e; " +
                                "RoundedCorners: 10; " +
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

                // Help text
                uiBuilder.appendInline("#Footer", "Text #HelpText { " +
                                "Anchor: (Left: 20, CenterY); " +
                                "Text: '💡 Cliquez sur un métier pour voir les détails ou le rejoindre'; " +
                                "FontSize: 12; " +
                                "Color: #888888; " +
                                "}");
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                        @Nonnull JobsGuiData data) {
                super.handleDataEvent(ref, store, data);

                if (data.action == null || data.action.isEmpty()) {
                        return;
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                Player player = store.getComponent(ref, Player.getComponentType());
                JobPlayer jobPlayer = jobManager.getOrCreatePlayer(playerUuid, playerName);

                if (data.action.equals("close")) {
                        this.close();
                        return;
                }

                if (data.action.equals("stats")) {
                        player.getPageManager().openCustomPage(ref, store, new JobStatsGui(playerRef));
                        return;
                }

                if (data.action.equals("top")) {
                        player.getPageManager().openCustomPage(ref, store, new JobLeaderboardGui(playerRef));
                        return;
                }

                if (data.action.startsWith("join:")) {
                        String jobName = data.action.substring(5);
                        JobType type = JobType.fromString(jobName);
                        if (type != null && !jobPlayer.hasJoinedJob(type)) {
                                int maxJobs = configManager.getConfig().maxJobs;
                                if (jobPlayer.getJoinedJobCount() < maxJobs) {
                                        jobPlayer.joinJob(type);
                                        playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                                        "§a✓ Vous avez rejoint le métier " + type.getDisplayName()
                                                                        + "!"));
                                } else {
                                        playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                                        "§c✕ Limite de " + maxJobs + " métiers atteinte!"));
                                }
                        }
                        this.rebuild();
                        return;
                }

                if (data.action.startsWith("leave:")) {
                        String jobName = data.action.substring(6);
                        JobType type = JobType.fromString(jobName);
                        if (type != null && jobPlayer.hasJoinedJob(type)) {
                                jobPlayer.leaveJob(type);
                                playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                                "§e○ Vous avez quitté le métier " + type.getDisplayName()));
                        }
                        this.rebuild();
                        return;
                }

                if (data.action.startsWith("details:")) {
                        String jobName = data.action.substring(8);
                        JobType type = JobType.fromString(jobName);
                        if (type != null) {
                                player.getPageManager().openCustomPage(ref, store, new JobDetailGui(playerRef, type));
                        }
                        return;
                }
        }

        /**
         * Adjust a hex color brightness
         */
        private String adjustColor(String hexColor, int amount) {
                try {
                        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
                        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
                        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

                        r = Math.min(255, Math.max(0, r + amount));
                        g = Math.min(255, Math.max(0, g + amount));
                        b = Math.min(255, Math.max(0, b + amount));

                        return String.format("#%02x%02x%02x", r, g, b);
                } catch (Exception e) {
                        return hexColor;
                }
        }

        /**
         * Event data codec for the Jobs GUI
         */
        public static class JobsGuiData {
                static final String KEY_ACTION = "Action";

                public static final BuilderCodec<JobsGuiData> CODEC = BuilderCodec.<JobsGuiData>builder(
                                JobsGuiData.class, JobsGuiData::new)
                                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                                                (d, s) -> d.action = s, d -> d.action)
                                .build();

                private String action;
        }
}
