package fr.snoof.jobs.hook;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.UUID;

public final class PermsHook {

    private PermsHook() {
    }

    public static boolean isAvailable() {
        return true;
    }

    public static boolean hasPermission(UUID uuid, String permission) {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef.getUuid().equals(uuid)) {
                Player player = playerRef.getComponent(Player.getComponentType());
                if (player != null) {
                    return player.hasPermission(permission);
                }
            }
        }
        return false;
    }

    public static String getPrefix(UUID uuid) {
        return "";
    }

    public static String getGroupName(UUID uuid) {
        return "default";
    }
}
