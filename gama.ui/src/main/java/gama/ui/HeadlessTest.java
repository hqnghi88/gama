package gama.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gama.api.GAMA;
import gama.api.additions.GamaBundleLoader;
import gama.api.additions.registries.AgentConstructorsRegistry;
import gama.api.compilation.GamlCompilationError;
import gama.api.kernel.species.IExperimentSpecies;
import gama.api.kernel.species.IModelSpecies;
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
import gama.api.types.matrix.GamaMatrixFactory;
import gama.api.types.geometry.GamaShapeFactory;
import gama.api.types.topology.GamaTopologyFactory;
import gama.api.types.graph.GamaPathFactory;
import gama.api.types.graph.GamaGraphFactory;
import gama.api.types.message.GamaMessageFactory;
import gaml.compiler.GamlStandaloneSetup;
import gaml.compiler.validation.GamlModelBuilder;

import com.google.inject.Injector;

/**
 * Headless test to verify the GAMA compilation and simulation APIs work correctly.
 */
public class HeadlessTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== GAMA UI Headless Test ===\n");

        // Step 1: Initialize core
        System.out.println("[1/5] Initializing GAMA core...");
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
        System.out.println("  PASS: Core initialized\n");

        // Step 2: Initialize GAML compiler
        System.out.println("[2/5] Initializing GAML compiler...");
        gaml.compiler.prototypes.ArtefactFactory af = gaml.compiler.prototypes.ArtefactFactory.getInstance();
        gaml.compiler.descriptions.DescriptionFactory df = gaml.compiler.descriptions.DescriptionFactory.getInstance();
        gama.api.gaml.GAML.registerArtefactProtoFactory(af);
        gama.api.gaml.GAML.registerDescriptionFactory(df);
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
        Injector injector = GamlStandaloneSetup.doSetup();
        GamlStandaloneSetup.initializeAfterPlatformReady(injector);
        System.out.println("  PASS: GAML compiler initialized\n");

        // Step 3: Compile model
        System.out.println("[3/5] Compiling model: " + args[0]);
        List<GamlCompilationError> errors = new ArrayList<>();
        IModelSpecies model = GamlModelBuilder.getInstance().compile(new File(args[0]), errors, null);
        if (model == null) {
            System.out.println("  FAIL: Model compilation failed");
            for (GamlCompilationError e : errors) {
                System.out.println("    - " + e.message());
            }
            System.exit(1);
        }
        System.out.println("  PASS: Model compiled: " + model.getName() + "\n");

        // Step 4: List experiments
        System.out.println("[4/5] Listing experiments...");
        for (IExperimentSpecies exp : model.getExperiments()) {
            System.out.println("  Experiment: " + exp.getName() + " (type=" + exp.getExperimentType() + ")");
        }
        System.out.println("  PASS\n");

        // Step 5: Run simulation
        String expName = args.length > 1 ? args[1] : null;
        int steps = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        if (expName == null) {
            IExperimentSpecies first = model.getExperiments().iterator().next();
            expName = first != null ? first.getName() : null;
        }

        if (expName != null) {
            System.out.println("[5/5] Running experiment '" + expName + "' for " + steps + " steps...");
            IExperimentSpecies experiment = model.getExperiment(expName);
            experiment.open(null);
            gama.api.kernel.agent.IAgent agent = experiment.getAgent();
            if (agent == null) {
                System.out.println("  FAIL: Could not create experiment agent");
                System.exit(1);
            }
            for (int i = 0; i < steps; i++) {
                agent.step(agent.getScope());
                System.out.println("  Step " + (i + 1) + "/" + steps + " OK");
            }
            System.out.println("  PASS\n");
        }

        System.out.println("=== All tests passed ===");
        System.exit(0);
    }
}
