package fr.snoof.jobs.ui;

import java.util.List;

import javax.annotation.Nonnull;

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
import com.hypixel.hytale.server.core.entity.entities.Player;

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.Job;

public class JobMainPage extends InteractiveCustomUIPage<JobMainPage.MainPageData> {

    private final JobManager jobManager;
    private final PlayerRef playerRef;
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;
    private final List<Job> jobs;
    private int currentPage = 0;
    private static final int JOBS_PER_PAGE = 4;

    public JobMainPage(Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef, JobManager jobManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, MainPageData.CODEC);
        this.ref = ref;
        this.store = store;
        this.playerRef = playerRef;
        this.jobManager = jobManager;
        this.jobs = jobManager.getAllJobs();
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store) {

        // Add Navigation Bar
        JobsUiHelper.setupBar(ref, commands, events, store);

        // Load Main Page UI
        commands.append("Pages/JobsMainPage.ui");

        // Render Jobs
        renderCurrentPage(commands, events);

        // Pagination Events
        events.addEventBinding(CustomUIEventBindingType.Activating, "#prev-page-btn",
                EventData.of("ACTION", "PREV_PAGE"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#next-page-btn",
                EventData.of("ACTION", "NEXT_PAGE"));
    }

    private void renderCurrentPage(UICommandBuilder commands, UIEventBuilder events) {
        // Clear container
        commands.clear("#jobs-container");

        int startIdx = currentPage * JOBS_PER_PAGE;
        int endIdx = Math.min(startIdx + JOBS_PER_PAGE, jobs.size());

        for (int i = startIdx; i < endIdx; i++) {
            Job job = jobs.get(i);
            String cardSelector = "#jobs-container > Group:nth-child(" + (i - startIdx + 1) + ")";

            commands.append("#jobs-container", "Pages/JobCard.ui");

            // Fill Job Data
            commands.set(cardSelector + " #job-icon", job.getIcon());
            commands.set(cardSelector + " #job-name", job.getName());
            commands.set(cardSelector + " #job-description", job.getDescription());
            commands.set(cardSelector + " #base-salary", "Salaire: " + job.getBaseSalary() + "$/h");
            commands.set(cardSelector + " #xp-per-action", "XP: +" + job.getXpPerAction() + " par action");

            int playerCount = jobManager.getPlayerCountForJob(job.getId());
            commands.set(cardSelector + " #player-count", playerCount + " joueur(s)");

            // Button Events
            events.addEventBinding(CustomUIEventBindingType.Activating, cardSelector + " #view-details-btn",
                    EventData.of("JOB_DETAILS", job.getId()));
            events.addEventBinding(CustomUIEventBindingType.Activating, cardSelector + " #join-btn",
                    EventData.of("JOB_JOIN", job.getId()));
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull MainPageData data) {
        super.handleDataEvent(ref, store, data);

        // Handle Navigation
        if (JobsUiHelper.handleData(playerRef, ref, store, jobManager, data.nav, this::close)) {
            return;
        }

        // Handle Pagination
        if ("PREV_PAGE".equals(data.action)) {
            if (currentPage > 0) {
                currentPage--;
                this.sendUpdate();
            }
        } else if ("NEXT_PAGE".equals(data.action)) {
            if ((currentPage + 1) * JOBS_PER_PAGE < jobs.size()) {
                currentPage++;
                this.sendUpdate();
            }
        }

        // Handle Job Actions
        else if (data.jobDetails != null) {
            Job job = jobManager.getJob(data.jobDetails);
            if (job != null)
                openJobDetails(ref, store, job);
        } else if (data.jobJoin != null) {
            Job job = jobManager.getJob(data.jobJoin);
            if (job != null)
                joinJob(job);
        }
    }

    private void openJobDetails(Ref<EntityStore> ref, Store<EntityStore> store, Job job) {
        new JobDetailsPage(ref, store, getPlayerRef(), job, jobManager).open();
        // Do not close, as we want to return here? Actually usually we replace the
        // page.
        // But JobDetailsPage will open on top? If it's CanDismiss it might stack?
        // AdminUI usually replaces. Let's just open the new one.
    }

    private void joinJob(Job job) {
        jobManager.assignJob(getPlayerRef(), job.getId());
        // Refresh to show joined status if we wanted, or just stay here.
        this.sendUpdate();
    }

    @SuppressWarnings("deprecation")
    public void open() {
        Player player = this.getPlayerRef().getComponent(Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(this.ref, this.store, this);
        }
    }

    public static class MainPageData {
        public static final BuilderCodec<MainPageData> CODEC = BuilderCodec
                .builder(MainPageData.class, MainPageData::new)
                .addField(new KeyedCodec<>("NAV", Codec.STRING), (d, v) -> d.nav = v, d -> d.nav)
                .addField(new KeyedCodec<>("ACTION", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("JOB_DETAILS", Codec.STRING), (d, v) -> d.jobDetails = v, d -> d.jobDetails)
                .addField(new KeyedCodec<>("JOB_JOIN", Codec.STRING), (d, v) -> d.jobJoin = v, d -> d.jobJoin)
                .build();

        public String nav;
        public String action;
        public String jobDetails;
        public String jobJoin;
    }
}
