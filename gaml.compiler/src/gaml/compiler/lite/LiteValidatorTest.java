package gaml.compiler.lite;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Injector;

import gama.api.additions.registries.ArtefactRegistry;
import gama.api.compilation.artefacts.IArtefact;
import gaml.compiler.prototypes.SymbolArtefact;

/**
 * A simple application to test the Lite GAML Validator.
 * 
 * It initializes the validator, reads the docGAMA.xml metamodels, parses a
 * sample GAML model in memory, and prints out any validation issues.
 */
public class LiteValidatorTest {

	public static void main(String[] args) {
		System.out.println("--- Starting Lite GAML Validator Test ---");
		
		// 1. Locate the GAMA workspace root (used to find docGAMA.xml files)
		// Change this path if testing in a different environment
		File gamaWorkspace = new File("/Users/hqnghi/git/hgama/gama");
		System.out.println("GAMA Workspace Root: " + gamaWorkspace.getAbsolutePath());

		// 2. Initialize Guice and the Lite bindings
		System.out.println("Initializing Xtext Injector in Lite mode...");
		Injector injector = LiteGamlStandaloneSetup.doSetup(gamaWorkspace);
		
		// 3. Populate the scopes from the XML files
		System.out.println("Loading docGAMA.xml metamodels...");
		long startTime = System.currentTimeMillis();
		LiteGamlStandaloneSetup.initializeLite(injector);
		long duration = System.currentTimeMillis() - startTime;
		System.out.println("Metamodels loaded in " + duration + " ms.");

		// 4. Create an in-memory Xtext resource
		System.out.println("\n--- Parsing Sample GAML Model ---");
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		Resource resource = resourceSet.createResource(URI.createURI("dummy.gaml"));
		
		// A sample GAML model
		String gamlCode = 
			"model DummyModel\n" +
			"global {\n" +
			"    init {\n" +
			"        int count <- 10;\n" +
			"        write \"Hello World!\";\n" +
			"        create my_agent number: count;\n" +
			"    }\n" +
			"}\n" +
			"species my_agent {\n" +
			"    float energy <- 100.0;\n" +
			"    aspect default {\n" +
			"        draw circle(5) color: #red;\n" +
			"    }\n" +
			"}\n" +
			"experiment my_exp type: gui {\n" +
			"    output {\n" +
			"        display map {\n" +
			"            species my_agent;\n" +
			"        }\n" +
			"    }\n" +
			"}\n";
		System.out.println(gamlCode);
		try {
			// Load the code into the resource
			resource.load(new ByteArrayInputStream(gamlCode.getBytes(StandardCharsets.UTF_8)),
					resourceSet.getLoadOptions());
			
			// Debug: check artefacts
			IArtefact.Symbol modelArtefact = ArtefactRegistry.getArtefact("model", null);
			System.out.println("Artefact for 'model': " + System.identityHashCode(modelArtefact));
			if (modelArtefact instanceof SymbolArtefact sa) {
				System.out.println("  has name facet: " + (sa.getFacet("name") != null));
				System.out.println("  omissible: " + sa.getOmissible());
			}
			ArtefactRegistry.writeStats();

			// 5. Validate the resource
			System.out.println("Validating model...");
			IResourceValidator validator = injector.getInstance(IResourceValidator.class);
			List<Issue> issues = validator.validate(resource, CheckMode.ALL, null);
			
			System.out.println("\n--- Validation Results ---");
			if (issues.isEmpty()) {
				System.out.println("✅ Model is perfectly valid! No errors or warnings.");
			} else {
				System.out.println("Found " + issues.size() + " issues:");
				for (Issue issue : issues) {
					System.out.println("- [" + issue.getSeverity() + "] line " + issue.getLineNumber() + ": " + issue.getMessage());
				}
			}
			
		} catch (Exception e) {
			System.err.println("Failed to parse or validate: ");
			e.printStackTrace();
		}
	}
}
