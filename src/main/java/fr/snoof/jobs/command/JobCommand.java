package fr.snoof.jobs.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.EcoJobsPlugin;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.hook.PermsHook;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.ui.JobInfoPage;
import fr.snoof.jobs.ui.JobsMainPage;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JobCommand extends AbstractPlayerCommand {

    private final EcoJobsPlugin plugin;
    private final JobManager jobManager;
    private final ConfigManager configManager;

    public JobCommand(EcoJobsPlugin plugin) {
        super("job", "Commandes de métiers");
        setAllowsExtraArguments(true);
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        UUID uuid = playerRef.getUuid();

        if (!PermsHook.hasPermission(uuid, "jobs.use")) {
            playerRef.sendMessage(MessageUtil.error(configManager.getMessages().noPermission));
            return;
        }

        String input = context.getInputString().trim();
        String[] parts = input.isEmpty() ? new String[0] : input.split("\\s+");

        int startIndex = 0;
        if (parts.length > 0 && (parts[0].equalsIgnoreCase("job") || parts[0].equalsIgnoreCase("/job"))) {
            startIndex = 1;
        }

        String subcommand = parts.length > startIndex ? parts[startIndex] : "gui";
        String arg1 = parts.length > startIndex + 1 ? parts[startIndex + 1] : "";

        switch (subcommand.toLowerCase()) {
            case "admin", "create" -> handleAdmin(ref, store, playerRef);
            case "gui" -> handleGui(playerRef, ref, store, context, world);
            case "join" -> handleJoin(playerRef, arg1, context, ref, store, world);
            case "leave" -> handleLeave(playerRef, arg1, context, ref, store, world);
            case "info" -> handleInfo(playerRef, arg1, ref, store, world);
            case "stats" -> handleStats(playerRef, arg1);
            case "top" -> handleTop(playerRef, arg1);
            case "rewards" -> handleRewards(playerRef);
            case "reload" -> handleReload(playerRef);
            default -> showHelp(playerRef);
        }
    }

    private void handleAdmin(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        if (!PermsHook.hasPermission(playerRef.getUuid(), "jobs.admin")) {
            playerRef.sendMessage(MessageUtil.error(configManager.getMessages().noPermission));
            return;
        }
        // Admin GUI or commands could go here
    }

    private void handleReload(PlayerRef playerRef) {
        if (!PermsHook.hasPermission(playerRef.getUuid(), "jobs.admin")) {
            playerRef.sendMessage(MessageUtil.error(configManager.getMessages().noPermission));
            return;
        }
        jobManager.reload();
        playerRef.sendMessage(MessageUtil.success("Configuration rechargée."));
    }

    private void showHelp(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.info("=== Commandes Métiers ==="));
        playerRef.sendMessage(MessageUtil.raw("  §e/job §7- Ouvrir l'interface graphique"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job stats <métier> §7- Stats détaillées"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job top [métier] §7- Classement"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job rewards §7- Liste des récompenses"));
    }

    private void handleJoin(PlayerRef playerRef, String jobName, CommandContext context, Ref<EntityStore> ref,
            Store<EntityStore> store, World world) {
        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job join <métier>"));
            return;
        }

        if (jobManager.assignJob(playerRef, jobName)) {
            playerRef.sendMessage(MessageUtil.success("Job rejoint!"));
            refreshGuiIfOpen(context, playerRef, ref, store, world);
        } else {
            playerRef.sendMessage(MessageUtil.error("Impossible de rejoindre ce job (déjà rejoint ? ou invalide)."));
        }
    }

    private void handleLeave(PlayerRef playerRef, String jobName, CommandContext context, Ref<EntityStore> ref,
            Store<EntityStore> store, World world) {
        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job leave <métier>"));
            return;
        }

        if (jobManager.removeJob(playerRef, jobName)) {
            playerRef.sendMessage(MessageUtil.info("Job quitté."));
            refreshGuiIfOpen(context, playerRef, ref, store, world);
        } else {
            playerRef.sendMessage(MessageUtil.error("Impossible de quitter ce job (non rejoint ? ou invalide)."));
        }
    }

    private void refreshGuiIfOpen(CommandContext context, PlayerRef playerRef, Ref<EntityStore> ref,
            Store<EntityStore> store, World world) {
        Player player = context.senderAs(Player.class);
        if (player != null && player.getPageManager().getCustomPage() instanceof JobsMainPage) {
            CompletableFuture.runAsync(() -> {
                ((JobsMainPage) player.getPageManager().getCustomPage()).sendUpdate();
            }, world);
        }
    }

    private void handleInfo(PlayerRef playerRef, String jobName, Ref<EntityStore> ref, Store<EntityStore> store,
            World world) {
        UUID uuid = playerRef.getUuid();
        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        JobType targetType = null;

        // If argument provided, try to find that specific job
        if (!jobName.isEmpty()) {
            targetType = JobType.fromString(jobName);
            if (targetType == null) {
                playerRef.sendMessage(MessageUtil.error("Job introuvable: " + jobName));
                return;
            }
            if (!player.hasJoinedJob(targetType)) {
                playerRef.sendMessage(
                        MessageUtil.error("Vous n'exercez pas le métier de " + targetType.getDisplayName()));
                return;
            }
        } else {
            // No argument, find the first joined job
            for (JobType type : JobType.values()) {
                if (player.hasJoinedJob(type)) {
                    targetType = type;
                    break;
                }
            }

            if (targetType == null) {
                playerRef.sendMessage(MessageUtil.error("Vous n'avez aucun métier actif."));
                return;
            }
        }

        // Open the GUI for the target job
        final JobType finalType = targetType;
        CompletableFuture.runAsync(() -> {
            Player ethPlayer = store.getComponent(ref, Player.getComponentType());
            ethPlayer.getPageManager().openCustomPage(ref, store, new JobInfoPage(playerRef, jobManager, finalType));
        }, world);
    }

    private void handleStats(PlayerRef playerRef, String jobName) {
        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job stats <métier>"));
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error("Job invalide."));
            return;
        }

        UUID uuid = playerRef.getUuid();
        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        int level = player.getLevel(type);
        long xp = player.getExperience(type);
        long required = jobManager.getXpRequired(level);
        long totalXp = player.getTotalExperience(type);
        double progress = jobManager.getProgressPercent(uuid, type);

        playerRef.sendMessage(MessageUtil.info(String.format("Stats: %s", type.getDisplayName())));
        playerRef.sendMessage(MessageUtil.raw(String.format("Niveau: %d", level), type.getColor()));
        playerRef.sendMessage(MessageUtil.raw(String.format("XP: %d / %d", xp, required)));
        playerRef.sendMessage(MessageUtil.raw(String.format("Total XP: %d", totalXp)));
        playerRef.sendMessage(MessageUtil.raw(String.format("Progression: %.1f%%", progress)));
    }

    private void handleTop(PlayerRef playerRef, String jobName) {
        playerRef.sendMessage(MessageUtil.info("Voir le classement dans l'interface /job"));
    }

    private void handleRewards(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.info("=== Récompenses par Métier ==="));
        for (JobType type : JobType.values()) {
            playerRef.sendMessage(MessageUtil.raw("§6" + type.getDisplayName() + " §7- " + type.getDescription()));
        }
    }

    private void handleGui(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, CommandContext context,
            World world) {
        Player player = context.senderAs(Player.class);
        player.getWorldMapTracker().tick(0);

        CompletableFuture.runAsync(() -> {
            player.getPageManager().openCustomPage(ref, store, new JobsMainPage(playerRef, jobManager));
        }, world);

    }
}
