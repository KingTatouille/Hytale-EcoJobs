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
import fr.snoof.jobs.ui.JobAdminGui;
import fr.snoof.jobs.ui.JobsMainGui;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.List;
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
            case "gui", "menu", "" -> handleGui(ref, store, playerRef);
            case "admin" -> handleAdmin(ref, store, playerRef);
            case "join" -> handleJoin(playerRef, arg1);
            case "leave" -> handleLeave(playerRef, arg1);
            case "list" -> handleList(playerRef);
            case "info" -> handleInfo(playerRef);
            case "stats" -> handleStats(playerRef, arg1);
            case "top" -> handleTop(playerRef, arg1);
            case "rewards" -> handleRewards(playerRef);
            default -> showHelp(playerRef);
        }
    }

    private void handleGui(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new JobsMainGui(playerRef));
    }

    private void handleAdmin(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        if (!PermsHook.hasPermission(playerRef.getUuid(), "jobs.admin")) {
            playerRef.sendMessage(MessageUtil.error(configManager.getMessages().noPermission));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new JobAdminGui(playerRef));
    }

    private void showHelp(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.info("=== Commandes Métiers ==="));
        playerRef.sendMessage(MessageUtil.raw("  §e/job §7ou §e/job gui §7- Ouvrir l'interface graphique"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job admin §7- Panel d'administration (admin)"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job join <métier> §7- Rejoindre un métier"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job leave <métier> §7- Quitter un métier"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job list §7- Voir vos métiers actifs"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job info §7- Voir tous vos niveaux"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job stats <métier> §7- Stats détaillées"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job top [métier] §7- Classement"));
        playerRef.sendMessage(MessageUtil.raw("  §e/job rewards §7- Liste des récompenses"));
    }

    private void handleJoin(PlayerRef playerRef, String jobName) {
        UUID uuid = playerRef.getUuid();
        var msg = configManager.getMessages();

        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job join <métier>"));
            showJobList(playerRef);
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(msg.invalidJob, jobName)));
            showJobList(playerRef);
            return;
        }

        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        // Check if already joined
        if (player.hasJoinedJob(type)) {
            playerRef.sendMessage(MessageUtil.error(msg.jobAlreadyJoined));
            return;
        }

        // Check max jobs limit
        int maxJobs = configManager.getConfig().maxJobs;
        if (player.getJoinedJobCount() >= maxJobs) {
            playerRef.sendMessage(MessageUtil.error(String.format(msg.jobLimitReached, maxJobs)));
            return;
        }

        // Join the job
        player.joinJob(type);
        playerRef.sendMessage(MessageUtil.success(String.format(msg.jobJoined, type.getDisplayName())));
    }

    private void handleLeave(PlayerRef playerRef, String jobName) {
        UUID uuid = playerRef.getUuid();
        var msg = configManager.getMessages();

        if (jobName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /job leave <métier>"));
            handleList(playerRef);
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(msg.invalidJob, jobName)));
            showJobList(playerRef);
            return;
        }

        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        // Check if joined
        if (!player.hasJoinedJob(type)) {
            playerRef.sendMessage(MessageUtil.error(msg.jobNotJoined));
            return;
        }

        // Leave the job
        player.leaveJob(type);
        playerRef.sendMessage(MessageUtil.info(String.format(msg.jobLeft, type.getDisplayName())));
    }

    private void handleList(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        var msg = configManager.getMessages();
        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());

        var joinedJobs = player.getJoinedJobs();
        if (joinedJobs.isEmpty()) {
            playerRef.sendMessage(MessageUtil.info(msg.noJobsJoined));
            playerRef.sendMessage(MessageUtil.raw("§7Utilisez §e/job join <métier> §7pour en rejoindre un."));
            showJobList(playerRef);
            return;
        }

        playerRef.sendMessage(MessageUtil.info(msg.yourJobs));
        for (JobType type : joinedJobs) {
            int level = player.getLevel(type);
            long xp = player.getExperience(type);
            long required = jobManager.getXpRequired(level);
            String entry = String.format("  %s §7- Niveau §e%d §7(§b%d§7/§b%d §7XP)",
                    type.getDisplayName(), level, xp, required);
            playerRef.sendMessage(MessageUtil.raw(entry, type.getColor()));
        }

        int maxJobs = configManager.getConfig().maxJobs;
        playerRef.sendMessage(MessageUtil.raw(String.format("§7Métiers: §e%d§7/§6%d",
                joinedJobs.size(), maxJobs)));
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
            showJobList(playerRef);
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidJob, jobName)));
            showJobList(playerRef);
            return;
        }

        UUID uuid = playerRef.getUuid();
        JobPlayer player = jobManager.getOrCreatePlayer(uuid, playerRef.getUsername());
        var msg = configManager.getMessages();

        int level = player.getLevel(type);
        long xp = player.getExperience(type);
        long required = jobManager.getXpRequired(level);
        long totalXp = player.getTotalExperience(type);
        double progress = jobManager.getProgressPercent(uuid, type);

        playerRef.sendMessage(MessageUtil.info(String.format(msg.statsHeader, type.getDisplayName())));
        playerRef.sendMessage(MessageUtil.raw(String.format(msg.statsLevel, level), type.getColor()));
        playerRef.sendMessage(MessageUtil.raw(String.format(msg.statsXp, xp, required)));
        playerRef.sendMessage(MessageUtil.raw(String.format(msg.statsTotal, totalXp)));
        playerRef.sendMessage(MessageUtil.raw(String.format(msg.statsProgress, progress)));
    }

    private void handleTop(PlayerRef playerRef, String jobName) {
        JobType type = JobType.MINER; // Default
        if (!jobName.isEmpty()) {
            type = JobType.fromString(jobName);
            if (type == null) {
                playerRef.sendMessage(MessageUtil.error(
                        String.format(configManager.getMessages().invalidJob, jobName)));
                showJobList(playerRef);
                return;
            }
        }

        var msg = configManager.getMessages();
        List<JobPlayer> topPlayers = jobManager.getTopPlayers(type, 10);

        playerRef.sendMessage(MessageUtil.info(String.format(msg.topHeader, type.getDisplayName())));

        if (topPlayers.isEmpty()) {
            playerRef.sendMessage(MessageUtil.raw("  §7Aucun joueur classé"));
            return;
        }

        int rank = 1;
        for (JobPlayer player : topPlayers) {
            playerRef.sendMessage(MessageUtil.raw(String.format(msg.topEntry,
                    rank, player.getName(), player.getLevel(type))));
            rank++;
        }
    }

    private void handleRewards(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.info("=== Récompenses par Métier ==="));

        for (JobType type : JobType.values()) {
            playerRef.sendMessage(MessageUtil.raw(""));
            playerRef.sendMessage(MessageUtil.raw("§6" + type.getDisplayName() + " §7- " + type.getDescription()));
        }

        playerRef.sendMessage(MessageUtil.raw(""));
        playerRef.sendMessage(MessageUtil.raw("§7Les récompenses dépendent des blocs/mobs/crafts."));
        playerRef.sendMessage(MessageUtil.raw("§7Consultez la documentation pour les détails."));
    }

    private void showJobList(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.raw("§7Métiers disponibles:"));
        for (JobType type : JobType.values()) {
            playerRef.sendMessage(MessageUtil.raw("  §e" + type.name() + " §7(" + type.getDisplayName() + ")"));
        }
    }
}
