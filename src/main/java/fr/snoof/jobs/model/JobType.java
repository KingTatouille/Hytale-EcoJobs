package fr.snoof.jobs.model;

import fr.snoof.jobs.config.ConfigManager;

public enum JobType {
    FARMER("Fermier", "Producteur agricole", "#2ecc71"),
    HUNTER("Chasseur Cueilleur", "Chasse et collecte", "#e67e22"),
    CHAMPION("Champion Militaire", "Combat PvP/PvE", "#e74c3c"),
    MINER("Mineur Tailleur", "Extraction minière", "#95a5a6"),
    BLACKSMITH("Forgeron Ingénieur", "Craft technique", "#f39c12"),
    LUMBERJACK("Bucheron Ébéniste", "Travail du bois", "#27ae60");

    private final String displayName;
    private final String description;
    private final String color;

    JobType(String displayName, String description, String color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
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
