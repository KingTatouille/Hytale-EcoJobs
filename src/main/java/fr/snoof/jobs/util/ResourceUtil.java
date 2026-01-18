package fr.snoof.jobs.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static String readResource(String path) {
        try (var inputStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
