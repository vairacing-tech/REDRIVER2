package tech.vairacing.redriver2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class GamePaths {
    static final String STORAGE_INTERNAL = "internal";

    private static final String PREFS_NAME = "redriver2";
    private static final String PREF_DATA_STORAGE = "data_storage";

    static final class StorageLocation {
        final String id;
        final String label;
        final File baseDir;

        StorageLocation(String id, String label, File baseDir) {
            this.id = id;
            this.label = label;
            this.baseDir = baseDir;
        }
    }

    private GamePaths() {
    }

    static File root(Context context) {
        return new File(context.getFilesDir(), "redriver2");
    }

    static File dataRoot(Context context) {
        return dataRoot(context, getDataStorageId(context));
    }

    static File dataRoot(Context context, String storageId) {
        return new File(storageBase(context, storageId), "redriver2/data");
    }

    static File driver2Data(Context context) {
        return driver2Data(context, getDataStorageId(context));
    }

    static File driver2Data(Context context, String storageId) {
        return new File(dataRoot(context, storageId), "DRIVER2");
    }

    static File profile(Context context) {
        return new File(root(context), "profile");
    }

    static File config(Context context) {
        return new File(root(context), "config.ini");
    }

    static boolean isInstalled(Context context) {
        return isValidDriver2Data(driver2Data(context));
    }

    static boolean isValidDriver2Data(File driver2) {
        return new File(driver2, "GFX").isDirectory()
            && new File(driver2, "DATA").isDirectory()
            && new File(driver2, "LANG").isDirectory();
    }

    static String getDataStorageId(Context context) {
        return preferences(context).getString(PREF_DATA_STORAGE, STORAGE_INTERNAL);
    }

    static void setDataStorageId(Context context, String storageId) {
        preferences(context).edit().putString(PREF_DATA_STORAGE, storageId).apply();
    }

    static StorageLocation[] availableStorageLocations(Context context) {
        List<StorageLocation> locations = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        locations.add(new StorageLocation(STORAGE_INTERNAL, "Memoria interna", context.getFilesDir()));

        int removableIndex = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager != null) {
                for (StorageVolume volume : storageManager.getStorageVolumes()) {
                    if (!volume.isRemovable()
                        || !Environment.MEDIA_MOUNTED.equals(volume.getState())
                        || volume.getDirectory() == null) {
                        continue;
                    }

                    File dir = new File(volume.getDirectory(),
                        "Android/data/" + context.getPackageName() + "/files");
                    String path = dir.getAbsolutePath();
                    if (!seenPaths.add(path)) {
                        continue;
                    }

                    locations.add(new StorageLocation(path,
                        storageLabel(removableIndex++, volumeName(dir)), dir));
                }
            }
        }

        File[] dirs = context.getExternalFilesDirs(null);
        if (dirs != null) {
            for (int i = 1; i < dirs.length; i++) {
                File dir = dirs[i];
                if (dir == null
                    || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(dir))) {
                    continue;
                }

                String path = dir.getAbsolutePath();
                if (!seenPaths.add(path)) {
                    continue;
                }
                locations.add(new StorageLocation(path,
                    storageLabel(removableIndex++, volumeName(dir)), dir));
            }
        }

        return locations.toArray(new StorageLocation[0]);
    }

    static String currentStorageLabel(Context context) {
        String storageId = getDataStorageId(context);
        for (StorageLocation location : availableStorageLocations(context)) {
            if (storageId.equals(location.id)) {
                return location.label;
            }
        }
        return STORAGE_INTERNAL.equals(storageId) ? "Memoria interna" : "Tarjeta SD no disponible";
    }

    static boolean hasRemovableStorage(Context context) {
        return availableStorageLocations(context).length > 1;
    }

    private static File storageBase(Context context, String storageId) {
        if (STORAGE_INTERNAL.equals(storageId)) {
            return context.getFilesDir();
        }
        return new File(storageId);
    }

    private static String volumeName(File dir) {
        String[] parts = dir.getAbsolutePath().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("storage".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return "";
    }

    private static String storageLabel(int index, String volumeName) {
        String label = "Tarjeta SD " + index;
        if (!volumeName.isEmpty()) {
            label += " (" + volumeName + ")";
        }
        return label;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
