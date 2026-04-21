package com.example.knittdaserver.common.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class DebugFileLogger {

    private static final Path LOG_PATH = Path.of("/Users/jisoolee/Documents/knittda-backend/.cursor/debug.log");

    private DebugFileLogger() {
    }

    public static void log(String runId, String hypothesisId, String location, String message, String dataJson) {
        String payload = "{\"id\":\"" + System.nanoTime() + "\","
                + "\"timestamp\":" + System.currentTimeMillis() + ","
                + "\"runId\":\"" + escape(runId) + "\","
                + "\"hypothesisId\":\"" + escape(hypothesisId) + "\","
                + "\"location\":\"" + escape(location) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"data\":" + (dataJson == null ? "{}" : dataJson)
                + "}\n";
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, payload, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
