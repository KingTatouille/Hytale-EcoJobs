package fr.snoof.jobs.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.Job;
import fr.snoof.jobs.model.JobData;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.Map;

public class JobStatsPage extends InteractiveCustomUIPage<JobStatsPage.StatsPageData> {

    private final JobManager jobManager;
    private final PlayerRef playerRef;

    public JobStatsPage(PlayerRef playerRef, JobManager jobManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, StatsPageData.CODEC);
        this.playerRef = playerRef;
        this.jobManager = jobManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store) {

        JobsUiHelper.setupBar(ref, commands, events, store);
        commands.append("Common/UI/Custom/Pages/JobStatsPage.ui");

        JobPlayer player = jobManager.getPlayer(playerRef.getUuid());

        if (player == null || player.getJobs().isEmpty()) {
            commands.appendInline("#stats-container",
                    "Label { Text: Vous n'avez rejoint aucun job.; Style: (FontSize: 18; Color: #aaaaaa; Alignment: Center); Margin: (Top: 50); }");
            return;
        }

        int index = 0;
        for (Map.Entry<JobType, JobData> entry : player.getJobs().entrySet()) {
            JobType type = entry.getKey();
            JobData data = entry.getValue();
            Job job = jobManager.getJob(type.name());
            // Note: JobType.name() might not match job ID exactly if casing differs,
            // but usually enum names are upper case. Job IDs in manager init were also
            // likely uppercase or consistent.
            // Let's check JobManager init: jobs.add(new Job(JobType.FARMER...)); -> ID is
            // typically the enum name.

            if (job == null)
                continue;

            String selector = "#stats-container > Group:nth-child(" + (index + 1) + ")";
            commands.append("#stats-container", "Common/UI/Custom/JobStatsEntry.ui");

            commands.set(selector + " #job-icon", job.getIcon());
            commands.set(selector + " #job-name", job.getName());
            commands.set(selector + " #job-level", "Niveau " + data.getLevel());

            long currentXp = data.getExperience();
            long requiredXp = jobManager.getXpRequired(data.getLevel());
            double percent = jobManager.getProgressPercent(playerRef.getUuid(), type);

            commands.set(selector + " #xp-text", "XP: " + currentXp + " / " + requiredXp);
            commands.set(selector + " #percent-text", String.format("Progression: %.1f%%", percent));
            commands.set(selector + " #earnings-text", "Gains totaux: " + data.getTotalEarnings() + "$"); // Assuming
                                                                                                          // getEarnings
                                                                                                          // exists

            index++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull StatsPageData data) {
        super.handleDataEvent(ref, store, data);
        JobsUiHelper.handleData(playerRef, ref, store, jobManager, data.nav, this::close);
    }

    public static class StatsPageData {
        public static final BuilderCodec<StatsPageData> CODEC = BuilderCodec
                .builder(StatsPageData.class, StatsPageData::new)
                .addField(new KeyedCodec<>("NAV", Codec.STRING), (d, v) -> d.nav = v, d -> d.nav)
                .build();
        public String nav;
    }
}
