package fr.snoof.jobs.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
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

public class BlockInteractListener extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    private final JobManager jobManager;
    private final ConfigManager configManager;
    private final PlacedBlockManager placedBlockManager;

    public BlockInteractListener(JobManager jobManager, ConfigManager configManager,
            PlacedBlockManager placedBlockManager) {
        super(InteractivelyPickupItemEvent.class);
        this.jobManager = jobManager;
        this.configManager = configManager;
        this.placedBlockManager = placedBlockManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractivelyPickupItemEvent event) {

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComp == null)
            return;

        UUID playerUuid = uuidComp.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null)
            return;

        String itemId = "unknown";
        try {
            // Use reflection to get item name/id from event.getItemStack()
            Object itemStack = event.getItemStack();
            if (itemStack != null) {
                // itemStack.getItem()
                java.lang.reflect.Method getItemMethod = itemStack.getClass().getMethod("getItem");
                Object item = getItemMethod.invoke(itemStack);

                if (item != null) {
                    // Try getId() first
                    try {
                        java.lang.reflect.Method getIdMethod = item.getClass().getMethod("getId");
                        Object idObj = getIdMethod.invoke(item);
                        if (idObj != null) {
                            itemId = idObj.toString();
                        }
                    } catch (NoSuchMethodException ignored) {
                    }

                    // If still unknown, try getName() as fallback or if specifically requested
                    if (itemId.equals("unknown")) {
                        try {
                            java.lang.reflect.Method getNameMethod = item.getClass().getMethod("getName");
                            Object name = getNameMethod.invoke(item);
                            if (name != null) {
                                itemId = name.toString();
                            }
                        } catch (NoSuchMethodException ignored) {
                        }
                    }

                    // Fallback to toString() if mostly nothing worked
                    if (itemId.equals("unknown")) {
                        itemId = item.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Normalize
        String normalizedId = normalizeId(itemId);

        // Check for Crop Reward
        JobReward cropReward = configManager.getCropReward(normalizedId);
        if (cropReward != null) {
            jobManager.giveReward(playerUuid, playerRef.getUsername(), JobType.FARMER, cropReward, playerRef);
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
