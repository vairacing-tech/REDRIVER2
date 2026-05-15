package tech.vairacing.redriver2;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class AndroidConfig {
    static final String[] LANGUAGE_NAMES = {
        "English", "Italiano", "Deutsch", "Francais", "Espanol"
    };

    static final String[][] CONTROLLER_ACTIONS = {
        { "cross", "Cruz / aceptar" },
        { "square", "Cuadrado" },
        { "circle", "Circulo" },
        { "triangle", "Triangulo / volver" },
        { "up", "Arriba" },
        { "down", "Abajo" },
        { "left", "Izquierda" },
        { "right", "Derecha" },
        { "start", "Start" },
        { "select", "Select" },
        { "l1", "L1" },
        { "r1", "R1" },
        { "l2", "L2" },
        { "r2", "R2" },
        { "l3", "L3" },
        { "r3", "R3" },
        { "axis_left_x", "Stick izquierdo X" },
        { "axis_left_y", "Stick izquierdo Y" },
        { "axis_right_x", "Stick derecho X" },
        { "axis_right_y", "Stick derecho Y" }
    };

    static final String[] CONTROLLER_VALUES = {
        "a", "b", "x", "y",
        "back", "guide", "start",
        "leftstick", "rightstick",
        "leftshoulder", "rightshoulder",
        "dpup", "dpdown", "dpleft", "dpright",
        "lefttrigger", "righttrigger",
        "leftx", "lefty", "rightx", "righty",
        "-leftx", "-lefty", "-rightx", "-righty"
    };

    String dataFolder;
    int windowWidth = 1280;
    int windowHeight = 720;
    int screenWidth = 0;
    int screenHeight = 0;
    int fullscreen = 1;
    int vsync = 1;
    int pgxpTextureMapping = 1;
    int bilinearFiltering = 0;
    int pgxpZbuffer = 1;

    int languageId = 0;
    int drawDistance = 1800;
    int dynamicLights = 1;
    int fieldOfView = 256;
    int disableChicagoBridges = 0;
    int freeCamera = 0;
    int fastLoadingScreens = 1;
    int widescreenOverlays = 1;
    int driver1music = 0;
    int overrideContent = 0;
    int unlockAll = 1;

    final LinkedHashMap<String, String> controllerMappings = new LinkedHashMap<>();

    private AndroidConfig() {
    }

    static AndroidConfig defaults(Context context) {
        AndroidConfig config = new AndroidConfig();
        config.dataFolder = GamePaths.driver2Data(context).getAbsolutePath().replace('\\', '/') + "/";
        config.controllerMappings.put("cross", "a");
        config.controllerMappings.put("square", "x");
        config.controllerMappings.put("circle", "b");
        config.controllerMappings.put("triangle", "y");
        config.controllerMappings.put("up", "dpup");
        config.controllerMappings.put("down", "dpdown");
        config.controllerMappings.put("left", "dpleft");
        config.controllerMappings.put("right", "dpright");
        config.controllerMappings.put("start", "start");
        config.controllerMappings.put("select", "back");
        config.controllerMappings.put("l1", "leftshoulder");
        config.controllerMappings.put("r1", "rightshoulder");
        config.controllerMappings.put("l2", "lefttrigger");
        config.controllerMappings.put("r2", "righttrigger");
        config.controllerMappings.put("l3", "leftstick");
        config.controllerMappings.put("r3", "rightstick");
        config.controllerMappings.put("axis_left_x", "leftx");
        config.controllerMappings.put("axis_left_y", "lefty");
        config.controllerMappings.put("axis_right_x", "rightx");
        config.controllerMappings.put("axis_right_y", "righty");
        return config;
    }

    static AndroidConfig load(Context context) throws IOException {
        AndroidConfig config = defaults(context);
        File file = GamePaths.config(context);
        if (!file.isFile()) {
            return config;
        }

        String section = "";
        for (String rawLine : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int equals = line.indexOf('=');
            if (equals < 1) {
                continue;
            }

            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            config.apply(section, key, value);
        }
        config.dataFolder = GamePaths.driver2Data(context).getAbsolutePath().replace('\\', '/') + "/";
        return config;
    }

    void save(Context context) throws IOException {
        File root = GamePaths.root(context);
        File profile = GamePaths.profile(context);
        if (!root.mkdirs() && !root.isDirectory()) {
            throw new IOException("No se pudo crear " + root);
        }
        if (!profile.mkdirs() && !profile.isDirectory()) {
            throw new IOException("No se pudo crear " + profile);
        }

        dataFolder = GamePaths.driver2Data(context).getAbsolutePath().replace('\\', '/') + "/";
        StringBuilder text = new StringBuilder();
        text.append("[fs]\n");
        text.append("dataFolder=").append(dataFolder).append("\n\n");
        text.append("[pad]\n");
        text.append("pad1device=-1\n");
        text.append("pad2device=-1\n\n");
        text.append("[controls_game]\n");
        for (Map.Entry<String, String> entry : controllerMappings.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        text.append("\n[controls_menu]\n");
        text.append("cross=a\n");
        text.append("square=x\n");
        text.append("circle=b\n");
        text.append("triangle=y\n");
        text.append("up=dpup\n");
        text.append("down=dpdown\n");
        text.append("left=dpleft\n");
        text.append("right=dpright\n");
        text.append("start=start\n");
        text.append("select=back\n\n");
        text.append("[render]\n");
        appendInt(text, "windowWidth", windowWidth);
        appendInt(text, "windowHeight", windowHeight);
        appendInt(text, "fullscreen", fullscreen);
        appendInt(text, "screenWidth", screenWidth);
        appendInt(text, "screenHeight", screenHeight);
        appendInt(text, "vsync", vsync);
        appendInt(text, "pgxpTextureMapping", pgxpTextureMapping);
        appendInt(text, "pgxpZbuffer", pgxpZbuffer);
        appendInt(text, "bilinearFiltering", bilinearFiltering);
        text.append("\n[game]\n");
        appendInt(text, "languageId", languageId);
        appendInt(text, "drawDistance", drawDistance);
        appendInt(text, "dynamicLights", dynamicLights);
        appendInt(text, "fieldOfView", fieldOfView);
        appendInt(text, "disableChicagoBridges", disableChicagoBridges);
        appendInt(text, "freeCamera", freeCamera);
        appendInt(text, "fastLoadingScreens", fastLoadingScreens);
        appendInt(text, "widescreenOverlays", widescreenOverlays);
        appendInt(text, "driver1music", driver1music);
        appendInt(text, "overrideContent", overrideContent);
        appendInt(text, "unlockAll", unlockAll);

        Files.write(GamePaths.config(context).toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void apply(String section, String key, String value) {
        if ("controls_game".equals(section) && controllerMappings.containsKey(key)) {
            controllerMappings.put(key, value);
            return;
        }

        int number = parseInt(value, Integer.MIN_VALUE);
        if (number == Integer.MIN_VALUE) {
            return;
        }

        if ("render".equals(section)) {
            if ("windowWidth".equals(key)) {
                windowWidth = number;
            } else if ("windowHeight".equals(key)) {
                windowHeight = number;
            } else if ("screenWidth".equals(key)) {
                screenWidth = number;
            } else if ("screenHeight".equals(key)) {
                screenHeight = number;
            } else if ("fullscreen".equals(key)) {
                fullscreen = number;
            } else if ("vsync".equals(key)) {
                vsync = number;
            } else if ("pgxpTextureMapping".equals(key)) {
                pgxpTextureMapping = number;
            } else if ("pgxpZbuffer".equals(key)) {
                pgxpZbuffer = number;
            } else if ("bilinearFiltering".equals(key)) {
                bilinearFiltering = number;
            }
        } else if ("game".equals(section)) {
            if ("languageId".equals(key)) {
                languageId = number;
            } else if ("drawDistance".equals(key)) {
                drawDistance = number;
            } else if ("dynamicLights".equals(key)) {
                dynamicLights = number;
            } else if ("fieldOfView".equals(key)) {
                fieldOfView = number;
            } else if ("disableChicagoBridges".equals(key)) {
                disableChicagoBridges = number;
            } else if ("freeCamera".equals(key)) {
                freeCamera = number;
            } else if ("fastLoadingScreens".equals(key)) {
                fastLoadingScreens = number;
            } else if ("widescreenOverlays".equals(key)) {
                widescreenOverlays = number;
            } else if ("driver1music".equals(key)) {
                driver1music = number;
            } else if ("overrideContent".equals(key)) {
                overrideContent = number;
            } else if ("unlockAll".equals(key)) {
                unlockAll = number;
            }
        }
    }

    private static void appendInt(StringBuilder text, String key, int value) {
        text.append(String.format(Locale.US, "%s=%d\n", key, value));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash >= 0 ? line.substring(0, hash) : line;
    }
}
