package fr.snoof.jobs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.snoof.jobs.command.JobAdminCommand;
import fr.snoof.jobs.command.JobCommand;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.listener.BlockBreakListener;
import fr.snoof.jobs.listener.BlockInteractListener;
import fr.snoof.jobs.listener.BlockPlaceListener;
import fr.snoof.jobs.listener.CraftingListener;
import fr.snoof.jobs.listener.EntityKillListener;
import fr.snoof.jobs.manager.DataManager;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.manager.PlacedBlockManager;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.logging.Level;

public class EcoJobsPlugin extends JavaPlugin {

    private static EcoJobsPlugin instance;

    private ConfigManager configManager;
    private JobManager jobManager;
    private PlacedBlockManager placedBlockManager;
    private DataManager dataManager;

    private BlockBreakListener blockBreakListener;
    private EntityKillListener entityKillListener;
    private CraftingListener craftingListener;
    private BlockInteractListener blockInteractListener;
    private BlockPlaceListener blockPlaceListener;

    public EcoJobsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        Path dataFolder = Path.of("plugins", "EcoJobs");

        configManager = new ConfigManager(dataFolder, getLogger());
        configManager.load();

        jobManager = new JobManager(configManager);

        placedBlockManager = new PlacedBlockManager(dataFolder, getLogger());
        placedBlockManager.load();

        dataManager = new DataManager(dataFolder, getLogger(), jobManager);
        dataManager.init(configManager.getConfig().autoSaveInterval);

        blockBreakListener = new BlockBreakListener(jobManager, configManager, placedBlockManager);
        blockInteractListener = new BlockInteractListener(jobManager, configManager, placedBlockManager);
        entityKillListener = new EntityKillListener(jobManager, configManager);
        craftingListener = new CraftingListener(jobManager, configManager);
        blockPlaceListener = new BlockPlaceListener(placedBlockManager);

        getCommandRegistry().registerCommand(new JobCommand(this));
        getCommandRegistry().registerCommand(new JobAdminCommand(this));

        getEntityStoreRegistry().registerSystem(blockBreakListener);
        getEntityStoreRegistry().registerSystem(blockInteractListener);
        getEntityStoreRegistry().registerSystem(craftingListener);
        getEntityStoreRegistry().registerSystem(entityKillListener);
        getEntityStoreRegistry().registerSystem(blockPlaceListener);

        // Register BlockPlaceListener later once created

        getLogger().at(Level.INFO).log("EcoJobs v" + configManager.getConfig().version + " chargé avec succès!");
    }

    @Override
    protected void shutdown() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        if (placedBlockManager != null) {
            placedBlockManager.save();
        }
        if (configManager != null) {
            configManager.save();
        }
        getLogger().at(Level.INFO).log("EcoJobs désactivé.");
    }

    public PlacedBlockManager getPlacedBlockManager() {
        return placedBlockManager;
    }

    public static EcoJobsPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}
