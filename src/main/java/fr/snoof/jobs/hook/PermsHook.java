package fr.snoof.jobs.hook;

import fr.snoof.perms.api.PermsAPI;

import java.util.UUID;

public final class PermsHook {

    private PermsHook() {
    }

    public static boolean isAvailable() {
        try {
            return PermsAPI.isAvailable();
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean hasPermission(UUID uuid, String permission) {
        if (!isAvailable())
            return true; // Allow if EcoPerms not available
        return PermsAPI.hasPermission(uuid, permission);
    }

    public static String getPrefix(UUID uuid) {
        if (!isAvailable())
            return "";
        return PermsAPI.getPrefix(uuid);
    }

    public static String getGroupName(UUID uuid) {
        if (!isAvailable())
            return "default";
        return PermsAPI.getGroupName(uuid);
    }
}
