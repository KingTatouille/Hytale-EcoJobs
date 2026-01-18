package fr.snoof.jobs.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.EcoJobsPlugin;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.hook.PermsHook;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobPlayer;
import fr.snoof.jobs.model.JobType;
import fr.snoof.jobs.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

public class JobAdminCommand extends AbstractPlayerCommand {

    private final EcoJobsPlugin plugin;
    private final JobManager jobManager;
    private final ConfigManager configManager;

    public JobAdminCommand(EcoJobsPlugin plugin) {
        super("jobadmin", "Commandes admin des métiers");
        setAllowsExtraArguments(true);
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        UUID uuid = playerRef.getUuid();

        if (!PermsHook.hasPermission(uuid, "jobs.admin")) {
            playerRef.sendMessage(MessageUtil.error(configManager.getMessages().noPermission));
            return;
        }

        String input = context.getInputString().trim();
        String[] parts = input.isEmpty() ? new String[0] : input.split("\\s+");

        int startIndex = 0;
        if (parts.length > 0 && (parts[0].equalsIgnoreCase("jobadmin") || parts[0].equalsIgnoreCase("/jobadmin"))) {
            startIndex = 1;
        }

        String subcommand = parts.length > startIndex ? parts[startIndex] : "help";
        String arg1 = parts.length > startIndex + 1 ? parts[startIndex + 1] : "";
        String arg2 = parts.length > startIndex + 2 ? parts[startIndex + 2] : "";
        String arg3 = parts.length > startIndex + 3 ? parts[startIndex + 3] : "";

        switch (subcommand.toLowerCase()) {
            case "setlevel" -> handleSetLevel(playerRef, arg1, arg2, arg3);
            case "setxp" -> handleSetXp(playerRef, arg1, arg2, arg3);
            case "addxp" -> handleAddXp(playerRef, arg1, arg2, arg3);
            case "reset" -> handleReset(playerRef, arg1, arg2);
            case "reload" -> handleReload(playerRef);
            case "info" -> handleInfo(playerRef, arg1);
            default -> showHelp(playerRef);
        }
    }

    private void showHelp(PlayerRef playerRef) {
        playerRef.sendMessage(MessageUtil.info("=== Commandes Admin Métiers ==="));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin setlevel <joueur> <métier> <niveau>"));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin setxp <joueur> <métier> <xp>"));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin addxp <joueur> <métier> <xp>"));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin reset <joueur> [métier]"));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin reload §7- Recharger config"));
        playerRef.sendMessage(MessageUtil.raw("  §e/jobadmin info <joueur> §7- Stats joueur"));
    }

    private void handleSetLevel(PlayerRef playerRef, String playerName, String jobName, String levelStr) {
        if (playerName.isEmpty() || jobName.isEmpty() || levelStr.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /jobadmin setlevel <joueur> <métier> <niveau>"));
            return;
        }

        PlayerRef target = findPlayer(playerName);
        if (target == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().playerNotFound, playerName)));
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidJob, jobName)));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidNumber, levelStr)));
            return;
        }

        jobManager.setLevel(target.getUuid(), target.getUsername(), type, level);
        playerRef.sendMessage(MessageUtil.success(String.format(
                configManager.getMessages().adminSetLevel, target.getUsername(), type.getDisplayName(), level)));
    }

    private void handleSetXp(PlayerRef playerRef, String playerName, String jobName, String xpStr) {
        if (playerName.isEmpty() || jobName.isEmpty() || xpStr.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /jobadmin setxp <joueur> <métier> <xp>"));
            return;
        }

        PlayerRef target = findPlayer(playerName);
        if (target == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().playerNotFound, playerName)));
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidJob, jobName)));
            return;
        }

        long xp;
        try {
            xp = Long.parseLong(xpStr);
        } catch (NumberFormatException e) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidNumber, xpStr)));
            return;
        }

        jobManager.setExperience(target.getUuid(), target.getUsername(), type, xp);
        playerRef.sendMessage(MessageUtil.success(String.format(
                configManager.getMessages().adminSetXp, target.getUsername(), type.getDisplayName(), xp)));
    }

    private void handleAddXp(PlayerRef playerRef, String playerName, String jobName, String xpStr) {
        if (playerName.isEmpty() || jobName.isEmpty() || xpStr.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /jobadmin addxp <joueur> <métier> <xp>"));
            return;
        }

        PlayerRef target = findPlayer(playerName);
        if (target == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().playerNotFound, playerName)));
            return;
        }

        JobType type = JobType.fromString(jobName);
        if (type == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidJob, jobName)));
            return;
        }

        long xp;
        try {
            xp = Long.parseLong(xpStr);
        } catch (NumberFormatException e) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().invalidNumber, xpStr)));
            return;
        }

        jobManager.addXp(target.getUuid(), target.getUsername(), type, xp, target);
        playerRef.sendMessage(MessageUtil.success(String.format(
                configManager.getMessages().adminAddXp, xp, target.getUsername(), type.getDisplayName())));
    }

    private void handleReset(PlayerRef playerRef, String playerName, String jobName) {
        if (playerName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /jobadmin reset <joueur> [métier]"));
            return;
        }

        PlayerRef target = findPlayer(playerName);
        if (target == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().playerNotFound, playerName)));
            return;
        }

        JobType type = null;
        if (!jobName.isEmpty()) {
            type = JobType.fromString(jobName);
            if (type == null) {
                playerRef.sendMessage(MessageUtil.error(
                        String.format(configManager.getMessages().invalidJob, jobName)));
                return;
            }
        }

        jobManager.resetPlayer(target.getUuid(), type);
        playerRef.sendMessage(MessageUtil.success(String.format(
                configManager.getMessages().adminReset, target.getUsername())));
    }

    private void handleReload(PlayerRef playerRef) {
        configManager.load();
        playerRef.sendMessage(MessageUtil.success(configManager.getMessages().adminReload));
    }

    private void handleInfo(PlayerRef playerRef, String playerName) {
        if (playerName.isEmpty()) {
            playerRef.sendMessage(MessageUtil.error("Usage: /jobadmin info <joueur>"));
            return;
        }

        PlayerRef target = findPlayer(playerName);
        if (target == null) {
            playerRef.sendMessage(MessageUtil.error(
                    String.format(configManager.getMessages().playerNotFound, playerName)));
            return;
        }

        JobPlayer player = jobManager.getOrCreatePlayer(target.getUuid(), target.getUsername());

        playerRef.sendMessage(MessageUtil.info(String.format(
                configManager.getMessages().adminInfo, target.getUsername())));

        for (JobType type : JobType.values()) {
            int level = player.getLevel(type);
            long xp = player.getExperience(type);
            long required = jobManager.getXpRequired(level);
            long total = player.getTotalExperience(type);

            playerRef.sendMessage(MessageUtil.raw(String.format(
                    "  §e%s §7- Nv.§6%d §7(§b%d§7/§b%d §7XP) Total: §b%d",
                    type.getDisplayName(), level, xp, required, total)));
        }
    }

    private PlayerRef findPlayer(String name) {
        for (PlayerRef pr : Universe.get().getPlayers()) {
            if (pr.getUsername().equalsIgnoreCase(name)) {
                return pr;
            }
        }
        return null;
    }
}
