package fr.snoof.jobs.ui;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

public class JobInfoPage extends InteractiveCustomUIPage<JobInfoPage.JobInfoData> {

    private final JobManager jobManager;
    private final PlayerRef playerRef;
    private final JobType jobType;

    public JobInfoPage(PlayerRef playerRef, JobManager jobManager, JobType jobType) {
        super(playerRef, CustomPageLifetime.CanDismiss, JobInfoData.CODEC);
        this.playerRef = playerRef;
        this.jobManager = jobManager;
        this.jobType = jobType;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

        builder.append("Pages/JobInfoPage.ui");

        // Set Job Name
        builder.set("#JobNameLabel.Text", jobType.getDisplayName());

        // Get Player Data
        JobPlayer player = jobManager.getOrCreatePlayer(playerRef.getUuid(), playerRef.getUsername());

        int level = player.getLevel(jobType);
        long xp = player.getExperience(jobType);
        long required = jobManager.getXpRequired(level);
        double progress = jobManager.getProgressPercent(playerRef.getUuid(), jobType); // 0-100

        // Set Stats
        builder.set("#LevelLabel.Text", "Niveau " + level);
        builder.set("#XpLabel.Text", "XP: " + xp + " / " + required);
        builder.set("#PercentLabel.Text", String.format("%.1f%%", progress));

        // Set Progress Bar Width (Max 300)
        // Ensure width is at least 0
        int width = (int) ((progress / 100.0) * 300);
        if (width < 0)
            width = 0;
        if (width > 300)
            width = 300;

        // builder.set("#ProgressBarFill.Anchor", "(Height: 20, Width: " + width + ",
        // Left: 0, Top: 0)");

        // Set Icon
        String iconPath = getIconPath(jobType);
        builder.set("#JobIcon.Background", iconPath);

        // Bind Close Buttons
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                EventData.of("Button", "Close"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Button", "Close"));
    }

    private String getIconPath(JobType type) {
        switch (type.name()) {
            case "FARMER":
                return "Common/Icons/CraftingCategories/Workbench/Consumables.png";
            case "HUNTER":
                return "Common/Icons/CraftingCategories/Workbench/WeaponsRanged.png";
            case "CHAMPION":
                return "Common/Icons/CraftingCategories/Workbench/WeaponsCrude.png";
            case "MINER":
                return "Common/Icons/CraftingCategories/Workbench/Resources.png";
            case "BLACKSMITH":
                return "Common/Icons/CraftingCategories/Workbench/ArmourMetal.png";
            case "LUMBERJACK":
                return "Common/Icons/CraftingCategories/Workbench/Tools.png";
            default:
                return "Common/Icons/CraftingCategories/Workbench/Resources.png"; // Fallback
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull JobInfoData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button != null && data.button.equals("Close")) {
            this.close();
        }
    }

    public static class JobInfoData {
        private String button;

        public static final BuilderCodec<JobInfoData> CODEC = BuilderCodec
                .<JobInfoData>builder(JobInfoData.class, JobInfoData::new)
                .addField(new KeyedCodec<>("Button", Codec.STRING), (data, s) -> data.button = s,
                        data -> data.button)
                .build();
    }
}
