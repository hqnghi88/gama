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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gama.api.GAMA;
import gama.api.additions.GamaBundleLoader;
import gama.api.compilation.GamlCompilationError;
import gama.api.kernel.species.IExperimentSpecies;
import gama.api.kernel.species.IModelSpecies;
import gama.core.standalone.StandaloneLauncher;
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

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GAMA Model Editor & Simulator");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));

        // Toolbar
        root.setTop(createToolBar());

        // Split pane: editor + console
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

        Scene scene = new Scene(root, 1200, 800);
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

        return new ToolBar(openBtn, new Separator(), validateBtn, compileBtn,
                new Separator(), runBtn, stopBtn, new Separator(), clearBtn);
    }

    private void initializePlatform() {
        try {
            gama.api.types.matrix.GamaMatrixFactory.setBuilder(new gama.core.util.matrix.InternalGamaMatrixFactory());
            gama.api.types.geometry.GamaShapeFactory.setBuilder(new gama.core.geometry.InternalGamaShapeFactory());
            gama.api.types.topology.GamaTopologyFactory.setBuilder(new gama.core.topology.InternalTopologyFactory());
            gama.api.types.graph.GamaPathFactory.setBuilder(new gama.core.util.path.InternalGamaPathFactory());
            gama.api.types.graph.GamaGraphFactory.setBuilder(new gama.core.util.graph.InternalGamaGraphFactory());
            gama.api.types.message.GamaMessageFactory.setBuilder(new gama.core.util.messaging.GamaMessage.Factory());

            gama.gaml.operators.Dates.initialize();
            gama.api.GAMA.setJsonEncoder(gama.core.util.json.Json.getNew());
            gama.api.GAMA.setWorkspaceManager(new gama.core.standalone.StubWorkspaceManager());

            final gama.api.ui.IGui stubGui = gama.core.standalone.StubGui.create();
            gama.api.GAMA.setHeadlessGui(stubGui);
            gama.api.GAMA.setRegularGui(stubGui);

            gama.api.additions.registries.AgentConstructorsRegistry.register(gama.core.agent.GamlAgent.class, false);
            gama.api.additions.registries.AgentConstructorsRegistry.register(gama.core.agent.MinimalAgent.class, true);

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
            gama.api.gaml.GAML.registerGamlModelBuilder(gaml.compiler.validation.GamlModelBuilder.getInstance());
            gama.api.gaml.GAML.registerGamlTextValidator(gaml.compiler.validation.GamlTextValidator.getInstance());

            gama.api.additions.GamaBundleLoader.buildContributions();

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
