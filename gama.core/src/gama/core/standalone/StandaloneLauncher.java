/*******************************************************************************************************
 *
 * StandaloneLauncher.java, in gama.core, is part of the source code of the GAMA modeling and simulation platform
 * (v.2025-03).
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.standalone;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Injector;

import gama.api.GAMA;
import gama.api.additions.GamaBundleLoader;
import gama.api.additions.registries.AgentConstructorsRegistry;
import gama.api.compilation.GamlCompilationError;
import gama.api.gaml.GAML;
import gama.api.kernel.species.IExperimentSpecies;
import gama.api.kernel.species.IModelSpecies;
import gama.api.types.geometry.GamaShapeFactory;
import gama.api.types.graph.GamaGraphFactory;
import gama.api.types.graph.GamaPathFactory;
import gama.api.types.matrix.GamaMatrixFactory;
import gama.api.types.message.GamaMessageFactory;
import gama.api.types.topology.GamaTopologyFactory;
import gama.core.geometry.InternalGamaShapeFactory;
import gama.core.topology.InternalTopologyFactory;
import gama.core.util.graph.InternalGamaGraphFactory;
import gama.core.util.path.InternalGamaPathFactory;
import gama.core.util.matrix.InternalGamaMatrixFactory;
import gama.core.util.messaging.GamaMessage;
import gama.core.agent.GamlAgent;
import gama.core.agent.MinimalAgent;
import gama.core.util.json.Json;
import gama.gaml.operators.Dates;
import gaml.compiler.GamlStandaloneSetup;
import gaml.compiler.validation.GamlModelBuilder;
import gaml.compiler.prototypes.ArtefactFactory;
import gaml.compiler.descriptions.DescriptionFactory;
import gaml.compiler.factories.ExperimentFactory;
import gaml.compiler.factories.ModelFactory;
import gaml.compiler.factories.PlatformFactory;
import gaml.compiler.factories.SpeciesFactory;
import gaml.compiler.factories.StatementFactory;
import gaml.compiler.factories.VariableFactory;
import gaml.compiler.factories.SkillFactory;
import gaml.compiler.factories.ClassFactory;
import gaml.compiler.expressions.GamlExpressionFactory;
import gaml.compiler.factories.ExpressionDescriptionFactory;
import gaml.compiler.resource.GamlResourceServices;
import gaml.compiler.validation.GamlTextValidator;

/**
 * Standalone entry-point launcher for running GAMA simulations without Eclipse/OSGi.
 *
 * <p>
 * This class provides a simple {@link #main(String[])} method that bootstraps the GAMA platform in standalone mode,
 * compiles a GAML model file, and executes the specified experiment for a given number of simulation steps.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>
 * java -cp &lt;classpath&gt; gama.core.standalone.StandaloneLauncher &lt;model.gaml&gt; &lt;experiment_name&gt; [num_steps]
 * </pre>
 *
 * <h3>Initialization Sequence</h3>
 * <ol>
 * <li>Initialize core data type factories (shapes, matrices, graphs, etc.)</li>
 * <li>Register agent constructors</li>
 * <li>Set up JSON encoder</li>
 * <li>Build GAML language contributions via ServiceLoader (standalone mode)</li>
 * <li>Initialize Xtext runtime for GAML parsing</li>
 * <li>Compile the model file</li>
 * <li>Open the experiment and run simulation steps</li>
 * </ol>
 *
 * @author GAMA Team
 * @since 2026
 */
public class StandaloneLauncher {

	/**
	 * Initializes the core GAMA platform factories and services.
	 * This mirrors {@code CoreActivator.initializeFactories()} but without OSGi dependencies.
	 */
	private static void initializeCore() {
		// Register concrete factory implementations for GAMA data types
		GamaMatrixFactory.setBuilder(new InternalGamaMatrixFactory());
		GamaShapeFactory.setBuilder(new InternalGamaShapeFactory());
		GamaTopologyFactory.setBuilder(new InternalTopologyFactory());
		GamaPathFactory.setBuilder(new InternalGamaPathFactory());
		GamaGraphFactory.setBuilder(new InternalGamaGraphFactory());
		GamaMessageFactory.setBuilder(new GamaMessage.Factory());

		// Initialize date utilities
		Dates.initialize();

		// Set up JSON encoder
		GAMA.setJsonEncoder(Json.getNew());

		// Set up workspace manager for standalone mode
		GAMA.setWorkspaceManager(new StubWorkspaceManager());

		// Set up a no-op headless GUI proxy
		final gama.api.ui.IGui stubGui = StubGui.create();
		GAMA.setHeadlessGui(stubGui);
		GAMA.setRegularGui(stubGui);

		// Register agent constructors
		AgentConstructorsRegistry.register(GamlAgent.class, false);
		AgentConstructorsRegistry.register(MinimalAgent.class, true);

		// Initialize GAML compiler factories (normally done by GamlActivator.start() in OSGi)
		GAML.registerArtefactProtoFactory(ArtefactFactory.getInstance());
		GAML.registerDescriptionFactory(DescriptionFactory.getInstance());
		GAML.registerSymbolFactory(ExperimentFactory.getInstance());
		GAML.registerSymbolFactory(ModelFactory.getInstance());
		GAML.registerSymbolFactory(PlatformFactory.getInstance());
		GAML.registerSymbolFactory(SpeciesFactory.getInstance());
		GAML.registerSymbolFactory(StatementFactory.getInstance());
		GAML.registerSymbolFactory(VariableFactory.getInstance());
		GAML.registerSymbolFactory(SkillFactory.getInstance());
		GAML.registerSymbolFactory(ClassFactory.getInstance());
		GAML.registerExpressionFactory(GamlExpressionFactory.getInstance());
		GAML.registerExpressionDescriptionFactory(ExpressionDescriptionFactory.getInstance());
		GAML.registerGamlContentProvider(GamlResourceServices::getOrCreateSyntacticContents);
		GAML.registerGamlModelBuilder(GamlModelBuilder.getInstance());
		GAML.registerGamlTextValidator(GamlTextValidator.getInstance());

		// Build all GAML language contributions via ServiceLoader (standalone mode)
		GamaBundleLoader.buildContributions();
	}

	/**
	 * Main entry point for standalone GAMA simulation execution.
	 *
	 * @param args
	 *            command-line arguments: {@code <model.gaml> <experiment_name> [num_steps]}
	 */
	public static void main(final String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: StandaloneLauncher <model.gaml> <experiment_name> [num_steps]");
			System.err.println("  model.gaml       - path to the GAML model file");
			System.err.println("  experiment_name  - name of the experiment to run");
			System.err.println("  num_steps        - number of simulation steps (default: 10)");
			System.exit(1);
		}

		final String modelPath = args[0];
		final String experimentName = args[1];
		final int numSteps = args.length > 2 ? Integer.parseInt(args[2]) : 10;

		System.out.println("GAMA Standalone Launcher");
		System.out.println("========================");
		System.out.println("Model: " + modelPath);
		System.out.println("Experiment: " + experimentName);
		System.out.println("Steps: " + numSteps);
		System.out.println();

		// Step 1: Initialize core platform
		System.out.println("Initializing GAMA core...");
		initializeCore();

		// Step 2: Initialize Xtext for GAML parsing
		System.out.println("Initializing GAML compiler...");
		final Injector injector = GamlStandaloneSetup.doSetup();

		// Step 3: Post-initialization (scope providers, etc.)
		GamlStandaloneSetup.initializeAfterPlatformReady(injector);
		System.out.println("Platform initialized successfully.");
		System.out.println();

		// Step 4: Compile the model
		System.out.println("Compiling model...");
		final List<GamlCompilationError> errors = new ArrayList<>();
		final IModelSpecies model;
		try {
			model = GamlModelBuilder.getInstance().compile(new File(modelPath), errors, null);
		} catch (final Exception ex) {
			System.err.println("Model compilation failed: " + ex.getMessage());
			System.exit(1);
			return;
		}

		if (model == null) {
			System.err.println("Model compilation failed:");
			for (final GamlCompilationError e : errors) {
				System.err.println("  " + e.message());
			}
			System.exit(1);
		}
		System.out.println("Model compiled successfully: " + model.getName());
		System.out.println();

		// Step 5: Get the experiment
		final IExperimentSpecies experiment = model.getExperiment(experimentName);
		if (experiment == null) {
			System.err.println("Experiment '" + experimentName + "' not found in model.");
			System.err.println("Available experiments:");
			// model.getExperiments() may not be directly available, but we can try
			System.exit(1);
		}

		// Step 6: Open the experiment (creates the experiment agent and first simulation)
		System.out.println("Opening experiment...");
		experiment.open(null);

		// Step 7: Run simulation steps
		System.out.println("Running simulation...");
		final gama.api.kernel.agent.IAgent experimentAgent = experiment.getAgent();
		if (experimentAgent == null) {
			System.err.println("Failed to create experiment agent.");
			System.exit(1);
		}

		for (int i = 0; i < numSteps; i++) {
			experimentAgent.step(experimentAgent.getScope());
			System.out.println("  Step " + (i + 1) + "/" + numSteps + " completed");
		}

		System.out.println();
		System.out.println("Simulation completed successfully.");
		System.exit(0);
	}
}
