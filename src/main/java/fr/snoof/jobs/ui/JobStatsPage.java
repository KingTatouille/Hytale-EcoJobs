package fr.snoof.jobs.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
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
        commands.append("Pages/JobStatsPage.ui");

        JobPlayer player = jobManager.getPlayer(playerRef.getUuid());

        if (player == null || player.getJobs().isEmpty()) {
            commands.appendInline("#statsContent",
                    "Label { Text: \"Vous n'avez rejoint aucun job.\"; Style: (FontSize: 18, TextColor: #aaaaaa, HorizontalAlignment: Center); Padding: (Top: 50); }");
            return;
        }

        int index = 0;
        for (Map.Entry<JobType, JobData> entry : player.getJobs().entrySet()) {
            JobType type = entry.getKey();
            JobData data = entry.getValue();
            Job job = jobManager.getJob(type.name());

            if (job == null)
                continue;

            // D'abord append l'entrée
            commands.append("#statsContent", "Pages/JobStatsEntry.ui");

            // Ensuite set les valeurs (index + 1 car nth-child commence à 1)
            String selector = "#statsContent > Group:nth-child(" + (index + 1) + ")";
            commands.set(selector + " #jobIcon", job.getIcon());
            commands.set(selector + " #jobName", job.getName());
            commands.set(selector + " #jobLevel", "Niveau " + data.getLevel());

            long currentXp = data.getExperience();
            long requiredXp = jobManager.getXpRequired(data.getLevel());
            double percent = jobManager.getProgressPercent(playerRef.getUuid(), type);

            commands.set(selector + " #xpText", "XP: " + currentXp + " / " + requiredXp);
            commands.set(selector + " #percentText", String.format("Progression: %.1f%%", percent));
            commands.set(selector + " #earningsText", "Gains totaux: " + data.getTotalEarnings() + "$");

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