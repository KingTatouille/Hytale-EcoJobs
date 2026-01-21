package fr.snoof.jobs.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.manager.PlacedBlockManager;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.UUID;

public class BlockBreakListener extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final PlacedBlockManager placedBlockManager;

    public BlockBreakListener(JobManager jobManager, ConfigManager configManager,
            PlacedBlockManager placedBlockManager) {
        super(BreakBlockEvent.class);
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.placedBlockManager = placedBlockManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComp == null)
            return;

        UUID playerUuid = uuidComp.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null)
            return;

        String blockId = event.getBlockType().getId();
        // Fallback: If event says "Empty" or null, try to retrieve from
        // PlacedBlockManager
        int x = event.getTargetBlock().getX();
        int y = event.getTargetBlock().getY();
        int z = event.getTargetBlock().getZ();

        UUID placerUuid = placedBlockManager.getPlacer(x, y, z);
        String placedBlockId = placedBlockManager.getPlacedBlockId(x, y, z);

        if ((blockId == null || blockId.equals("Empty") || blockId.isEmpty()) && placedBlockId != null) {
            blockId = placedBlockId;
        }

        if (blockId == null || blockId.isEmpty() || blockId.equals("Empty")) {
            if (placerUuid != null)
                placedBlockManager.removeBlock(x, y, z);
            return;
        }

        String normalizedBlockId = normalizeId(blockId);
        String playerName = playerRef.getUsername();

        if (placerUuid != null) {
            placedBlockManager.removeBlock(x, y, z);
        }

        JobReward minerReward = configManager.getBlockReward(normalizedBlockId);
        if (minerReward != null) {
            jobManager.giveReward(playerUuid, playerName, JobType.MINER, minerReward, playerRef);
            return;
        }

        JobReward woodReward = configManager.getWoodReward(normalizedBlockId);
        if (woodReward != null) {
            jobManager.giveReward(playerUuid, playerName, JobType.LUMBERJACK, woodReward, playerRef);
            return;
        }

        JobReward cropReward = configManager.getCropReward(normalizedBlockId);
        if (cropReward != null) {
            // Only reward if placed by the player (cultivated)
            if (placerUuid != null && placerUuid.equals(playerUuid)) {
                jobManager.giveReward(playerUuid, playerName, JobType.FARMER, cropReward, playerRef);
            }
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
