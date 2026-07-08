/*******************************************************************************************************
 *
 * DocGamlMetamodel.java, in gaml.compiler, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.compiler.lite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parses one or more {@code docGAMA.xml} files and extracts the GAML language metamodel as plain
 * {@code String} sets. This class has <strong>zero</strong> runtime dependency on {@code gama.core},
 * {@code gama.api}, or any execution-related class — it works purely with the XML documentation
 * files produced by {@code gama.processor}.
 *
 * <p>
 * The extracted sets correspond exactly to what
 * {@link gaml.compiler.scoping.BuiltinGlobalScopeProvider#initialize()} populates from the live
 * platform registries:
 * <ul>
 * <li><strong>types</strong> — GAML type names (int, float, list, map, agent, …)</li>
 * <li><strong>operators</strong> — GAML operator names (+, -, *, union, …)</li>
 * <li><strong>skills</strong> — skill names (moving, grid, …)</li>
 * <li><strong>species</strong> — built-in species names (agent, experiment, model, …)</li>
 * <li><strong>statements</strong> — statement keyword names (if, loop, create, ask, …)</li>
 * <li><strong>actions</strong> — built-in action names (die, do, …)</li>
 * <li><strong>variables</strong> — built-in variable names (name, shape, location, …)</li>
 * <li><strong>constants</strong> — constant names (nil, true, false, …)</li>
 * <li><strong>units</strong> — unit names (#m, #km, #s, …)</li>
 * <li><strong>files</strong> — file type names (shape, csv, text, …)</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * DocGamlMetamodel metamodel = new DocGamlMetamodel();
 * metamodel.loadFromDirectory(new File("path/to/gama"));
 * Set<String> operators = metamodel.getOperatorNames();
 * }</pre>
 *
 * @author GAMA Development Team
 * @since 2026
 */
public class DocGamlMetamodel {

	// ---- Collected names (all insertion-ordered for deterministic scoping) ----

	private final Set<String> typeNames = new LinkedHashSet<>();
	private final Set<String> operatorNames = new LinkedHashSet<>();
	private final Set<String> skillNames = new LinkedHashSet<>();
	private final Set<String> speciesNames = new LinkedHashSet<>();
	private final Set<String> statementNames = new LinkedHashSet<>();
	private final Set<String> actionNames = new LinkedHashSet<>();
	private final Set<String> variableNames = new LinkedHashSet<>();
	private final Set<String> constantNames = new LinkedHashSet<>();
	private final Set<String> unitNames = new LinkedHashSet<>();
	private final Set<String> fileTypeNames = new LinkedHashSet<>();

	/**
	 * The well-known built-in GAML types that are always present in the language, even when
	 * no docGAMA.xml files are available. These are defined by {@code gama.api.gaml.types.Types}
	 * and are fundamental to the language's type system.
	 */
	private static final String[] BUILTIN_TYPE_NAMES = {
		"int", "float", "bool", "string", "list", "map", "pair", "matrix",
		"point", "geometry", "agent", "species", "container", "path", "graph",
		"file", "topology", "font", "date", "message", "material",
		"action", "type", "field", "kml", "unknown", "regression",
		"date_interval", "color", "dataframe"
	};

	/**
	 * The well-known built-in constants in GAML.
	 */
	private static final String[] BUILTIN_CONSTANTS = {
		"nil", "true", "false", "unknown",
		"each", "self", "myself", "super",
		// colors
		"white", "black", "red", "green", "blue", "yellow",
		"cyan", "magenta", "orange", "pink", "gray", "grey",
		"darkgray", "darkgrey", "lightgray", "lightgrey",
		"transparent",
		// math
		"pi", "e", "to_deg", "to_rad",
		"max_float", "min_float", "max_int", "min_int",
		"infinity", "nan",
		// display
		"flat", "circle", "square", "triangle",
		"sphere", "cube", "cylinder", "cone",
		"teapot", "pyramid", "diamond",
		// misc
		"default", "world", "simulation"
	};

	/**
	 * Well-known unit names (e.g. m, km, cm, etc.)
	 */
	private static final String[] BUILTIN_UNITS = {
		// length
		"m", "cm", "dm", "mm", "km", "mile", "yard", "inch", "foot", "nm",
		// time
		"s", "mn", "min", "h", "hour", "d", "day", "week",
		"month", "year", "ms", "msec",
		"cycle", "cycles", "step",
		// surface
		"m2", "km2", "ha",
		// volume
		"m3", "dm3", "cm3", "l", "cl", "dl", "hl",
		// weight
		"kg", "g", "mg", "ton", "ounce", "pound",
		// display/angle
		"px", "°", "rad", "deg",
		// misc
		"percent"
	};

	/** Creates an empty metamodel. Call one of the {@code load*} methods to populate it. */
	public DocGamlMetamodel() {
		// Seed with built-in names so even without XML, basic types are available
		Collections.addAll(typeNames, BUILTIN_TYPE_NAMES);
		Collections.addAll(constantNames, BUILTIN_CONSTANTS);
		Collections.addAll(unitNames, BUILTIN_UNITS);
	}

	// ===========================================================================
	// Loading
	// ===========================================================================

	/**
	 * Scans a GAMA workspace root directory for all {@code docGAMA.xml} files (in
	 * {@code <plugin>/gaml/docGAMA.xml}) and loads them.
	 *
	 * @param gamaRoot the root directory of the GAMA workspace (e.g. {@code /path/to/gama})
	 * @return this metamodel, for chaining
	 */
	public DocGamlMetamodel loadFromDirectory(final File gamaRoot) {
		if (gamaRoot == null || !gamaRoot.isDirectory()) return this;
		final File[] children = gamaRoot.listFiles();
		if (children == null) return this;
		for (final File child : children) {
			if (!child.isDirectory()) continue;
			final File gamlDir = new File(child, "gaml");
			final File docFile = new File(gamlDir, "docGAMA.xml");
			if (docFile.isFile()) {
				loadFile(docFile);
			}
		}
		return this;
	}

	/**
	 * Loads a single {@code docGAMA.xml} file.
	 *
	 * @param file the XML file to parse
	 * @return this metamodel, for chaining
	 */
	public DocGamlMetamodel loadFile(final File file) {
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Disable external entities for security
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(file);
			doc.getDocumentElement().normalize();
			parseDocument(doc);
		} catch (final Exception e) {
			System.err.println("[DocGamlMetamodel] Warning: Failed to parse " + file.getAbsolutePath() + ": " + e.getMessage());
		}
		return this;
	}

	/**
	 * Loads from an InputStream (useful for classpath resources).
	 *
	 * @param stream the input stream containing the XML
	 * @return this metamodel, for chaining
	 */
	public DocGamlMetamodel loadFromStream(final InputStream stream) {
		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(stream);
			doc.getDocumentElement().normalize();
			parseDocument(doc);
		} catch (final Exception e) {
			System.err.println("[DocGamlMetamodel] Warning: Failed to parse stream: " + e.getMessage());
		}
		return this;
	}

	// ===========================================================================
	// Parsing
	// ===========================================================================

	/**
	 * Parses all sections of a docGAMA.xml document and populates the name sets.
	 */
	private void parseDocument(final Document doc) {
		parseOperators(doc);
		parseSkills(doc);
		parseSpecies(doc);
		parseStatements(doc);
		parseFiles(doc);
		parseTypes(doc);
		parseConstants(doc);
	}

	/**
	 * Extracts operator names from {@code <operators><operator name="..." id="...">}.
	 * Also collects alternative names via the {@code alternativeNameOf} attribute.
	 */
	private void parseOperators(final Document doc) {
		final NodeList operators = doc.getElementsByTagName("operator");
		for (int i = 0; i < operators.getLength(); i++) {
			final Element el = (Element) operators.item(i);
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				operatorNames.add(name);
			}
			final String id = el.getAttribute("id");
			if (id != null && !id.isEmpty() && !id.equals(name)) {
				operatorNames.add(id);
			}
		}
	}

	/**
	 * Extracts skill names, their variables, and their actions from {@code <skills><skill>}.
	 */
	private void parseSkills(final Document doc) {
		final NodeList skills = doc.getElementsByTagName("skill");
		for (int i = 0; i < skills.getLength(); i++) {
			final Element el = (Element) skills.item(i);
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				skillNames.add(name);
			}
			// Extract variables defined in skills
			extractVarsFrom(el);
			// Extract actions defined in skills
			extractActionsFrom(el);
		}
	}

	/**
	 * Extracts species names, their variables, and their actions from {@code <speciess><species>}.
	 */
	private void parseSpecies(final Document doc) {
		final NodeList species = doc.getElementsByTagName("species");
		for (int i = 0; i < species.getLength(); i++) {
			final Element el = (Element) species.item(i);
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				speciesNames.add(name);
				// Species names are also valid types
				typeNames.add(name);
			}
			extractVarsFrom(el);
			extractActionsFrom(el);
		}
	}

	/**
	 * Extracts statement names and their facets from {@code <statements><statement>}.
	 */
	private void parseStatements(final Document doc) {
		final NodeList statements = doc.getElementsByTagName("statement");
		for (int i = 0; i < statements.getLength(); i++) {
			final Element el = (Element) statements.item(i);
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				statementNames.add(name);
			}
			final String id = el.getAttribute("id");
			if (id != null && !id.isEmpty() && !id.equals(name)) {
				statementNames.add(id);
			}
		}
	}

	/**
	 * Extracts file type names from {@code <files><file>}.
	 */
	private void parseFiles(final Document doc) {
		final NodeList files = doc.getElementsByTagName("file");
		for (int i = 0; i < files.getLength(); i++) {
			final Element el = (Element) files.item(i);
			// Only direct children of <files> element
			if (!"files".equals(el.getParentNode().getNodeName())) continue;
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				fileTypeNames.add(name);
				// file types are also registered as types (e.g., "csv_file")
				typeNames.add(name + "_file");
			}
		}
	}

	/**
	 * Extracts type names from {@code <types><type>}.
	 */
	private void parseTypes(final Document doc) {
		final NodeList types = doc.getElementsByTagName("type");
		for (int i = 0; i < types.getLength(); i++) {
			final Element el = (Element) types.item(i);
			// Only direct children of <types> element
			if (!"types".equals(el.getParentNode().getNodeName())) continue;
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				typeNames.add(name);
			}
		}
	}

	/**
	 * Extracts constant names from {@code <constants><constant>}.
	 */
	private void parseConstants(final Document doc) {
		final NodeList constants = doc.getElementsByTagName("constant");
		for (int i = 0; i < constants.getLength(); i++) {
			final Element el = (Element) constants.item(i);
			final String name = el.getAttribute("name");
			if (name != null && !name.isEmpty()) {
				constantNames.add(name);
			}
		}
	}

	/**
	 * Extracts variable names from {@code <vars><var>} within a given parent element.
	 */
	private void extractVarsFrom(final Element parent) {
		final NodeList vars = parent.getElementsByTagName("var");
		for (int j = 0; j < vars.getLength(); j++) {
			final Element varEl = (Element) vars.item(j);
			final String varName = varEl.getAttribute("name");
			if (varName != null && !varName.isEmpty()) {
				variableNames.add(varName);
			}
		}
	}

	/**
	 * Extracts action names from {@code <actions><action>} within a given parent element.
	 */
	private void extractActionsFrom(final Element parent) {
		final NodeList actions = parent.getElementsByTagName("action");
		for (int j = 0; j < actions.getLength(); j++) {
			final Element actionEl = (Element) actions.item(j);
			final String actionName = actionEl.getAttribute("name");
			if (actionName != null && !actionName.isEmpty()) {
				actionNames.add(actionName);
			}
		}
	}

	// ===========================================================================
	// Accessors
	// ===========================================================================

	/** Returns all GAML type names (built-in + parsed from XML). */
	public Set<String> getTypeNames() { return Collections.unmodifiableSet(typeNames); }

	/** Returns all GAML operator names. */
	public Set<String> getOperatorNames() { return Collections.unmodifiableSet(operatorNames); }

	/** Returns all GAML skill names. */
	public Set<String> getSkillNames() { return Collections.unmodifiableSet(skillNames); }

	/** Returns all built-in GAML species names. */
	public Set<String> getSpeciesNames() { return Collections.unmodifiableSet(speciesNames); }

	/** Returns all GAML statement keyword names. */
	public Set<String> getStatementNames() { return Collections.unmodifiableSet(statementNames); }

	/** Returns all GAML action names (from species + skills). */
	public Set<String> getActionNames() { return Collections.unmodifiableSet(actionNames); }

	/** Returns all built-in GAML variable names (from species + skills). */
	public Set<String> getVariableNames() { return Collections.unmodifiableSet(variableNames); }

	/** Returns all GAML constant names. */
	public Set<String> getConstantNames() { return Collections.unmodifiableSet(constantNames); }

	/** Returns all GAML unit names. */
	public Set<String> getUnitNames() { return Collections.unmodifiableSet(unitNames); }

	/** Returns all GAML file type names. */
	public Set<String> getFileTypeNames() { return Collections.unmodifiableSet(fileTypeNames); }

	/**
	 * Returns all names that should be registered in the Xtext scope as "actions" (operators + actions).
	 * This mirrors how {@link gaml.compiler.scoping.BuiltinGlobalScopeProvider} registers operators
	 * as action artefacts.
	 */
	public Set<String> getAllActionScopeNames() {
		final Set<String> all = new LinkedHashSet<>(operatorNames);
		all.addAll(actionNames);
		return Collections.unmodifiableSet(all);
	}

	/**
	 * Returns all names that should be registered in the Xtext scope as "variables".
	 * This includes built-in variables, type names (which double as var definitions),
	 * constants, and skill names (which also act as var definitions).
	 */
	public Set<String> getAllVarScopeNames() {
		final Set<String> all = new LinkedHashSet<>(variableNames);
		all.addAll(typeNames);
		all.addAll(constantNames);
		all.addAll(skillNames);
		return Collections.unmodifiableSet(all);
	}

	/**
	 * Returns a summary of the loaded metamodel for diagnostic purposes.
	 */
	public String getSummary() {
		return String.format(
			"DocGamlMetamodel: %d types, %d operators, %d skills, %d species, " +
			"%d statements, %d actions, %d vars, %d constants, %d units, %d file types",
			typeNames.size(), operatorNames.size(), skillNames.size(), speciesNames.size(),
			statementNames.size(), actionNames.size(), variableNames.size(),
			constantNames.size(), unitNames.size(), fileTypeNames.size());
	}
}
