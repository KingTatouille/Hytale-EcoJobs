package fr.snoof.jobs.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.snoof.jobs.manager.JobManager;

import javax.annotation.Nonnull;

public class JobsUiHelper {

    public static void setupBar(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        commands.append("Pages/JobsNavBar.ui");

        // Bind events for the buttons defined in JobsNavBar.ui
        events.addEventBinding(CustomUIEventBindingType.Activating, "#nav-btn-jobs", EventData.of("NAV", "JOBS"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#nav-btn-stats", EventData.of("NAV", "STATS"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#nav-btn-leaderboard",
                EventData.of("NAV", "LEADERBOARD"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#nav-btn-close", EventData.of("NAV", "CLOSE"));
    }

    public static boolean handleData(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store, @Nonnull JobManager jobManager, String navAction, Runnable onClose) {
        if (navAction == null)
            return false;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return false;

        switch (navAction) {
            case "JOBS":
                player.getPageManager().openCustomPage(ref, store, new JobMainPage(ref, store, playerRef, jobManager));
                return true;
            case "STATS":
                player.getPageManager().openCustomPage(ref, store, new JobStatsPage(playerRef, jobManager));
                return true;
            case "LEADERBOARD":
                player.getPageManager().openCustomPage(ref, store, new JobLeaderboardPage(playerRef, jobManager));
                return true;
            case "CLOSE":
                if (onClose != null) {
                    onClose.run();
                }
                return true;
            default:
                return false;
        }
    }
}
