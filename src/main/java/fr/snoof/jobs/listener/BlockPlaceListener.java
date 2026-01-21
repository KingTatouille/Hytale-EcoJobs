package fr.snoof.jobs.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.manager.PlacedBlockManager;

import javax.annotation.Nonnull;
import java.util.UUID;

public class BlockPlaceListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private final PlacedBlockManager placedBlockManager;

    public BlockPlaceListener(PlacedBlockManager placedBlockManager) {
        super(PlaceBlockEvent.class);
        this.placedBlockManager = placedBlockManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event) {

        UUIDComponent uuidComp = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uuidComp == null)
            return;

        UUID playerUuid = uuidComp.getUuid();

        int x = event.getTargetBlock().getX();
        int y = event.getTargetBlock().getY();
        int z = event.getTargetBlock().getZ();

        String blockId = "unknown";
        if (event.getItemInHand() != null) {
            try {
                // Reflection to find the ID
                Object itemStack = event.getItemInHand();
                if (itemStack != null) {
                    // itemStack.getItem()
                    java.lang.reflect.Method getItemMethod = itemStack.getClass().getMethod("getItem");
                    Object item = getItemMethod.invoke(itemStack);

                    if (item != null) {
                        try {
                            java.lang.reflect.Method getIdMethod = item.getClass().getMethod("getId");
                            Object id = getIdMethod.invoke(item);
                            if (id != null) {
                                blockId = id.toString();
                            }
                        } catch (NoSuchMethodException ignored) {
                        }

                        if (blockId.equals("unknown")) {
                            try {
                                java.lang.reflect.Method getNameMethod = item.getClass().getMethod("getName");
                                Object name = getNameMethod.invoke(item);
                                if (name != null) {
                                    blockId = name.toString();
                                }
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback: parse toString() for itemId=... only if reflection failed
                try {
                    String raw = event.getItemInHand().toString();
                    if (raw.contains("itemId=")) {
                        int start = raw.indexOf("itemId=") + 7;
                        int end = raw.indexOf(",", start);
                        if (end == -1)
                            end = raw.indexOf("}", start);
                        if (end != -1) {
                            blockId = raw.substring(start, end);
                        } else {
                            blockId = raw;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // System.out.println("[EcoJobs DEBUG] Placed Block at " + x + "," + y + "," + z
        // + " by " + playerUuid + " ID: " + blockId);
        placedBlockManager.addBlock(x, y, z, playerUuid, blockId);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
