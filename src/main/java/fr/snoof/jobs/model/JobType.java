package fr.snoof.jobs.model;

import fr.snoof.jobs.config.ConfigManager;

public enum JobType {
    FARMER("Fermier", "Producteur agricole", "#2ecc71", "Weapon_Tool_Hoe_Iron"),
    HUNTER("Chasseur", "Chasse et collecte", "#e67e22", "Weapon_Bow_Wood"),
    CHAMPION("Champion", "Combat PvP/PvE", "#e74c3c", "Weapon_Sword_Iron"),
    MINER("Mineur", "Extraction minière", "#95a5a6", "Weapon_Tool_Pickaxe_Iron"),
    BLACKSMITH("Forgeron", "Craft technique", "#f39c12", "Block_Anvil"),
    LUMBERJACK("Bûcheron", "Travail du bois", "#27ae60", "Weapon_Tool_Axe_Iron");

    private final String displayName;
    private final String description;
    private final String color;
    private final String iconItem;

    JobType(String displayName, String description, String color, String iconItem) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.iconItem = iconItem;
    }

    public String getIconItem() {
        return iconItem;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public static JobType fromString(String name) {
        if (name == null || name.isEmpty())
            return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try matching display name
            for (JobType type : values()) {
                if (type.displayName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
