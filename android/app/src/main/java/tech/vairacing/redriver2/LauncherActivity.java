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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherActivity extends Activity {
    private static final int REQUEST_OPEN_TREE = 1001;
    private static final int COLOR_BACKGROUND = 0xff111111;
    private static final int COLOR_TEXT = 0xffffffff;
    private static final int COLOR_MUTED = 0xffb8b8b8;
    private static final int SCREEN_MAIN = 0;
    private static final int SCREEN_OPTIONS = 1;
    private static final int SCREEN_CONTROLLER = 2;
    private static final int SCREEN_IMPORT = 3;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView importStatus;
    private ProgressBar importProgress;
    private Button importButton;
    private Spinner importStorage;
    private GamePaths.StorageLocation[] importStorageLocations;
    private int currentScreen = SCREEN_MAIN;
    private int uiLanguageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ensureConfigExists();
        showMainMenu();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == SCREEN_OPTIONS || currentScreen == SCREEN_IMPORT) {
            showMainMenu();
        } else if (currentScreen == SCREEN_CONTROLLER) {
            showOptionsUi();
        } else {
            super.onBackPressed();
        }
    }

    private void ensureConfigExists() {
        if (GamePaths.config(this).isFile()) {
            return;
        }
        try {
            AndroidConfig.defaults(this).save(this);
        } catch (IOException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showMainMenu() {
        currentScreen = SCREEN_MAIN;
        uiLanguageId = loadUiLanguageId();
        LinearLayout content = createContentLayout(false);
        addTitle(content, "REDRIVER2");
        addCaption(content, GamePaths.isInstalled(this)
            ? tr("Game data installed. Ready to start.",
                "Dati di gioco installati. Pronto per l'avvio.",
                "Spieldaten installiert. Bereit zum Start.",
                "Données du jeu installées. Prêt à démarrer.",
                "Datos instalados. Preparado para arrancar.")
            : tr("Import the DRIVER2 folder before starting the game.",
                "Importa la cartella DRIVER2 prima di avviare il gioco.",
                "Importiere den Ordner DRIVER2, bevor du das Spiel startest.",
                "Importez le dossier DRIVER2 avant de lancer le jeu.",
                "Importa la carpeta DRIVER2 antes de iniciar el juego."));

        Button startButton = addButton(content, tr("Start game", "Avvia gioco", "Spiel starten", "Démarrer le jeu", "Iniciar juego"));
        startButton.setEnabled(GamePaths.isInstalled(this));
        startButton.setOnClickListener(v -> startGame());
        startButton.requestFocus();

        Button optionsButton = addButton(content, tr("Options", "Opzioni", "Optionen", "Options", "Opciones"));
        optionsButton.setOnClickListener(v -> showOptionsUi());

        Button importDataButton = addButton(content,
            GamePaths.isInstalled(this)
                ? tr("Game data", "Dati di gioco", "Spieldaten", "Données du jeu", "Datos del juego")
                : tr("Import DRIVER2", "Importa DRIVER2", "DRIVER2 importieren", "Importer DRIVER2", "Importar DRIVER2"));
        importDataButton.setOnClickListener(v -> showImportUi());

        Button exitButton = addButton(content, tr("Exit", "Esci", "Beenden", "Quitter", "Salir"));
        exitButton.setOnClickListener(v -> finishAffinity());

        setContentView(wrapInScrollView(content));
    }

    private void showOptionsUi() {
        currentScreen = SCREEN_OPTIONS;
        final AndroidConfig config;
        try {
            config = AndroidConfig.load(this);
        } catch (IOException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            showMainMenu();
            return;
        }
        uiLanguageId = clamp(config.languageId, 0, 4);

        LinearLayout content = createContentLayout(true);
        addTitle(content, tr("Options", "Opzioni", "Optionen", "Options", "Opciones"));

        addSectionTitle(content, tr("Graphics", "Grafica", "Grafik", "Graphismes", "Gráficos"));
        int[][] resolutionChoices = buildResolutionChoices(config.windowWidth, config.windowHeight);
        Spinner resolution = addSpinner(content, tr("Resolution", "Risoluzione", "Auflösung", "Résolution", "Resolución"), resolutionLabels(resolutionChoices),
            resolutionIndexOf(resolutionChoices, config.windowWidth, config.windowHeight));
        CheckBox nativeResolution = addCheckBox(content, tr("Use native fullscreen resolution",
                "Usa la risoluzione nativa a schermo intero",
                "Native Vollbildauflösung verwenden",
                "Utiliser la résolution native en plein écran",
                "Usar resolución nativa en pantalla completa"),
            config.screenWidth == 0 && config.screenHeight == 0);
        CheckBox fullscreen = addCheckBox(content, tr("Run in fullscreen", "Esegui a schermo intero", "Im Vollbild ausführen", "Exécuter en plein écran", "Ejecutar en pantalla completa"), config.fullscreen != 0);
        CheckBox textureFiltering = addCheckBox(content, tr("Texture filtering", "Filtraggio texture", "Texturfilterung", "Filtrage des textures", "Filtrado de texturas"), config.bilinearFiltering != 0);
        CheckBox perspectiveTexturing = addCheckBox(content, tr("Perspective-correct textures", "Texture con prospettiva", "Perspektivisch korrekte Texturen", "Textures corrigées en perspective", "Texturas con perspectiva"), config.pgxpTextureMapping != 0);
        CheckBox zBuffer = addCheckBox(content, "Z-buffer", config.pgxpZbuffer != 0);
        CheckBox widescreenOverlays = addCheckBox(content, tr("Widescreen overlays", "Overlay panoramici", "Breitbild-Overlays", "Overlays panoramiques", "Overlays panorámicos"), config.widescreenOverlays != 0);
        CheckBox vsync = addCheckBox(content, "VSync", config.vsync != 0);

        addSectionTitle(content, tr("Gameplay", "Gameplay", "Gameplay", "Gameplay", "Gameplay"));
        CheckBox fastLoading = addCheckBox(content, tr("Fast loading screens", "Schermate di caricamento rapide", "Schnelle Ladebildschirme", "Écrans de chargement rapides", "Pantallas de carga rápidas"), config.fastLoadingScreens != 0);
        CheckBox disableBridges = addCheckBox(content, tr("Disable Chicago bridges", "Disattiva i ponti di Chicago", "Chicago-Brücken deaktivieren", "Désactiver les ponts de Chicago", "Desactivar puentes de Chicago"), config.disableChicagoBridges != 0);
        CheckBox dynamicLights = addCheckBox(content, tr("Dynamic lights", "Luci dinamiche", "Dynamische Lichter", "Lumières dynamiques", "Luces dinámicas"), config.dynamicLights != 0);
        CheckBox unlockAll = addCheckBox(content, tr("Unlock all playable content", "Sblocca tutti i contenuti giocabili", "Alle spielbaren Inhalte freischalten", "Débloquer tout le contenu jouable", "Desbloquear todo el contenido jugable"), config.unlockAll != 0);
        CheckBox freeCamera = addCheckBox(content, tr("Free camera", "Telecamera libera", "Freie Kamera", "Caméra libre", "Cámara libre"), config.freeCamera != 0);
        CheckBox driver1Music = addCheckBox(content, tr("Driver 1 music", "Musica di Driver 1", "Driver-1-Musik", "Musique de Driver 1", "Música de Driver 1"), config.driver1music != 0);
        CheckBox overrideContent = addCheckBox(content, tr("Enable content overrides", "Abilita contenuti sostitutivi", "Inhalts-Overrides aktivieren", "Activer les remplacements de contenu", "Activar reemplazos de contenido"), config.overrideContent != 0);
        int fieldOfViewValue = clamp(config.fieldOfView,
            AndroidConfig.MIN_FIELD_OF_VIEW, AndroidConfig.MAX_FIELD_OF_VIEW);
        int[] fieldOfViewChoices = buildNumberChoices(fieldOfViewValue,
            128, 160, 192, 224, 256, 288, 320, 352, 384);
        Spinner fieldOfView = addSpinner(content, tr("Field of view", "Campo visivo", "Sichtfeld", "Champ de vision", "Campo de visión"), numberLabels(fieldOfViewChoices),
            numberIndexOf(fieldOfViewChoices, fieldOfViewValue));
        int drawDistanceValue = clamp(config.drawDistance,
            AndroidConfig.MIN_DRAW_DISTANCE, AndroidConfig.MAX_DRAW_DISTANCE);
        int[] drawDistanceChoices = buildNumberChoices(drawDistanceValue,
            441, 600, 900, 1200, 1500, 1800);
        Spinner drawDistance = addSpinner(content, tr("Draw distance", "Distanza visiva", "Sichtweite", "Distance d'affichage", "Distancia de dibujado"), numberLabels(drawDistanceChoices),
            numberIndexOf(drawDistanceChoices, drawDistanceValue));
        Spinner language = addSpinner(content, tr("Game language", "Lingua del gioco", "Spielsprache", "Langue du jeu", "Idioma del juego"), AndroidConfig.LANGUAGE_NAMES, clamp(config.languageId, 0, 4));

        addSectionTitle(content, tr("Controls", "Controlli", "Steuerung", "Commandes", "Controles"));
        Button controllerButton = addButton(content, tr("Controller mapping", "Mappatura controller", "Controller-Belegung", "Configuration de la manette", "Mapeo de mando"));
        controllerButton.setOnClickListener(v -> showControllerMappingUi(config));

        Button saveButton = addButton(content, tr("Save and return", "Salva e torna indietro", "Speichern und zurück", "Enregistrer et revenir", "Guardar y volver"));
        saveButton.setOnClickListener(v -> {
            try {
                int[] selectedResolution = resolutionChoices[resolution.getSelectedItemPosition()];
                config.windowWidth = selectedResolution[0];
                config.windowHeight = selectedResolution[1];
                config.screenWidth = nativeResolution.isChecked() ? 0 : config.windowWidth;
                config.screenHeight = nativeResolution.isChecked() ? 0 : config.windowHeight;
                config.fullscreen = bool(fullscreen);
                config.bilinearFiltering = bool(textureFiltering);
                config.pgxpTextureMapping = bool(perspectiveTexturing);
                config.pgxpZbuffer = bool(zBuffer);
                config.widescreenOverlays = bool(widescreenOverlays);
                config.vsync = bool(vsync);
                config.fastLoadingScreens = bool(fastLoading);
                config.disableChicagoBridges = bool(disableBridges);
                config.dynamicLights = bool(dynamicLights);
                config.unlockAll = bool(unlockAll);
                config.freeCamera = bool(freeCamera);
                config.driver1music = bool(driver1Music);
                config.overrideContent = bool(overrideContent);
                config.fieldOfView = fieldOfViewChoices[fieldOfView.getSelectedItemPosition()];
                config.drawDistance = drawDistanceChoices[drawDistance.getSelectedItemPosition()];
                config.languageId = language.getSelectedItemPosition();
                config.save(this);
                recreate();
            } catch (IOException | IllegalArgumentException ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        Button backButton = addButton(content, tr("Return without saving", "Torna senza salvare", "Ohne Speichern zurück", "Revenir sans enregistrer", "Volver sin guardar"));
        backButton.setOnClickListener(v -> showMainMenu());

        CompoundButton.OnCheckedChangeListener resolutionListener = (buttonView, isChecked) -> {
            resolution.setEnabled(!isChecked);
        };
        nativeResolution.setOnCheckedChangeListener(resolutionListener);
        resolutionListener.onCheckedChanged(nativeResolution, nativeResolution.isChecked());

        setContentView(wrapInScrollView(content));
    }

    private void showControllerMappingUi(AndroidConfig config) {
        currentScreen = SCREEN_CONTROLLER;
        LinearLayout content = createContentLayout(true);
        addTitle(content, tr("Controller mapping", "Mappatura controller", "Controller-Belegung", "Configuration de la manette", "Mapeo de mando"));
        addCaption(content, tr("SDL GameController mapping used by REDRIVER2.",
            "Mappatura SDL GameController usata da REDRIVER2.",
            "Von REDRIVER2 verwendete SDL-GameController-Belegung.",
            "Configuration SDL GameController utilisée par REDRIVER2.",
            "Mapeo SDL GameController usado por REDRIVER2."));

        LinkedHashMap<String, Spinner> spinners = new LinkedHashMap<>();
        for (String[] action : AndroidConfig.CONTROLLER_ACTIONS) {
            String key = action[0];
            String label = action[1];
            String value = config.controllerMappings.get(key);
            int selected = indexOf(AndroidConfig.CONTROLLER_VALUES, value);
            Spinner spinner = addSpinner(content, label, AndroidConfig.CONTROLLER_VALUES, selected < 0 ? 0 : selected);
            spinners.put(key, spinner);
        }

        Button saveButton = addButton(content, tr("Save and return", "Salva e torna indietro", "Speichern und zurück", "Enregistrer et revenir", "Guardar y volver"));
        saveButton.setOnClickListener(v -> {
            for (Map.Entry<String, Spinner> entry : spinners.entrySet()) {
                config.controllerMappings.put(entry.getKey(), (String) entry.getValue().getSelectedItem());
            }
            try {
                config.save(this);
                showOptionsUi();
            } catch (IOException ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        Button backButton = addButton(content, tr("Return without saving", "Torna senza salvare", "Ohne Speichern zurück", "Revenir sans enregistrer", "Volver sin guardar"));
        backButton.setOnClickListener(v -> showOptionsUi());

        setContentView(wrapInScrollView(content));
    }

    private void showImportUi() {
        currentScreen = SCREEN_IMPORT;
        LinearLayout content = createContentLayout(false);
        addTitle(content, tr("Import data", "Importa dati", "Daten importieren", "Importer les données", "Importar datos"));

        importStatus = addCaption(content, GamePaths.isInstalled(this)
            ? tr("Data installed in ", "Dati installati in ", "Daten installiert in ", "Données installées dans ", "Datos instalados en ")
                + GamePaths.currentStorageLabel(this) + "."
            : tr("Select the DRIVER2 folder or a folder that contains it.",
                "Seleziona la cartella DRIVER2 o una cartella che la contiene.",
                "Wähle den Ordner DRIVER2 oder einen Ordner, der ihn enthält.",
                "Sélectionnez le dossier DRIVER2 ou un dossier qui le contient.",
                "Selecciona la carpeta DRIVER2 o una carpeta que la contenga."));
        importStorageLocations = GamePaths.availableStorageLocations(this);
        importStorage = addSpinner(content, tr("Destination", "Destinazione", "Ziel", "Destination", "Destino"), storageLabels(importStorageLocations),
            storageIndexOf(importStorageLocations, GamePaths.getDataStorageId(this)));
        if (!GamePaths.hasRemovableStorage(this)) {
            addCaption(content, tr("No available SD card was detected.",
                "Nessuna scheda SD disponibile rilevata.",
                "Keine verfügbare SD-Karte erkannt.",
                "Aucune carte SD disponible détectée.",
                "No se ha detectado una tarjeta SD disponible."));
        }
        importProgress = new ProgressBar(this);
        importProgress.setIndeterminate(true);
        importProgress.setVisibility(ProgressBar.GONE);

        importButton = addButton(content, tr("Import DRIVER2", "Importa DRIVER2", "DRIVER2 importieren", "Importer DRIVER2", "Importar DRIVER2"));
        importButton.setOnClickListener(v -> openTreePicker());
        content.addView(importProgress);

        Button backButton = addButton(content, tr("Return", "Indietro", "Zurück", "Retour", "Volver"));
        backButton.setOnClickListener(v -> showMainMenu());

        setContentView(wrapInScrollView(content));
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
        String previousStorageId = GamePaths.getDataStorageId(this);
        GamePaths.StorageLocation selectedStorage =
            importStorageLocations[importStorage.getSelectedItemPosition()];

        setBusy(true, "Importando datos...");
        executor.execute(() -> {
            try {
                String driver2DocumentId = resolveDriver2DocumentId(treeUri);
                if (driver2DocumentId == null) {
                    throw new IOException("Selecciona la carpeta DRIVER2 o una carpeta que la contenga.");
                }

                File target = GamePaths.driver2Data(this, selectedStorage.id);
                deleteRecursively(target);
                if (!target.mkdirs() && !target.isDirectory()) {
                    throw new IOException("No se pudo crear " + target);
                }

                copyDocumentTree(treeUri, driver2DocumentId, target);
                GamePaths.setDataStorageId(this, selectedStorage.id);
                AndroidConfig.load(this).save(this);
                if (!previousStorageId.equals(selectedStorage.id)) {
                    deleteRecursively(GamePaths.driver2Data(this, previousStorageId));
                }
                mainHandler.post(this::showMainMenu);
            } catch (Exception ex) {
                mainHandler.post(() -> {
                    setBusy(false, ex.getMessage());
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setBusy(boolean busy, String message) {
        if (importStatus != null) {
            importStatus.setText(message);
        }
        if (importProgress != null) {
            importProgress.setVisibility(busy ? ProgressBar.VISIBLE : ProgressBar.GONE);
        }
        if (importButton != null) {
            importButton.setEnabled(!busy);
        }
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
        startActivity(new Intent(this, GameActivity.class));
    }

    private int loadUiLanguageId() {
        try {
            return clamp(AndroidConfig.load(this).languageId, 0, 4);
        } catch (IOException ignored) {
            return 0;
        }
    }

    private String tr(String english, String italian, String german, String french, String spanish) {
        switch (uiLanguageId) {
            case 1:
                return italian;
            case 2:
                return german;
            case 3:
                return french;
            case 4:
                return spanish;
            default:
                return english;
        }
    }

    private LinearLayout createContentLayout(boolean alignTop) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(alignTop ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(COLOR_BACKGROUND);
        return layout;
    }

    private ScrollView wrapInScrollView(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void addTitle(LinearLayout layout, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, 24);
        layout.addView(title, params);
    }

    private TextView addCaption(LinearLayout layout, String text) {
        TextView caption = new TextView(this);
        caption.setText(text);
        caption.setTextColor(COLOR_MUTED);
        caption.setTextSize(16);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, 24);
        layout.addView(caption, params);
        return caption;
    }

    private void addSectionTitle(LinearLayout layout, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(20);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 24, 0, 12);
        layout.addView(title, params);
    }

    private Button addButton(LinearLayout layout, String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 8, 0, 8);
        layout.addView(button, params);
        return button;
    }

    private CheckBox addCheckBox(LinearLayout layout, String text, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextColor(COLOR_TEXT);
        checkBox.setChecked(checked);
        layout.addView(checkBox, matchWrap());
        return checkBox;
    }

    private Spinner addSpinner(LinearLayout layout, String label, String[] values, int selected) {
        addFieldLabel(layout, label);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(clamp(selected, 0, values.length - 1));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, 8);
        layout.addView(spinner, params);
        return spinner;
    }

    private void addFieldLabel(LinearLayout layout, String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(COLOR_MUTED);
        view.setTextSize(14);
        layout.addView(view, matchWrap());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int bool(CheckBox checkBox) {
        return checkBox.isChecked() ? 1 : 0;
    }

    private int indexOf(String[] values, String needle) {
        if (needle == null) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (needle.equals(values[i])) {
                return i;
            }
        }
        return -1;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int[][] buildResolutionChoices(int currentWidth, int currentHeight) {
        int[][] presets = {
            { 640, 480 },
            { 800, 600 },
            { 1024, 768 },
            { 1280, 720 },
            { 1600, 900 },
            { 1920, 1080 },
            { 2560, 1440 }
        };
        if (resolutionIndexOf(presets, currentWidth, currentHeight) >= 0) {
            return presets;
        }

        int[][] choices = new int[presets.length + 1][2];
        for (int i = 0; i < presets.length; i++) {
            choices[i] = presets[i];
        }
        choices[presets.length] = new int[] { currentWidth, currentHeight };
        return choices;
    }

    private String[] resolutionLabels(int[][] choices) {
        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i][0] + " x " + choices[i][1];
        }
        return labels;
    }

    private int resolutionIndexOf(int[][] choices, int width, int height) {
        for (int i = 0; i < choices.length; i++) {
            if (choices[i][0] == width && choices[i][1] == height) {
                return i;
            }
        }
        return 0;
    }

    private int[] buildNumberChoices(int current, int... presets) {
        if (numberIndexOf(presets, current) >= 0) {
            return presets;
        }
        int[] choices = new int[presets.length + 1];
        System.arraycopy(presets, 0, choices, 0, presets.length);
        choices[presets.length] = current;
        return choices;
    }

    private String[] numberLabels(int[] values) {
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = String.valueOf(values[i]);
        }
        return labels;
    }

    private String[] storageLabels(GamePaths.StorageLocation[] locations) {
        String[] labels = new String[locations.length];
        for (int i = 0; i < locations.length; i++) {
            labels[i] = locations[i].label;
        }
        return labels;
    }

    private int storageIndexOf(GamePaths.StorageLocation[] locations, String storageId) {
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].id.equals(storageId)) {
                return i;
            }
        }
        return 0;
    }

    private int numberIndexOf(int[] values, int needle) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == needle) {
                return i;
            }
        }
        return 0;
    }
}
