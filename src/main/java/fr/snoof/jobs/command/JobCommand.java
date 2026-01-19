package fr.snoof.jobs.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.EcoJobsPlugin;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.hook.PermsHook;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.ui.JobMainPage;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

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
            case "gui" -> handleGui(ref, store, playerRef);
            case "join" -> handleJoin(playerRef, arg1);
            case "leave" -> handleLeave(playerRef, arg1);
            case "info" -> handleInfo(playerRef);
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

    private void handleJoin(PlayerRef playerRef, String jobName) {
        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job join <métier>"));
            return;
        }

        if (jobManager.assignJob(playerRef, jobName)) {
            playerRef.sendMessage(MessageUtil.success("Job rejoint!"));
        } else {
            playerRef.sendMessage(MessageUtil.error("Impossible de rejoindre ce job (déjà rejoint ? ou invalide)."));
        }
    }

    private void handleLeave(PlayerRef playerRef, String jobName) {
        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job leave <métier>"));
            return;
        }

        if (jobManager.removeJob(playerRef, jobName)) {
            playerRef.sendMessage(MessageUtil.info("Job quitté."));
        } else {
            playerRef.sendMessage(MessageUtil.error("Impossible de quitter ce job (non rejoint ? ou invalide)."));
        }
    }

    private void handleInfo(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        playerRef.sendMessage(MessageUtil.info(configManager.getMessages().jobInfo));

        for (JobType type : JobType.values()) {
            int level = player.getLevel(type);
            long xp = player.getExperience(type);
            long required = jobManager.getXpRequired(level);

            String entry = String.format(configManager.getMessages().jobEntry,
                    type.getDisplayName(), level, xp, required);
            playerRef.sendMessage(MessageUtil.raw(entry, type.getColor()));
        }
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

    private void handleGui(Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        new JobMainPage(ref, store, playerRef, jobManager).open();
    }
}
