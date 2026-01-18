package fr.snoof.jobs.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.snoof.jobs.config.ConfigManager;
import fr.snoof.jobs.manager.JobManager;
import fr.snoof.jobs.model.JobReward;
import fr.snoof.jobs.model.JobType;

import javax.annotation.Nonnull;
import java.util.UUID;

public class EntityKillListener extends RefChangeSystem<EntityStore, DeathComponent> {

    private final JobManager jobManager;
    private final ConfigManager configManager;

    public EntityKillListener(JobManager jobManager, ConfigManager configManager) {
        this.jobManager = jobManager;
        this.configManager = configManager;
    }

    @Override
    @Nonnull
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef, @Nonnull DeathComponent deathComponent,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null)
            return;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource))
            return;

        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid())
            return;

        PlayerRef killerPlayerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
        if (killerPlayerRef == null)
            return;

        UUID killerUuid = killerPlayerRef.getUuid();
        String killerName = killerPlayerRef.getUsername();

        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimPlayer != null) {
            long xp = configManager.getConfig().pvpKillXp;
            double money = configManager.getConfig().pvpKillMoney;
            JobReward pvpReward = new JobReward(xp, money);
            jobManager.giveReward(killerUuid, killerName, JobType.CHAMPION, pvpReward, killerPlayerRef);
            return;
        }

        String entityType = getEntityTypeName(victimRef, store);
        if (entityType == null || entityType.isEmpty())
            return;

        String normalizedType = normalizeId(entityType);

        JobReward legendReward = configManager.getLegendMobReward(normalizedType);
        if (legendReward != null) {
            jobManager.giveReward(killerUuid, killerName, JobType.CHAMPION, legendReward, killerPlayerRef);
            return;
        }

        JobReward mobReward = configManager.getMobReward(normalizedType);
        if (mobReward != null) {
            jobManager.giveReward(killerUuid, killerName, JobType.HUNTER, mobReward, killerPlayerRef);
        }
    }

    private String getEntityTypeName(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Get the archetype of the entity reference
        Archetype<EntityStore> archetype = store.getArchetype(ref);
        if (archetype != null) {
            // Use the archetype's toString() to get a representation of the entity type
            return archetype.toString();
        }
        return null;
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, DeathComponent oldComponent,
            @Nonnull DeathComponent newComponent, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
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
