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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.EcoJobsPlugin;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.hook.PermsHook;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin GUI for managing player jobs - set levels, XP, reset jobs, etc.
 */
public class JobAdminGui extends InteractiveCustomUIPage<JobAdminGui.AdminGuiData> {

    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final UUID adminUuid;
    private final String adminName;

    private String searchQuery = "";
    private List<PlayerRef> displayedPlayers = new ArrayList<>();
    private PlayerRef selectedPlayer = null;
    private JobType selectedJob = JobType.MINER;

    public JobAdminGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminGuiData.CODEC);
        this.jobManager = EcoJobsPlugin.getInstance().getJobManager();
        this.configManager = EcoJobsPlugin.getInstance().getConfigManager();
        this.adminUuid = playerRef.getUuid();
        this.adminName = playerRef.getUsername();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

        // Permission check
        if (!PermsHook.hasPermission(adminUuid, "jobs.admin")) {
            buildNoPermission(uiBuilder, eventBuilder);
            return;
        }

        // Main container
        uiBuilder.appendInline("", "Group #AdminContainer { " +
                "Anchor: (Fill); " +
                "BackgroundColor: #1a1a2e; " +
                "Padding: 25; " +
                "}");

        // Header
        buildHeader(uiBuilder, eventBuilder);

        // Left panel - Player list
        buildPlayerList(uiBuilder, eventBuilder);

        // Right panel - Player details/actions
        buildPlayerDetails(uiBuilder, eventBuilder);

        // Footer
        buildFooter(uiBuilder, eventBuilder);
    }

    private void buildNoPermission(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        uiBuilder.appendInline("", "Group #NoPermContainer { " +
                "Anchor: (Fill); " +
                "BackgroundColor: #1a1a2e; " +
                "Padding: 50; " +
                "}");

        uiBuilder.appendInline("#NoPermContainer", "Text #NoPermText { " +
                "Anchor: (Center); " +
                "Text: '⛔ Accès Refusé'; " +
                "FontSize: 32; " +
                "FontWeight: Bold; " +
                "Color: #e74c3c; " +
                "}");

        uiBuilder.appendInline("#NoPermContainer", "Text #NoPermDesc { " +
                "Anchor: (CenterX, Top: 55%); " +
                "Text: 'Vous devez avoir la permission jobs.admin'; " +
                "FontSize: 16; " +
                "Color: #888888; " +
                "}");

        uiBuilder.appendInline("#NoPermContainer", "Button #CloseBtn { " +
                "Anchor: (CenterX, Top: 65%); " +
                "Dimensions: (Width: 150, Height: 40); " +
                "Text: 'Fermer'; " +
                "FontSize: 16; " +
                "BackgroundColor: #e94560; " +
                "HoverColor: #ff6b8a; " +
                "RoundedCorners: 10; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
                EventData.of("Action", "close"));
    }

    private void buildHeader(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Header bar
        uiBuilder.appendInline("#AdminContainer", "Group #Header { " +
                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 70); " +
                "BackgroundColor: #c0392b; " +
                "RoundedCorners: 12; " +
                "Padding: 15; " +
                "}");

        // Admin icon and title
        uiBuilder.appendInline("#Header", "Text #Title { " +
                "Anchor: (Left: 20, CenterY); " +
                "Text: '⚙ Administration des Métiers'; " +
                "FontSize: 24; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Reload config button
        uiBuilder.appendInline("#Header", "Button #ReloadBtn { " +
                "Anchor: (Right: 20, CenterY); " +
                "Dimensions: (Width: 140, Height: 38); " +
                "Text: '🔄 Recharger'; " +
                "FontSize: 14; " +
                "BackgroundColor: #27ae60; " +
                "HoverColor: #2ecc71; " +
                "RoundedCorners: 8; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadBtn",
                EventData.of("Action", "reload"));

        // Back button
        uiBuilder.appendInline("#Header", "Button #BackBtn { " +
                "Anchor: (Right: 175, CenterY); " +
                "Dimensions: (Width: 100, Height: 38); " +
                "Text: '← Retour'; " +
                "FontSize: 14; " +
                "BackgroundColor: #333355; " +
                "HoverColor: #4a4a6a; " +
                "RoundedCorners: 8; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Action", "back"));
    }

    private void buildPlayerList(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Left panel
        uiBuilder.appendInline("#AdminContainer", "Group #LeftPanel { " +
                "Anchor: (Top: 90, Left: 0, Bottom: 80); " +
                "Dimensions: (Width: 350); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 15; " +
                "}");

        // Search header
        uiBuilder.appendInline("#LeftPanel", "Text #PlayersTitle { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '👥 Joueurs en ligne'; " +
                "FontSize: 16; " +
                "FontWeight: Bold; " +
                "Color: #ffffff; " +
                "}");

        // Search input
        uiBuilder.appendInline("#LeftPanel", "Input #SearchInput { " +
                "Anchor: (Top: 35, Left: 0, Right: 0); " +
                "Dimensions: (Height: 36); " +
                "Placeholder: '🔍 Rechercher un joueur...'; " +
                "BackgroundColor: #0f3460; " +
                "RoundedCorners: 8; " +
                "Padding: 8; " +
                "}");
        uiBuilder.set("#SearchInput.Value", this.searchQuery);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
                EventData.of("@SearchQuery", "#SearchInput.Value"), false);

        // Player list
        uiBuilder.appendInline("#LeftPanel", "Group #PlayerList { " +
                "Anchor: (Top: 85, Left: 0, Right: 0, Bottom: 0); " +
                "Overflow: Scroll; " +
                "}");

        // Get online players filtered by search
        displayedPlayers.clear();
        for (PlayerRef pr : Universe.get().getPlayers()) {
            if (searchQuery.isEmpty() || pr.getUsername().toLowerCase().contains(searchQuery.toLowerCase())) {
                displayedPlayers.add(pr);
            }
        }

        int yOffset = 0;
        int index = 0;
        for (PlayerRef pr : displayedPlayers) {
            boolean isSelected = selectedPlayer != null && selectedPlayer.getUuid().equals(pr.getUuid());
            String bgColor = isSelected ? "#3498db" : "#0f3460";

            JobPlayer jobPlayer = jobManager.getOrCreatePlayer(pr.getUuid(), pr.getUsername());
            int totalLevel = jobPlayer.getTotalLevel();
            int activeJobs = jobPlayer.getJoinedJobCount();

            uiBuilder.appendInline("#PlayerList", "Group #PlayerRow" + index + " { " +
                    "Anchor: (Top: " + yOffset + ", Left: 0, Right: 0); " +
                    "Dimensions: (Height: 50); " +
                    "BackgroundColor: " + bgColor + "; " +
                    "RoundedCorners: 8; " +
                    "Padding: 8; " +
                    "}");

            // Player name
            uiBuilder.appendInline("#PlayerRow" + index, "Text { " +
                    "Anchor: (Left: 10, Top: 5); " +
                    "Text: '" + pr.getUsername() + "'; " +
                    "FontSize: 14; " +
                    "FontWeight: Bold; " +
                    "Color: #ffffff; " +
                    "}");

            // Stats summary
            uiBuilder.appendInline("#PlayerRow" + index, "Text { " +
                    "Anchor: (Left: 10, Bottom: 5); " +
                    "Text: 'Nv." + totalLevel + " | " + activeJobs + " métiers'; " +
                    "FontSize: 11; " +
                    "Color: #a0a0a0; " +
                    "}");

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PlayerRow" + index,
                    EventData.of("Action", "select_player:" + pr.getUsername()));

            yOffset += 55;
            index++;
        }

        if (displayedPlayers.isEmpty()) {
            uiBuilder.appendInline("#PlayerList", "Text #NoPlayers { " +
                    "Anchor: (Top: 20, CenterX); " +
                    "Text: 'Aucun joueur trouvé'; " +
                    "FontSize: 14; " +
                    "Color: #666666; " +
                    "}");
        }
    }

    private void buildPlayerDetails(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Right panel
        uiBuilder.appendInline("#AdminContainer", "Group #RightPanel { " +
                "Anchor: (Top: 90, Left: 370, Right: 0, Bottom: 80); " +
                "BackgroundColor: #16213e; " +
                "RoundedCorners: 12; " +
                "Padding: 20; " +
                "}");

        if (selectedPlayer == null) {
            // No player selected
            uiBuilder.appendInline("#RightPanel", "Text #SelectPrompt { " +
                    "Anchor: (Center); " +
                    "Text: '← Sélectionnez un joueur'; " +
                    "FontSize: 18; " +
                    "Color: #666666; " +
                    "}");
            return;
        }

        JobPlayer jobPlayer = jobManager.getOrCreatePlayer(selectedPlayer.getUuid(), selectedPlayer.getUsername());

        // Player header
        uiBuilder.appendInline("#RightPanel", "Group #PlayerHeader { " +
                "Anchor: (Top: 0, Left: 0, Right: 0); " +
                "Dimensions: (Height: 60); " +
                "BackgroundColor: #3498db40; " +
                "RoundedCorners: 10; " +
                "Padding: 10; " +
                "}");

        uiBuilder.appendInline("#PlayerHeader", "Text { " +
                "Anchor: (Left: 10, Top: 5); " +
                "Text: '" + selectedPlayer.getUsername() + "'; " +
                "FontSize: 22; " +
                "FontWeight: Bold; " +
                "Color: #3498db; " +
                "}");

        uiBuilder.appendInline("#PlayerHeader", "Text { " +
                "Anchor: (Left: 10, Bottom: 5); " +
                "Text: 'UUID: " + selectedPlayer.getUuid().toString().substring(0, 8) + "...'; " +
                "FontSize: 11; " +
                "Color: #888888; " +
                "}");

        // Job selector
        uiBuilder.appendInline("#RightPanel", "Group #JobSelector { " +
                "Anchor: (Top: 75, Left: 0, Right: 0); " +
                "Dimensions: (Height: 40); " +
                "LayoutMode: Left; " +
                "LayoutGap: 8; " +
                "}");

        int tabIndex = 0;
        for (JobType type : JobType.values()) {
            boolean isSelected = type == selectedJob;
            String bgColor = isSelected ? type.getColor() : "#333355";

            uiBuilder.appendInline("#JobSelector", "Button #JobTab" + tabIndex + " { " +
                    "Dimensions: (Width: 90, Height: 32); " +
                    "Text: '" + type.name().substring(0, Math.min(8, type.name().length())) + "'; " +
                    "FontSize: 10; " +
                    "BackgroundColor: " + bgColor + "; " +
                    "HoverColor: " + type.getColor() + "; " +
                    "RoundedCorners: 6; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#JobTab" + tabIndex,
                    EventData.of("Action", "select_job:" + type.name()));

            tabIndex++;
        }

        // Selected job stats
        int level = jobPlayer.getLevel(selectedJob);
        long xp = jobPlayer.getExperience(selectedJob);
        long required = jobManager.getXpRequired(level);
        long totalXp = jobPlayer.getTotalExperience(selectedJob);
        boolean isJoined = jobPlayer.hasJoinedJob(selectedJob);

        uiBuilder.appendInline("#RightPanel", "Group #JobStats { " +
                "Anchor: (Top: 130, Left: 0, Right: 0); " +
                "Dimensions: (Height: 120); " +
                "BackgroundColor: #0f3460; " +
                "RoundedCorners: 10; " +
                "Padding: 15; " +
                "}");

        uiBuilder.appendInline("#JobStats", "Text { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '" + selectedJob.getDisplayName() + "'; " +
                "FontSize: 18; " +
                "FontWeight: Bold; " +
                "Color: " + selectedJob.getColor() + "; " +
                "}");

        String statusText = isJoined ? "✓ Rejoint" : "○ Non rejoint";
        String statusColor = isJoined ? "#2ecc71" : "#e74c3c";
        uiBuilder.appendInline("#JobStats", "Text { " +
                "Anchor: (Top: 0, Right: 0); " +
                "Text: '" + statusText + "'; " +
                "FontSize: 12; " +
                "Color: " + statusColor + "; " +
                "}");

        uiBuilder.appendInline("#JobStats", "Text { " +
                "Anchor: (Top: 35, Left: 0); " +
                "Text: 'Niveau: " + level + " / " + configManager.getConfig().maxLevel + "'; " +
                "FontSize: 14; " +
                "Color: #ffffff; " +
                "}");

        uiBuilder.appendInline("#JobStats", "Text { " +
                "Anchor: (Top: 55, Left: 0); " +
                "Text: 'XP: " + xp + " / " + required + "'; " +
                "FontSize: 14; " +
                "Color: #a0a0a0; " +
                "}");

        uiBuilder.appendInline("#JobStats", "Text { " +
                "Anchor: (Top: 75, Left: 0); " +
                "Text: 'XP Total: " + formatNumber(totalXp) + "'; " +
                "FontSize: 14; " +
                "Color: #888888; " +
                "}");

        // Admin actions
        uiBuilder.appendInline("#RightPanel", "Group #AdminActions { " +
                "Anchor: (Top: 270, Left: 0, Right: 0); " +
                "Dimensions: (Height: 180); " +
                "BackgroundColor: #c0392b20; " +
                "BorderColor: #c0392b; " +
                "BorderWidth: 1; " +
                "RoundedCorners: 10; " +
                "Padding: 15; " +
                "}");

        uiBuilder.appendInline("#AdminActions", "Text { " +
                "Anchor: (Top: 0, Left: 0); " +
                "Text: '⚡ Actions Admin'; " +
                "FontSize: 16; " +
                "FontWeight: Bold; " +
                "Color: #e74c3c; " +
                "}");

        // Set level row
        uiBuilder.appendInline("#AdminActions", "Group #SetLevelRow { " +
                "Anchor: (Top: 35, Left: 0, Right: 0); " +
                "Dimensions: (Height: 36); " +
                "}");

        uiBuilder.appendInline("#SetLevelRow", "Text { " +
                "Anchor: (Left: 0, CenterY); " +
                "Text: 'Niveau:'; " +
                "FontSize: 13; " +
                "Color: #ffffff; " +
                "}");

        // Level buttons
        int[] levelAmounts = { -10, -1, 1, 10 };
        int xOffset = 100;
        for (int amount : levelAmounts) {
            String sign = amount > 0 ? "+" : "";
            String btnColor = amount > 0 ? "#27ae60" : "#e74c3c";

            uiBuilder.appendInline("#SetLevelRow", "Button #LevelBtn" + amount + " { " +
                    "Anchor: (Left: " + xOffset + ", CenterY); " +
                    "Dimensions: (Width: 50, Height: 28); " +
                    "Text: '" + sign + amount + "'; " +
                    "FontSize: 12; " +
                    "BackgroundColor: " + btnColor + "; " +
                    "RoundedCorners: 6; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LevelBtn" + amount,
                    EventData.of("Action", "set_level:" + amount));

            xOffset += 58;
        }

        // Set XP row
        uiBuilder.appendInline("#AdminActions", "Group #SetXpRow { " +
                "Anchor: (Top: 80, Left: 0, Right: 0); " +
                "Dimensions: (Height: 36); " +
                "}");

        uiBuilder.appendInline("#SetXpRow", "Text { " +
                "Anchor: (Left: 0, CenterY); " +
                "Text: 'XP:'; " +
                "FontSize: 13; " +
                "Color: #ffffff; " +
                "}");

        // XP buttons
        int[] xpAmounts = { -100, -10, 10, 100 };
        xOffset = 100;
        for (int amount : xpAmounts) {
            String sign = amount > 0 ? "+" : "";
            String btnColor = amount > 0 ? "#27ae60" : "#e74c3c";

            uiBuilder.appendInline("#SetXpRow", "Button #XpBtn" + amount + " { " +
                    "Anchor: (Left: " + xOffset + ", CenterY); " +
                    "Dimensions: (Width: 50, Height: 28); " +
                    "Text: '" + sign + amount + "'; " +
                    "FontSize: 12; " +
                    "BackgroundColor: " + btnColor + "; " +
                    "RoundedCorners: 6; " +
                    "}");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#XpBtn" + amount,
                    EventData.of("Action", "add_xp:" + amount));

            xOffset += 58;
        }

        // Reset button
        uiBuilder.appendInline("#AdminActions", "Button #ResetJobBtn { " +
                "Anchor: (Top: 125, Left: 0); " +
                "Dimensions: (Width: 150, Height: 32); " +
                "Text: '🔄 Reset ce métier'; " +
                "FontSize: 12; " +
                "BackgroundColor: #e67e22; " +
                "HoverColor: #f39c12; " +
                "RoundedCorners: 6; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetJobBtn",
                EventData.of("Action", "reset_job"));

        // Reset all button
        uiBuilder.appendInline("#AdminActions", "Button #ResetAllBtn { " +
                "Anchor: (Top: 125, Left: 160); " +
                "Dimensions: (Width: 150, Height: 32); " +
                "Text: '⚠ Reset TOUS'; " +
                "FontSize: 12; " +
                "BackgroundColor: #c0392b; " +
                "HoverColor: #e74c3c; " +
                "RoundedCorners: 6; " +
                "}");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ResetAllBtn",
                EventData.of("Action", "reset_all"));
    }

    private void buildFooter(@Nonnull UICommandBuilder uiBuilder, @Nonnull UIEventBuilder eventBuilder) {
        // Footer bar
        uiBuilder.appendInline("#AdminContainer", "Group #Footer { " +
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

        // Stats
        int onlinePlayers = Universe.get().getPlayers().size();
        uiBuilder.appendInline("#Footer", "Text #OnlineCount { " +
                "Anchor: (Left: 20, CenterY); " +
                "Text: '👥 " + onlinePlayers + " joueurs en ligne'; " +
                "FontSize: 13; " +
                "Color: #888888; " +
                "}");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull AdminGuiData data) {
        super.handleDataEvent(ref, store, data);

        PlayerRef adminRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player adminPlayer = store.getComponent(ref, Player.getComponentType());

        // Handle search query update
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim();
            // Don't rebuild for every character, just update state
        }

        if (data.action == null || data.action.isEmpty()) {
            this.rebuild();
            return;
        }

        switch (data.action) {
            case "back" -> {
                adminPlayer.getPageManager().openCustomPage(ref, store, new JobsMainGui(adminRef));
                return;
            }
            case "close" -> {
                this.close();
                return;
            }
            case "reload" -> {
                configManager.load();
                adminRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                        "§a✓ Configuration rechargée!"));
                this.rebuild();
                return;
            }
        }

        // Player selection
        if (data.action.startsWith("select_player:")) {
            String playerName = data.action.substring(14);
            for (PlayerRef pr : Universe.get().getPlayers()) {
                if (pr.getUsername().equals(playerName)) {
                    this.selectedPlayer = pr;
                    break;
                }
            }
            this.rebuild();
            return;
        }

        // Job selection
        if (data.action.startsWith("select_job:")) {
            String jobName = data.action.substring(11);
            JobType type = JobType.fromString(jobName);
            if (type != null) {
                this.selectedJob = type;
            }
            this.rebuild();
            return;
        }

        // Admin actions (require selected player)
        if (selectedPlayer == null) {
            return;
        }

        JobPlayer jobPlayer = jobManager.getOrCreatePlayer(selectedPlayer.getUuid(), selectedPlayer.getUsername());

        if (data.action.startsWith("set_level:")) {
            int amount = Integer.parseInt(data.action.substring(10));
            int newLevel = Math.max(1, Math.min(jobPlayer.getLevel(selectedJob) + amount,
                    configManager.getConfig().maxLevel));
            jobManager.setLevel(selectedPlayer.getUuid(), selectedPlayer.getUsername(), selectedJob, newLevel);
            adminRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                    "§a✓ Niveau de " + selectedPlayer.getUsername() + " en " + selectedJob.getDisplayName() +
                            " défini à " + newLevel));
            this.rebuild();
            return;
        }

        if (data.action.startsWith("add_xp:")) {
            int amount = Integer.parseInt(data.action.substring(7));
            long newXp = Math.max(0, jobPlayer.getExperience(selectedJob) + amount);
            jobManager.setExperience(selectedPlayer.getUuid(), selectedPlayer.getUsername(), selectedJob, newXp);
            adminRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                    "§a✓ " + (amount > 0 ? "Ajouté" : "Retiré") + " " + Math.abs(amount) + " XP à " +
                            selectedPlayer.getUsername() + " en " + selectedJob.getDisplayName()));
            this.rebuild();
            return;
        }

        if (data.action.equals("reset_job")) {
            jobManager.resetPlayer(selectedPlayer.getUuid(), selectedJob);
            adminRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                    "§e○ Métier " + selectedJob.getDisplayName() + " de " + selectedPlayer.getUsername()
                            + " réinitialisé"));
            this.rebuild();
            return;
        }

        if (data.action.equals("reset_all")) {
            jobManager.resetPlayer(selectedPlayer.getUuid(), null); // null = all jobs
            adminRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                    "§c⚠ TOUS les métiers de " + selectedPlayer.getUsername() + " réinitialisés!"));
            this.rebuild();
            return;
        }
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public static class AdminGuiData {
        static final String KEY_ACTION = "Action";
        static final String KEY_SEARCH = "@SearchQuery";

        public static final BuilderCodec<AdminGuiData> CODEC = BuilderCodec.<AdminGuiData>builder(
                AdminGuiData.class, AdminGuiData::new)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        (d, s) -> d.action = s, d -> d.action)
                .addField(new KeyedCodec<>(KEY_SEARCH, Codec.STRING),
                        (d, s) -> d.searchQuery = s, d -> d.searchQuery)
                .build();

        private String action;
        private String searchQuery;
    }
}
