package fr.snoof.jobs.ui;

import javax.annotation.Nonnull;

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

public class JobDetailsPage extends InteractiveCustomUIPage<Void> {

    private final Job job;
    private final JobManager jobManager;
    private final PlayerRef playerRef;
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;

    public JobDetailsPage(Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef, Job job,
            JobManager jobManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, null);
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

        commands.append("Common/UI/Custom/Pages/JobDetailsPage.ui");

        // Basic Info
        commands.set("#job-name", job.getName());
        commands.set("#job-description", job.getDescription());
        commands.set("#job-icon", job.getIcon());

        // Stats
        commands.set("#base-salary", "Salaire: " + job.getBaseSalary() + "$");
        commands.set("#xp-per-action", "XP: " + job.getXpPerAction());
        commands.set("#required-level", "Niveau requis: " + job.getRequiredLevel());

        int playerCount = jobManager.getPlayerCountForJob(job.getId());
        commands.set("#player-count", playerCount + " Joueurs");

        // Buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#back-btn", EventData.of("ACTION", "BACK"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#join-btn", EventData.of("ACTION", "JOIN"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#leave-btn", EventData.of("ACTION", "LEAVE"));
    }

    @SuppressWarnings("deprecation")
    public void open() {
        Player player = this.getPlayerRef().getComponent(Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(this.ref, this.store, this);
        }
    }
}
