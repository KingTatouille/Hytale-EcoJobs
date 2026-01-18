package fr.snoof.jobs.hook;

import fr.snoof.economy.api.EconomyAPI;

import java.util.UUID;

public final class EconomyHook {

    private EconomyHook() {
    }

    public static boolean isAvailable() {
        try {
            return EconomyAPI.isAvailable();
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static void addBalance(UUID uuid, double amount) {
        if (!isAvailable() || amount <= 0)
            return;
        EconomyAPI.addBalance(uuid, amount);
    }

    public static double getBalance(UUID uuid) {
        if (!isAvailable())
            return 0;
        return EconomyAPI.getBalance(uuid);
    }

    public static String formatAmount(double amount) {
        if (!isAvailable())
            return String.format("%.2f$", amount);
        return EconomyAPI.formatAmount(amount);
    }

    public static String getCurrencySymbol() {
        if (!isAvailable())
            return "$";
        return EconomyAPI.getCurrencySymbol();
    }
}
