package fr.snoof.jobs.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.UUID;

public class CraftingListener extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    private final JobManager jobManager;
    private final ConfigManager configManager;

    public CraftingListener(JobManager jobManager, ConfigManager configManager) {
        super(CraftRecipeEvent.Post.class);
        this.jobManager = jobManager;
        this.configManager = configManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull CraftRecipeEvent.Post event) {

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComp == null)
            return;

        UUID playerUuid = uuidComp.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null)
            return;

        String itemId = event.getCraftedRecipe().getId();
        if (itemId == null || itemId.isEmpty())
            return;

        String normalizedId = normalizeId(itemId);
        String playerName = playerRef.getUsername();
        int amount = event.getQuantity();

        JobReward craftReward = configManager.getCraftReward(normalizedId);
        if (craftReward != null) {
            JobReward scaledReward = new JobReward(
                    craftReward.getXp() * amount,
                    craftReward.getMoney() * amount);
            jobManager.giveReward(playerUuid, playerName, JobType.BLACKSMITH, scaledReward, playerRef);
            return;
        }

        JobReward furnitureReward = configManager.getFurnitureReward(normalizedId);
        if (furnitureReward != null) {
            JobReward scaledReward = new JobReward(
                    furnitureReward.getXp() * amount,
                    furnitureReward.getMoney() * amount);
            jobManager.giveReward(playerUuid, playerName, JobType.LUMBERJACK, scaledReward, playerRef);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private String normalizeId(String id) {
        if (id == null)
            return "";
        int colonIndex = id.indexOf(':');
        if (colonIndex >= 0) {
            return id.substring(colonIndex + 1).toLowerCase();
        }
        return id.toLowerCase();
    }
}
