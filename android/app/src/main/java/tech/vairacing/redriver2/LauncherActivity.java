package tech.vairacing.redriver2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherActivity extends Activity {
    private static final int REQUEST_OPEN_TREE = 1001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout layout;
    private TextView status;
    private ProgressBar progress;
    private Button importButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (GamePaths.isInstalled(this)) {
            startGame();
            return;
        }
        showImportUi();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showImportUi() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xff111111);

        status = new TextView(this);
        status.setTextColor(0xffffffff);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setText("Importa la carpeta DRIVER2 para jugar con mando.");

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(ProgressBar.GONE);

        importButton = new Button(this);
        importButton.setText("Importar DRIVER2");
        importButton.setOnClickListener(v -> openTreePicker());

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, 0, 0, 32);
        layout.addView(status, textParams);
        layout.addView(importButton);
        layout.addView(progress);

        setContentView(layout);
    }

    private void openTreePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_TREE || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri treeUri = data.getData();
        if (treeUri == null) {
            return;
        }

        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(treeUri, flags);
        importTree(treeUri);
    }

    private void importTree(Uri treeUri) {
        setBusy(true, "Importando datos...");
        executor.execute(() -> {
            try {
                String driver2DocumentId = resolveDriver2DocumentId(treeUri);
                if (driver2DocumentId == null) {
                    throw new IOException("Selecciona la carpeta DRIVER2 o una carpeta que la contenga.");
                }

                File target = GamePaths.driver2Data(this);
                deleteRecursively(target);
                if (!target.mkdirs() && !target.isDirectory()) {
                    throw new IOException("No se pudo crear " + target);
                }

                copyDocumentTree(treeUri, driver2DocumentId, target);
                writeConfig();

                mainHandler.post(this::startGame);
            } catch (Exception ex) {
                mainHandler.post(() -> {
                    setBusy(false, ex.getMessage());
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setBusy(boolean busy, String message) {
        status.setText(message);
        progress.setVisibility(busy ? ProgressBar.VISIBLE : ProgressBar.GONE);
        importButton.setEnabled(!busy);
    }

    private String resolveDriver2DocumentId(Uri treeUri) {
        String rootName = getDisplayName(treeUri);
        String rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        if ("DRIVER2".equalsIgnoreCase(rootName)) {
            return rootDocumentId;
        }
        return findChildDocumentId(treeUri, rootDocumentId, "DRIVER2");
    }

    private String findChildDocumentId(Uri treeUri, String parentDocumentId, String childName) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        try (Cursor cursor = getContentResolver().query(childrenUri,
            new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            }, null, null, null)) {
            if (cursor == null) {
                return null;
            }
            while (cursor.moveToNext()) {
                String documentId = cursor.getString(0);
                String displayName = cursor.getString(1);
                String mimeType = cursor.getString(2);
                if (childName.equalsIgnoreCase(displayName)
                    && DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    return documentId;
                }
            }
        }
        return null;
    }

    private String getDisplayName(Uri treeUri) {
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        try (Cursor cursor = getContentResolver().query(documentUri,
            new String[] { DocumentsContract.Document.COLUMN_DISPLAY_NAME }, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return "";
    }

    private void copyDocumentTree(Uri treeUri, String documentId, File targetDir) throws IOException {
        ContentResolver resolver = getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        try (Cursor cursor = resolver.query(childrenUri,
            new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            }, null, null, null)) {
            if (cursor == null) {
                throw new IOException("No se pudo leer la carpeta seleccionada.");
            }

            while (cursor.moveToNext()) {
                String childDocumentId = cursor.getString(0);
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocumentId);
                File out = new File(targetDir, name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    if (!out.mkdirs() && !out.isDirectory()) {
                        throw new IOException("No se pudo crear " + out);
                    }
                    copyDocumentTree(treeUri, childDocumentId, out);
                } else {
                    copyDocumentFile(childUri, out);
                }
            }
        }
    }

    private void copyDocumentFile(Uri uri, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("No se pudo crear " + parent);
        }
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) {
                throw new IOException("No se pudo abrir " + uri);
            }
            byte[] buffer = new byte[1024 * 128];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void writeConfig() throws IOException {
        File root = GamePaths.root(this);
        File profile = GamePaths.profile(this);
        if (!root.mkdirs() && !root.isDirectory()) {
            throw new IOException("No se pudo crear " + root);
        }
        if (!profile.mkdirs() && !profile.isDirectory()) {
            throw new IOException("No se pudo crear " + profile);
        }

        String dataFolder = GamePaths.driver2Data(this).getAbsolutePath().replace('\\', '/') + "/";
        String config = String.format(Locale.US,
            "[fs]\n"
                + "dataFolder=%s\n\n"
                + "[pad]\n"
                + "pad1device=-1\n"
                + "pad2device=-1\n\n"
                + "[render]\n"
                + "windowWidth=1280\n"
                + "windowHeight=720\n"
                + "fullscreen=1\n"
                + "vsync=1\n"
                + "pgxpTextureMapping=0\n"
                + "pgxpZbuffer=0\n"
                + "bilinearFiltering=0\n\n"
                + "[game]\n"
                + "drawDistance=600\n"
                + "fastLoadingScreens=1\n"
                + "languageId=0\n"
                + "unlockAll=1\n",
            dataFolder);
        java.nio.file.Files.write(GamePaths.config(this).toPath(), config.getBytes(StandardCharsets.UTF_8));
    }

    private void deleteRecursively(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("No se pudo borrar " + file);
        }
    }

    private void startGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish();
    }
}
