package fr.snoof.jobs.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.Job;
import java.util.List;

import javax.annotation.Nonnull;

public class JobLeaderboardPage extends InteractiveCustomUIPage<JobLeaderboardPage.LeaderboardData> {

    private final JobManager jobManager;
    private final PlayerRef playerRef;
    private String selectedJobId;

    public JobLeaderboardPage(PlayerRef playerRef, JobManager jobManager, String selectedJobId) {
        super(playerRef, CustomPageLifetime.CanDismiss, LeaderboardData.CODEC);
        this.playerRef = playerRef;
        this.jobManager = jobManager;
        // Default to first job if null
        if (selectedJobId == null && !jobManager.getAllJobs().isEmpty()) {
            this.selectedJobId = jobManager.getAllJobs().get(0).getId();
        } else {
            this.selectedJobId = selectedJobId;
        }
    }

    public JobLeaderboardPage(PlayerRef playerRef, JobManager jobManager) {
        this(playerRef, jobManager, null);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store) {

        JobsUiHelper.setupBar(ref, commands, events, store);
        commands.append("Pages/JobLeaderboardPage.ui");

        // Render Job List (Sidebar)
        int index = 0;
        List<Job> jobs = jobManager.getAllJobs();
        for (Job job : jobs) {
            String selector = "#job-buttons-container > Group:nth-child(" + (index + 1) + ")";
            // We can use a simple button template or just render a label generic if we had
            // one.
            // Let's create a quick inline generic button since we didn't make a file for
            // this sidebar item.
            // Or better, define it inline.

            // Check if selected
            String color = job.getId().equals(selectedJobId) ? "#4CAF50" : "#555555";

            commands.appendInline("#job-buttons-container",
                    "Group { Anchor: (Width: 180, Height: 40; Margin: (Bottom: 5)); " +
                            "   Group #btn-" + index + " { Anchor: (Width: 180, Height: 40); " +
                            "       Label { Text: " + job.getIcon() + " " + job.getName()
                            + "; Style: (FontSize: 14; Color: #ffffff; Alignment: Center); } " +
                            "       Background { Color: " + color + "; } " +
                            "   } " +
                            "}");

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#job-buttons-container > Group:nth-child(" + (index + 1) + ") #btn-" + index,
                    EventData.of("SELECT_JOB", job.getId()));

            index++;
        }

        // Render Leaderboard (Main content)
        if (selectedJobId != null) {
            Job selectedJob = jobManager.getJob(selectedJobId);
            if (selectedJob != null) {
                commands.set("#selected-job-title", "Classement - " + selectedJob.getName());

                List<JobManager.Records.TopPlayerEntry> topPlayers = jobManager.getTopPlayersForJob(selectedJobId, 10);

                int rank = 1;
                for (JobManager.Records.TopPlayerEntry entry : topPlayers) {
                    String rowSelector = "#ranking-list-container > Group:nth-child(" + rank + ")";
                    commands.append("#ranking-list-container", "Pages/JobLeaderboardEntry.ui");

                    commands.set(rowSelector + " #rank", "#" + rank);
                    commands.set(rowSelector + " #player-name", entry.playerName());
                    commands.set(rowSelector + " #level", "Niveau " + entry.level());
                    commands.set(rowSelector + " #xp", entry.totalXP() + " XP");

                    rank++;
                }

                if (topPlayers.isEmpty()) {
                    commands.appendInline("#ranking-list-container",
                            "Label { Text: Aucun joueur classé.; Style: (FontSize: 18; Color: #aaaaaa; Alignment: Center; Margin: (Top: 50)); }");
                }
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull LeaderboardData data) {
        super.handleDataEvent(ref, store, data);

        if (JobsUiHelper.handleData(playerRef, ref, store, jobManager, data.nav, this::close)) {
            return;
        }

        if (data.selectJob != null) {
            this.selectedJobId = data.selectJob;
            this.sendUpdate();
        }
    }

    public static class LeaderboardData {
        public static final BuilderCodec<LeaderboardData> CODEC = BuilderCodec
                .builder(LeaderboardData.class, LeaderboardData::new)
                .addField(new KeyedCodec<>("NAV", Codec.STRING), (d, v) -> d.nav = v, d -> d.nav)
                .addField(new KeyedCodec<>("SELECT_JOB", Codec.STRING), (d, v) -> d.selectJob = v, d -> d.selectJob)
                .build();
        public String nav;
        public String selectJob;
    }
}
