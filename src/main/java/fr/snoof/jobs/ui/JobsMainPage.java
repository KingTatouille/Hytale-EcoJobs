package fr.snoof.jobs.ui;

import javax.annotation.Nonnull;

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

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

public class JobsMainPage extends InteractiveCustomUIPage<JobsMainPage.JobsGuiData> {

    private final JobManager jobManager;
    private final PlayerRef playerRef;

    public JobsMainPage(PlayerRef playerRef, JobManager jobManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, JobsGuiData.CODEC);
        this.playerRef = playerRef;
        this.jobManager = jobManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        builder.append("Pages/JobsMainPage.ui");

        // Bind Close Button if needed, or rely on CanDismiss.
        // For now, we won't bind Close explicitly as CanDismiss handles ESC.
        // If the UI button is needed, we usually bind it to a close action.
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                EventData.of("Button", "Close"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Button", "Close"));

        System.out.println("DEBUG: JobsMainPage build() called for " + playerRef.getUsername());

        JobPlayer player = jobManager.getOrCreatePlayer(playerRef.getUuid(), playerRef.getUsername());

        for (JobType type : JobType.values()) {
            boolean joined = player.hasJoinedJob(type);
            String jobName = type.name();

            // Toggle button visibility
            builder.set("#btnJoin" + jobName + ".Visible", !joined);
            builder.set("#btnLeave" + jobName + ".Visible", joined);

            // Bind buttons
            if (!joined) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#btnJoin" + jobName,
                        EventData.of("Button", "Join:" + jobName));
            } else {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#btnLeave" + jobName,
                        EventData.of("Button", "Leave:" + jobName));
            }

            // Level info
            if (joined) {
                int level = player.getLevel(type);
                builder.set("#lvl" + jobName + ".Text", "Niveau " + level);
                builder.set("#lvl" + jobName + ".Visible", true);
            } else {
                builder.set("#lvl" + jobName + ".Visible", false);
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull JobsGuiData data) {
        super.handleDataEvent(ref, store, data);

        if (data.button != null) {
            String[] parts = data.button.split(":");
            String action = parts[0];

            if (action.equals("Close")) {
                this.close();
                return;
            }

            if (parts.length > 1) {
                String jobName = parts[1];
                PlayerRef pRef = playerRef;

                if (action.equals("Join")) {
                    if (jobManager.assignJob(pRef, jobName)) {
                        pRef.sendMessage(MessageUtil.success("Job " + jobName + " rejoint !"));
                    } else {
                        pRef.sendMessage(MessageUtil.error("Impossible de rejoindre ce job."));
                    }
                } else if (action.equals("Leave")) {
                    if (jobManager.removeJob(pRef, jobName)) {
                        pRef.sendMessage(MessageUtil.info("Job " + jobName + " quitté."));
                    } else {
                        pRef.sendMessage(MessageUtil.error("Impossible de quitter ce job."));
                    }
                }

                // Refresh the UI to show updated state
                this.sendUpdate();
            }
        }
    }

    @Override
    public void sendUpdate() {
        super.sendUpdate();
    }

    public static class JobsGuiData {
        static final String KEY_BUTTON = "Button";

        public static final BuilderCodec<JobsGuiData> CODEC = BuilderCodec
                .<JobsGuiData>builder(JobsGuiData.class, JobsGuiData::new)
                .addField(new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (data, s) -> data.button = s,
                        data -> data.button)
                .build();

        private String button;
    }

}
