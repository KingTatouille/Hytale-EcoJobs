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
        List<Job> jobs = jobManager.getAllJobs();
        for (int index = 0; index < jobs.size(); index++) {
            Job job = jobs.get(index);

            // Check if selected
            String color = job.getId().equals(selectedJobId) ? "#4CAF50" : "#555555";

            // D'abord append l'élément
            commands.appendInline("#jobButtonsContent",
                    "Group { Anchor: (Width: 180, Height: 40, Margin: (Bottom: 5)); " +
                            "   Group #btn" + index + " { Anchor: (Width: 180, Height: 40); " +
                            "       Label { Text: \"" + job.getIcon() + " " + job.getName()
                            + "\"; Style: (FontSize: 14, TextColor: #ffffff, HorizontalAlignment: Center); } " +
                            "       Background: (Color: " + color + "); " +
                            "   } " +
                            "}");

            // Ensuite bind l'event
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#jobButtonsContent > Group:nth-child(" + (index + 1) + ") #btn" + index,
                    EventData.of("SELECT_JOB", job.getId()));
        }

        // Render Leaderboard (Main content)
        if (selectedJobId != null) {
            Job selectedJob = jobManager.getJob(selectedJobId);
            if (selectedJob != null) {
                commands.set("#selectedJobTitle", "Classement - " + selectedJob.getName());

                List<JobManager.Records.TopPlayerEntry> topPlayers = jobManager.getTopPlayersForJob(selectedJobId, 10);

                if (topPlayers.isEmpty()) {
                    commands.appendInline("#rankingListContent",
                            "Label { Text: \"Aucun joueur classé.\"; Style: (FontSize: 18, TextColor: #aaaaaa, HorizontalAlignment: Center); Padding: (Top: 50); }");
                } else {
                    for (int rank = 0; rank < topPlayers.size(); rank++) {
                        JobManager.Records.TopPlayerEntry entry = topPlayers.get(rank);

                        // D'abord append l'entrée
                        commands.append("#rankingListContent", "Pages/JobLeaderboardEntry.ui");

                        // Ensuite set les valeurs (rank + 1 car nth-child commence à 1)
                        String rowSelector = "#rankingListContent > Group:nth-child(" + (rank + 1) + ")";
                        commands.set(rowSelector + " #rank", "#" + (rank + 1));
                        commands.set(rowSelector + " #playerName", entry.playerName());
                        commands.set(rowSelector + " #level", "Niveau " + entry.level());
                        commands.set(rowSelector + " #xp", entry.totalXP() + " XP");
                    }
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