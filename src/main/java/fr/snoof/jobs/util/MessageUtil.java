package fr.snoof.jobs.util;

import com.hypixel.hytale.server.core.Message;

public class MessageUtil {

    public static final String PREFIX_TEXT = "[Jobs] ";
    public static final String PREFIX_COLOR = "#9b59b6"; // Purple

    public static final String ERROR_COLOR = "#e74c3c";
    public static final String SUCCESS_COLOR = "#2ecc71";
    public static final String INFO_COLOR = "#3498db";
    public static final String WARNING_COLOR = "#f39c12";

    public static Message success(String text) {
        return Message.join(
                Message.raw(PREFIX_TEXT).color(PREFIX_COLOR),
                parse(" " + text, SUCCESS_COLOR));
    }

    public static Message error(String text) {
        return Message.join(
                Message.raw(PREFIX_TEXT).color(PREFIX_COLOR),
                parse(" " + text, ERROR_COLOR));
    }

    public static Message info(String text) {
        return Message.join(
                Message.raw(PREFIX_TEXT).color(PREFIX_COLOR),
                parse(" " + text, INFO_COLOR));
    }

    public static Message warning(String text) {
        return Message.join(
                Message.raw(PREFIX_TEXT).color(PREFIX_COLOR),
                parse(" " + text, WARNING_COLOR));
    }

    public static Message raw(String text) {
        return parse(text, "#FFFFFF");
    }

    public static Message raw(String text, String color) {
        return parse(text, color);
    }

    private static Message parse(String text, String defaultColor) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }

        if (!text.contains("§")) {
            return Message.raw(text).color(defaultColor);
        }

        String[] parts = text.split("§");
        Message currentMessage = null;

        if (!text.startsWith("§")) {
            currentMessage = Message.raw(parts[0]).color(defaultColor);
        }

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;
            if (i == 0 && !text.startsWith("§"))
                continue;

            char code = part.charAt(0);
            String content = part.substring(1);
            String color = getColorFromCode(code);

            if (color == null) {
                color = defaultColor;
            }

            Message partMsg = Message.raw(content).color(color);

            if (currentMessage == null) {
                currentMessage = partMsg;
            } else {
                currentMessage = Message.join(currentMessage, partMsg);
            }
        }

        return currentMessage != null ? currentMessage : Message.raw("");
    }

    private static String getColorFromCode(char code) {
        switch (code) {
            case '0':
                return "#000000";
            case '1':
                return "#0000AA";
            case '2':
                return "#00AA00";
            case '3':
                return "#00AAAA";
            case '4':
                return "#AA0000";
            case '5':
                return "#AA00AA";
            case '6':
                return "#FFAA00";
            case '7':
                return "#AAAAAA";
            case '8':
                return "#555555";
            case '9':
                return "#5555FF";
            case 'a':
                return "#55FF55";
            case 'b':
                return "#55FFFF";
            case 'c':
                return "#FF5555";
            case 'd':
                return "#FF55FF";
            case 'e':
                return "#FFFF55";
            case 'f':
                return "#FFFFFF";
            case 'r':
                return "#FFFFFF";
            default:
                return null;
        }
    }
}
