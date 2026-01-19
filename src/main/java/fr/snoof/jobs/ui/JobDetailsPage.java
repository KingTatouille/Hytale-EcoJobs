package fr.snoof.jobs.ui;

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

import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.Job;
import com.hypixel.hytale.server.core.entity.entities.Player;

public class JobDetailsPage extends InteractiveCustomUIPage<JobDetailsPage.DetailsData> {

    private final Job job;
    private final JobManager jobManager;
    private final PlayerRef playerRef;
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;

    public JobDetailsPage(Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef, Job job,
            JobManager jobManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, DetailsData.CODEC);
        this.ref = ref;
        this.store = store;
        this.playerRef = playerRef;
        this.job = job;
        this.jobManager = jobManager;
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store) {

        commands.append("Pages/JobDetailsPage.ui");

        // Basic Info
        commands.set("#jobName", job.getName());
        commands.set("#jobDescription", job.getDescription());
        commands.set("#jobIcon", job.getIcon());

        // Stats
        commands.set("#baseSalary", "Salaire: " + job.getBaseSalary() + "$");
        commands.set("#xpPerAction", "XP: " + job.getXpPerAction());
        commands.set("#requiredLevel", "Niveau requis: " + job.getRequiredLevel());

        int playerCount = jobManager.getPlayerCountForJob(job.getId());
        commands.set("#playerCount", playerCount + " Joueurs");

        // Buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#backBtn", EventData.of("ACTION", "BACK"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#joinJobBtn", EventData.of("ACTION", "JOIN"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#leaveBtn", EventData.of("ACTION", "LEAVE"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull DetailsData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null)
            return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        switch (data.action) {
            case "BACK":
                player.getPageManager().openCustomPage(ref, store, new JobMainPage(ref, store, playerRef, jobManager));
                break;
            case "JOIN":
                jobManager.assignJob(playerRef, job.getId());
                this.sendUpdate(); // Refresh pour afficher le changement
                break;
            case "LEAVE":
                jobManager.removeJob(playerRef, job.getId());
                this.sendUpdate();
                break;
        }
    }

    @SuppressWarnings("deprecation")
    public void open() {
        Player player = this.getPlayerRef().getComponent(Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(this.ref, this.store, this);
        }
    }

    public static class DetailsData {
        public static final BuilderCodec<DetailsData> CODEC = BuilderCodec
                .builder(DetailsData.class, DetailsData::new)
                .addField(new KeyedCodec<>("ACTION", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .build();
        public String action;
    }
}