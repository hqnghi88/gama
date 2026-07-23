package gama.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.text.Font;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.TextFieldTreeCell;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import gama.api.GAMA;
import gama.api.additions.GamaBundleLoader;
import gama.api.additions.registries.AgentConstructorsRegistry;
import gama.api.compilation.GamlCompilationError;
import gama.api.kernel.species.IExperimentSpecies;
import gama.api.kernel.species.IModelSpecies;
import gama.api.types.matrix.GamaMatrixFactory;
import gama.api.types.geometry.GamaShapeFactory;
import gama.api.types.topology.GamaTopologyFactory;
import gama.api.types.graph.GamaPathFactory;
import gama.api.types.graph.GamaGraphFactory;
import gama.api.types.message.GamaMessageFactory;
import gama.core.agent.GamlAgent;
import gama.core.agent.MinimalAgent;
import gama.core.geometry.InternalGamaShapeFactory;
import gama.core.standalone.StubGui;
import gama.core.standalone.StubWorkspaceManager;
import gama.core.topology.InternalTopologyFactory;
import gama.core.util.graph.InternalGamaGraphFactory;
import gama.core.util.json.Json;
import gama.core.util.matrix.InternalGamaMatrixFactory;
import gama.core.util.messaging.GamaMessage;
import gama.core.util.path.InternalGamaPathFactory;
import gama.gaml.operators.Dates;
import gaml.compiler.GamlStandaloneSetup;
import gaml.compiler.validation.GamlModelBuilder;

import com.google.inject.Injector;

public class GamaUIApp extends Application {

    private TextArea codeEditor;
    private Label statusLabel;
    private ComboBox<String> experimentCombo;
    private Button validateBtn;
    private Button compileBtn;
    private Button runBtn;
    private Button stopBtn;
    private TextArea consoleOutput;
    private File currentFile;
    private IModelSpecies currentModel;
    private Injector injector;
    private TreeView<String> fileTree;

    private static File getLibraryRoot() {
        // gama.library is a sibling of gama.ui in the repo
        File uiDir = new File(System.getProperty("user.dir"));
        File repoRoot = uiDir.getParentFile();
        if (repoRoot == null) repoRoot = uiDir;
        File lib = new File(repoRoot, "gama.library/models");
        return lib.exists() ? lib : repoRoot;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GAMA Model Editor & Simulator");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));

        // Toolbar
        root.setTop(createToolBar());

        // Left: file tree
        root.setLeft(createFileTree());

        // Center: editor + console
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);

        codeEditor = new TextArea();
        codeEditor.setFont(Font.font("Monospaced", 13));
        codeEditor.setWrapText(true);
        splitPane.getItems().add(new ScrollPane(codeEditor));

        consoleOutput = new TextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(Font.font("Monospaced", 12));
        splitPane.getItems().add(consoleOutput);
        splitPane.setDividerPositions(0.7);

        root.setCenter(splitPane);

        // Status bar
        BorderPane statusBar = new BorderPane();
        statusLabel = new Label("Ready");
        experimentCombo = new ComboBox<>();
        experimentCombo.setOnAction(e -> {});
        experimentCombo.setPrefWidth(200);
        statusBar.setLeft(statusLabel);
        statusBar.setRight(experimentCombo);
        BorderPane.setMargin(statusBar, new Insets(5));
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.show();

        appendConsole("Initializing GAMA platform...\n");
        new Thread(this::initializePlatform).start();
    }

    private ToolBar createToolBar() {
        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> openFile());
        validateBtn = new Button("Validate");
        validateBtn.setOnAction(e -> validateModel());
        validateBtn.setDisable(true);
        compileBtn = new Button("Compile");
        compileBtn.setOnAction(e -> compileModel());
        compileBtn.setDisable(true);
        runBtn = new Button("Run");
        runBtn.setOnAction(e -> runSimulation());
        runBtn.setDisable(true);
        stopBtn = new Button("Stop");
        stopBtn.setOnAction(e -> appendConsole("Stop not yet implemented.\n"));
        stopBtn.setDisable(true);
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> { consoleOutput.clear(); statusLabel.setText("Ready"); });
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshFileTree());

        return new ToolBar(openBtn, new Separator(), validateBtn, compileBtn,
                new Separator(), runBtn, stopBtn, new Separator(), clearBtn, refreshBtn);
    }

    private BorderPane createFileTree() {
        BorderPane treePane = new BorderPane();
        treePane.setPrefWidth(280);

        Label treeHeader = new Label("  Models");
        treeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 5 0;");
        treePane.setTop(treeHeader);

        TreeItem<String> root = buildTreeItem(getLibraryRoot());
        root.setExpanded(true);

        fileTree = new TreeView<>(root);
        fileTree.setShowRoot(false);
        fileTree.setCellFactory(tv -> new TextFieldTreeCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(getTreeItem() != null && getTreeItem().isLeaf()
                            ? new Label("\uD83D\uDCC4 ")  // document icon
                            : new Label("\uD83D\uDCC1 ")); // folder icon
                }
            }
        });

        fileTree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<String> selected = fileTree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.isLeaf()) {
                    File f = getFileForItem(selected);
                    if (f != null && f.getName().endsWith(".gaml")) {
                        loadFile(f);
                    }
                }
            }
        });

        treePane.setCenter(new ScrollPane(fileTree));
        return treePane;
    }

    private TreeItem<String> buildTreeItem(File dir) {
        TreeItem<String> item = new TreeItem<>(dir.getName());
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                List<File> sorted = new ArrayList<>();
                for (File c : children) sorted.add(c);
                sorted.sort((a, b) -> {
                    if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : sorted) {
                    if (child.isDirectory() || child.getName().endsWith(".gaml")) {
                        item.getChildren().add(buildTreeItem(child));
                    }
                }
            }
            item.setExpanded(dir.getName().equals("models") || dir.getParentFile() != null
                    && dir.getParentFile().getName().equals("models"));
        }
        return item;
    }

    private File getFileForItem(TreeItem<String> item) {
        List<String> path = new ArrayList<>();
        TreeItem<String> current = item;
        while (current != null) {
            path.add(0, current.getValue());
            current = current.getParent();
        }
        File f = getLibraryRoot();
        for (int i = 1; i < path.size(); i++) {
            f = new File(f, path.get(i));
        }
        return f;
    }

    private void refreshFileTree() {
        TreeItem<String> newRoot = buildTreeItem(getLibraryRoot());
        newRoot.setExpanded(true);
        fileTree.setRoot(newRoot);
        appendConsole("File tree refreshed.\n");
    }

    private void initializePlatform() {
        try {
            GamaMatrixFactory.setBuilder(new InternalGamaMatrixFactory());
            GamaShapeFactory.setBuilder(new InternalGamaShapeFactory());
            GamaTopologyFactory.setBuilder(new InternalTopologyFactory());
            GamaPathFactory.setBuilder(new InternalGamaPathFactory());
            GamaGraphFactory.setBuilder(new InternalGamaGraphFactory());
            GamaMessageFactory.setBuilder(new GamaMessage.Factory());

            Dates.initialize();
            GAMA.setJsonEncoder(Json.getNew());
            GAMA.setWorkspaceManager(new StubWorkspaceManager());

            gama.api.ui.IGui stubGui = StubGui.create();
            GAMA.setHeadlessGui(stubGui);
            GAMA.setRegularGui(stubGui);

            AgentConstructorsRegistry.register(GamlAgent.class, false);
            AgentConstructorsRegistry.register(MinimalAgent.class, true);

            gama.api.gaml.GAML.registerArtefactProtoFactory(gaml.compiler.prototypes.ArtefactFactory.getInstance());
            gama.api.gaml.GAML.registerDescriptionFactory(gaml.compiler.descriptions.DescriptionFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.ExperimentFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.ModelFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.PlatformFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.SpeciesFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.StatementFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.VariableFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.SkillFactory.getInstance());
            gama.api.gaml.GAML.registerSymbolFactory(gaml.compiler.factories.ClassFactory.getInstance());
            gama.api.gaml.GAML.registerExpressionFactory(gaml.compiler.expressions.GamlExpressionFactory.getInstance());
            gama.api.gaml.GAML.registerExpressionDescriptionFactory(gaml.compiler.factories.ExpressionDescriptionFactory.getInstance());
            gama.api.gaml.GAML.registerGamlContentProvider(gaml.compiler.resource.GamlResourceServices::getOrCreateSyntacticContents);
            gama.api.gaml.GAML.registerGamlModelBuilder(GamlModelBuilder.getInstance());
            gama.api.gaml.GAML.registerGamlTextValidator(gaml.compiler.validation.GamlTextValidator.getInstance());

            GamaBundleLoader.buildContributions();

            injector = GamlStandaloneSetup.doSetup();
            GamlStandaloneSetup.initializeAfterPlatformReady(injector);

            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Platform ready");
                appendConsole("GAMA platform initialized successfully.\n");
            });
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Platform init failed");
                appendConsole("Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            });
        }
    }

    private void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open GAMA Model");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GAMA Models", "*.gaml"));
        File file = fc.showOpenDialog(getPrimaryStage());
        if (file != null) {
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        currentFile = file;
        try {
            codeEditor.setText(new String(java.nio.file.Files.readAllBytes(file.toPath())));
            statusLabel.setText("Loaded: " + file.getName());
            validateBtn.setDisable(false);
            compileBtn.setDisable(false);
            runBtn.setDisable(true);
            experimentCombo.getItems().clear();
            currentModel = null;
            appendConsole("Loaded: " + file.getAbsolutePath() + "\n");
        } catch (Exception e) {
            appendConsole("Error loading: " + e.getMessage() + "\n");
        }
    }

    private void validateModel() {
        if (currentFile == null) return;
        statusLabel.setText("Validating...");
        appendConsole("Validating model...\n");
        new Thread(() -> compileOrValidate(true)).start();
    }

    private void compileModel() {
        if (currentFile == null) return;
        statusLabel.setText("Compiling...");
        appendConsole("Compiling model...\n");
        new Thread(() -> compileOrValidate(false)).start();
    }

    private void compileOrValidate(boolean validateOnly) {
        try {
            List<GamlCompilationError> errors = new ArrayList<>();
            IModelSpecies model = GamlModelBuilder.getInstance().compile(currentFile, errors, null);
            javafx.application.Platform.runLater(() -> {
                if (errors.isEmpty() && model != null) {
                    statusLabel.setText(validateOnly ? "Validation OK" : "Compilation OK");
                    appendConsole((validateOnly ? "Validation" : "Compilation") + " successful: " + model.getName() + "\n");
                    currentModel = model;
                    updateExperimentList(model);
                    if (!validateOnly) runBtn.setDisable(false);
                } else {
                    statusLabel.setText(validateOnly ? "Validation failed" : "Compilation failed");
                    appendConsole("Errors:\n");
                    for (GamlCompilationError err : errors) appendConsole("  - " + err.message() + "\n");
                }
            });
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Error");
                appendConsole("Error: " + e.getMessage() + "\n");
            });
        }
    }

    private void updateExperimentList(IModelSpecies model) {
        experimentCombo.getItems().clear();
        if (model != null) {
            for (IExperimentSpecies exp : model.getExperiments()) {
                experimentCombo.getItems().add(exp.getName());
            }
            if (!experimentCombo.getItems().isEmpty()) experimentCombo.getSelectionModel().selectFirst();
        }
    }

    private void runSimulation() {
        if (currentModel == null) return;
        String expName = experimentCombo.getSelectionModel().getSelectedItem();
        if (expName == null) { appendConsole("Select an experiment.\n"); return; }

        statusLabel.setText("Running...");
        runBtn.setDisable(true);
        stopBtn.setDisable(false);

        new Thread(() -> {
            try {
                appendConsole("Starting: " + expName + "\n");
                IExperimentSpecies experiment = currentModel.getExperiment(expName);
                if (experiment == null) { appendConsole("Experiment not found.\n"); return; }

                experiment.open(null);
                gama.api.kernel.agent.IAgent agent = experiment.getAgent();
                if (agent == null) { appendConsole("Failed to create agent.\n"); return; }

                for (int i = 0; i < 10; i++) {
                    agent.step(agent.getScope());
                    final int step = i + 1;
                    javafx.application.Platform.runLater(() -> appendConsole("Step " + step + "/10\n"));
                }

                appendConsole("Simulation completed.\n");
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Done");
                    runBtn.setDisable(false);
                    stopBtn.setDisable(true);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Error");
                    appendConsole("Error: " + e.getMessage() + "\n");
                    runBtn.setDisable(false);
                    stopBtn.setDisable(true);
                });
            }
        }).start();
    }

    private void appendConsole(String text) {
        javafx.application.Platform.runLater(() -> consoleOutput.appendText(text));
    }

    private Stage getPrimaryStage() {
        return (Stage) codeEditor.getScene().getWindow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
