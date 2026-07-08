/*******************************************************************************************************
 *
 * LiteGamlStandaloneSetup.java, in gaml.compiler, is part of the source code of the GAMA modeling and simulation
 * platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/

package gaml.compiler.lite;

import java.io.File;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;

import com.google.inject.Guice;
import com.google.inject.Injector;

import gama.api.gaml.GAML;
import gaml.compiler.GamlStandaloneSetupGenerated;
import gaml.compiler.descriptions.DescriptionFactory;
import gaml.compiler.expressions.GamlExpressionFactory;
import gaml.compiler.factories.ClassFactory;
import gaml.compiler.factories.ExperimentFactory;
import gaml.compiler.factories.ExpressionDescriptionFactory;
import gaml.compiler.factories.ModelFactory;
import gaml.compiler.factories.PlatformFactory;
import gaml.compiler.factories.SkillFactory;
import gaml.compiler.factories.SpeciesFactory;
import gaml.compiler.factories.StatementFactory;
import gaml.compiler.factories.VariableFactory;
import gaml.compiler.gaml.GamlPackage;
import gaml.compiler.prototypes.ArtefactFactory;
import gaml.compiler.resource.GamlResourceServices;
import gaml.compiler.validation.GamlModelBuilder;
import gaml.compiler.validation.GamlTextValidator;

/**
 * Initialization support for running Xtext languages without the Equinox extension registry
 * and without loading the heavy execution classes from {@code gama.core}.
 *
 * <p>
 * This class uses {@link LiteGamlRuntimeModule} which binds the global scope provider
 * to {@link LiteBuiltinGlobalScopeProvider}.
 * </p>
 */
public class LiteGamlStandaloneSetup extends GamlStandaloneSetupGenerated {

	/**
	 * Creates the Guice injector, registers EMF resource factories and Xtext service providers
	 * for the {@code .gaml} extension, and returns the injector.
	 *
	 * @return the Guice injector for the GAML language in lite mode
	 */
	public static Injector doSetup(File gamaWorkspaceRoot) {
		LiteBuiltinGlobalScopeProvider.GAMA_WORKSPACE_ROOT = gamaWorkspaceRoot;
		return new LiteGamlStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	@Override
	public Injector createInjector() {
		return Guice.createInjector(new LiteGamlRuntimeModule());
	}

	@Override
	public void register(Injector injector) {
		if (!EPackage.Registry.INSTANCE.containsKey("http://www.gama-platform.org/Gaml")) {
			EPackage.Registry.INSTANCE.put("http://www.gama-platform.org/Gaml", GamlPackage.eINSTANCE);
		}
		super.register(injector);
	}

	/**
	 * Eagerly instantiates the lite Xtext singletons that depend on the XML metamodel.
	 *
	 * @param injector the injector returned by {@link #doSetup(File)}
	 */
	public static void initializeLite(final Injector injector) {
		((LiteBuiltinGlobalScopeProvider) injector.getInstance(IGlobalScopeProvider.class)).initialize();

		// Initialize the type system (normally done by GamaBundleLoader in OSGi mode)
		new LiteTypeInitializer().initialize();

		// Register minimal artefacts for the keywords used in the test model
		new LiteArtefactInitializer().initialize();

		// Register factories (normally done by GamlActivator in OSGi mode)
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
	}
}
