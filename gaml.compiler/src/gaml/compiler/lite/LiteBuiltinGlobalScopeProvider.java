/*******************************************************************************************************
 *
 * LiteBuiltinGlobalScopeProvider.java, in gaml.compiler, is part of the source code of the GAMA modeling and simulation
 * platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.compiler.lite;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.SelectableBasedScope;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import gama.api.compilation.factories.IExpressionFactory;
import gaml.compiler.EGaml;
import gaml.compiler.gaml.GamlDefinition;
import gaml.compiler.gaml.GamlPackage;
import gaml.compiler.indexer.GamlResourceIndexer;
import gaml.compiler.resource.GamlResource;

/**
 * Global GAML scope provider supporting built-in definitions WITHOUT executing java classes.
 * <p>
 * This provider reads from docGAMA.xml files instead of GamaBundleLoader.
 * </p>
 */
@Singleton
@SuppressWarnings ({ "unchecked", "rawtypes" })
public class LiteBuiltinGlobalScopeProvider extends ImportUriGlobalScopeProvider {

	class EClassBasedScope implements IScope {

		final Resource resource;
		final Map<QualifiedName, IEObjectDescription> elements = new HashMap<>();
		private final Map<URI, IEObjectDescription> uriCache = new HashMap<>();
		private Collection<IEObjectDescription> cachedValues;

		public EClassBasedScope(final String uri) {
			final URI resourceUri = URI.createURI(uri, false);
			Resource r = rs.getResource(resourceUri, false);
			if (r == null) { r = rs.createResource(resourceUri); }
			resource = r;
		}

		@Override
		public IEObjectDescription getSingleElement(final QualifiedName name) {
			return elements.get(name);
		}

		@Override
		public Iterable<IEObjectDescription> getAllElements() {
			if (cachedValues == null) { cachedValues = Collections.unmodifiableCollection(elements.values()); }
			return cachedValues;
		}

		@Override
		public Iterable<IEObjectDescription> getElements(final QualifiedName name) {
			final IEObjectDescription result = elements.get(name);
			if (result == null) return Collections.emptyList();
			return Collections.singleton(result);
		}

		@Override
		public IEObjectDescription getSingleElement(final EObject object) {
			final URI uri = EcoreUtil2.getPlatformResourceOrNormalizedURI(object);
			IEObjectDescription cached = uriCache.get(uri);
			if (cached != null && (cached.getEObjectOrProxy() == object || uri.equals(cached.getEObjectURI())))
				return cached;

			for (IEObjectDescription input : elements.values()) {
				if (input.getEObjectOrProxy() == object || uri.equals(input.getEObjectURI())) {
					uriCache.put(uri, input);
					return input;
				}
			}
			return null;
		}

		@Override
		public List<IEObjectDescription> getElements(final EObject object) {
			final IEObjectDescription result = getSingleElement(object);
			return result != null ? Collections.singletonList(result) : Collections.emptyList();
		}

		public void add(final QualifiedName name, final GamlDefinition stub) {
			resource.getContents().add(stub);
			final IEObjectDescription desc = EObjectDescription.create(name, stub);
			elements.put(name, desc);
			uriCache.put(EcoreUtil2.getPlatformResourceOrNormalizedURI(stub), desc);
			cachedValues = null;
		}

	}

	private final Map<EClass, EClassBasedScope> scopes = new HashMap<>();
	private final Set<QualifiedName> allQualifiedNames = new HashSet<>();
	private final EClass eType, eVar, eSkill, eAction, eUnit, eEquation;
	private final XtextResourceSet rs = new XtextResourceSet();
	private volatile boolean initialized = false;

	// Note: We need to know where the GAMA workspace root is.
	// For LSP and headless mode, this should be set before initialization.
	public static File GAMA_WORKSPACE_ROOT = null;

	public LiteBuiltinGlobalScopeProvider() {
		eType = GamlPackage.eINSTANCE.getTypeDefinition();
		eVar = GamlPackage.eINSTANCE.getVarDefinition();
		eSkill = GamlPackage.eINSTANCE.getSkillFakeDefinition();
		eAction = GamlPackage.eINSTANCE.getActionDefinition();
		eUnit = GamlPackage.eINSTANCE.getUnitFakeDefinition();
		eEquation = GamlPackage.eINSTANCE.getEquationDefinition();
		scopes.put(eType, new EClassBasedScope("types.xmi"));
		scopes.put(eVar, new EClassBasedScope("vars.xmi"));
		scopes.put(eSkill, new EClassBasedScope("skills.xmi"));
		scopes.put(eAction, new EClassBasedScope("units.xmi"));
		scopes.put(eUnit, new EClassBasedScope("actions.xmi"));
		scopes.put(eEquation, new EClassBasedScope("equations.xmi"));
	}

	public synchronized void initialize() {
		if (initialized) return;
		initialized = true;

		final DocGamlMetamodel metamodel = new DocGamlMetamodel();
		if (GAMA_WORKSPACE_ROOT != null) {
			metamodel.loadFromDirectory(GAMA_WORKSPACE_ROOT);
		}

		add(IExpressionFactory.TEMPORARY_ACTION_NAME, eAction);
		
		metamodel.getTypeNames().forEach(t -> add(t, eType, eVar, eAction));
		metamodel.getConstantNames().forEach(t -> add(t, eType, eVar, eUnit));
		metamodel.getUnitNames().forEach(t -> add(t, eUnit));
		// Variables
		metamodel.getAllVarScopeNames().forEach(t -> add(t, eVar));
		// Skills
		metamodel.getSkillNames().forEach(t -> add(t, eSkill, eVar));
		// Actions / Operators
		metamodel.getAllActionScopeNames().forEach(t -> add(t, eAction));
	}

	private void ensureInitialized() {
		if (!initialized) { initialize(); }
	}

	public boolean contains(final QualifiedName name) {
		ensureInitialized();
		return allQualifiedNames.contains(name);
	}

	void add(final String t, final EClass... classes) {
		final QualifiedName qName = QualifiedName.create(t);
		allQualifiedNames.add(qName);
		final EGaml eGaml = EGaml.getInstance();
		for (final EClass eClass : classes) { scopes.get(eClass).add(qName, eGaml.createGamlDefinition(t, eClass)); }
	}

	@Override
	protected IScope getScope(final Resource resource, final boolean ignoreCase, final EClass type,
			final Predicate<IEObjectDescription> filter) {
		ensureInitialized();
		IScope scope = scopes.get(type);
		Collection<URI> imports = GamlResourceIndexer.allImportsOf((GamlResource) resource).keySet();
		int size = imports.size();
		if (size == 0) return scope;
		if (size > 1) {
			imports = Lists.newArrayList(imports);
			Collections.reverse((List<URI>) imports);
		}
		final IResourceDescriptions descriptions = getResourceDescriptions(resource, imports);
		return SelectableBasedScope.createScope(scope, descriptions, filter, type, false);
	}
}
