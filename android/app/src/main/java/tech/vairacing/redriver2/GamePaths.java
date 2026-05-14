package tech.vairacing.redriver2;

import android.content.Context;

import java.io.File;

final class GamePaths {
    private GamePaths() {
    }

    static File root(Context context) {
        return new File(context.getFilesDir(), "redriver2");
    }

    static File dataRoot(Context context) {
        return new File(root(context), "data");
    }

    static File driver2Data(Context context) {
        return new File(dataRoot(context), "DRIVER2");
    }

    static File profile(Context context) {
        return new File(root(context), "profile");
    }

    static File config(Context context) {
        return new File(root(context), "config.ini");
    }

    static boolean isInstalled(Context context) {
        File driver2 = driver2Data(context);
        return new File(driver2, "GFX").isDirectory()
            && new File(driver2, "DATA").isDirectory()
            && new File(driver2, "LANG").isDirectory();
    }
}
