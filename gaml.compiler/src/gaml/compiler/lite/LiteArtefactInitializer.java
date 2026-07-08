package gaml.compiler.lite;

import gama.annotations.constants.IKeyword;
import gama.annotations.support.ISymbolKind;
import gama.api.additions.registries.ArtefactRegistry;
import gama.api.compilation.artefacts.IArtefact;
import gama.api.compilation.descriptions.IDescription;
import gama.api.gaml.types.IType;
import gaml.compiler.prototypes.FacetArtefact;
import gaml.compiler.prototypes.SymbolArtefact;

/**
 * Registers minimal symbol artefacts for the lite compiler mode.
 *
 * In the full platform artefacts are registered by generated GamlAdditions
 * classes loaded via the OSGi extension registry. That code path depends on
 * gama.core classes that are not available in lite mode.
 */
public class LiteArtefactInitializer {

	private static final IArtefact.Facet NAME_FACET = new FacetArtefact(IKeyword.NAME,
			new int[] { IType.LABEL }, IType.NONE, IType.NONE, new String[0], true, false, false);

	private static final IArtefact.Facet TYPE_FACET = new FacetArtefact(IKeyword.TYPE,
			new int[] { IType.LABEL }, IType.NONE, IType.NONE, new String[0], true, false, false);

	private static final IArtefact.Facet INIT_FACET = new FacetArtefact(IKeyword.INIT,
			new int[] { IType.NONE }, IType.NONE, IType.NONE, new String[0], true, false, false);

	private static final IArtefact.Facet OF_FACET = new FacetArtefact(IKeyword.OF,
			new int[] { IType.STRING }, IType.NONE, IType.NONE, new String[0], true, false, false);

	private static final IArtefact.Facet TITLE_FACET = new FacetArtefact(IKeyword.TITLE,
			new int[] { IType.LABEL }, IType.NONE, IType.NONE, new String[0], true, false, false);

	private static final IArtefact.Facet NUMBER_FACET = new FacetArtefact(IKeyword.NUMBER,
			new int[] { IType.INT }, IType.NONE, IType.NONE, new String[0], true, false, false);

	public void initialize() {
		IArtefact.Facet[] facets = new IArtefact.Facet[] { NAME_FACET, TYPE_FACET };

		registerStatement("model", ISymbolKind.MODEL, facets, IKeyword.NAME, new int[0]);
		registerStatement("global", ISymbolKind.SPECIES, facets, null, new int[0]);
		registerStatement("init", ISymbolKind.SINGLE_STATEMENT, facets, null,
				new int[] { ISymbolKind.SPECIES.code(), ISymbolKind.MODEL.code(), ISymbolKind.EXPERIMENT.code() });
		registerStatement("write", ISymbolKind.SINGLE_STATEMENT, facets, null,
				new int[] { ISymbolKind.SPECIES.code(), ISymbolKind.MODEL.code(), ISymbolKind.EXPERIMENT.code() });
		IArtefact.Facet[] createFacets = new IArtefact.Facet[] { NAME_FACET, TYPE_FACET, NUMBER_FACET };
		registerStatement("create", ISymbolKind.SINGLE_STATEMENT, createFacets, null,
				new int[] { ISymbolKind.SPECIES.code(), ISymbolKind.MODEL.code(), ISymbolKind.EXPERIMENT.code() });
		registerStatement("species", ISymbolKind.SPECIES, facets, IKeyword.NAME,
				new int[] { ISymbolKind.MODEL.code(), ISymbolKind.SPECIES.code(), ISymbolKind.EXPERIMENT.code(),
					ISymbolKind.OUTPUT.code() });
		registerStatement("aspect", ISymbolKind.BEHAVIOR, facets, IKeyword.NAME,
				new int[] { ISymbolKind.SPECIES.code() });
		registerStatement("draw", ISymbolKind.LAYER, facets, null,
				new int[] { ISymbolKind.BEHAVIOR.code(), ISymbolKind.SPECIES.code() });
		IArtefact.Facet[] expFacets = new IArtefact.Facet[] { NAME_FACET, TYPE_FACET, TITLE_FACET };
		registerStatement("experiment", ISymbolKind.EXPERIMENT, expFacets, IKeyword.NAME,
				new int[] { ISymbolKind.MODEL.code() });
		registerStatement("output", ISymbolKind.OUTPUT, facets, null,
				new int[] { ISymbolKind.EXPERIMENT.code() });
		registerStatement("display", ISymbolKind.OUTPUT, facets, IKeyword.NAME,
				new int[] { ISymbolKind.OUTPUT.code() });
		registerStatement("action", ISymbolKind.ACTION, facets, IKeyword.NAME, new int[0]);

		int[] varParentKinds = new int[] {
				ISymbolKind.SPECIES.code(), ISymbolKind.MODEL.code(),
				ISymbolKind.EXPERIMENT.code(), ISymbolKind.CLASS.code()
		};
		registerVariable(ISymbolKind.NUMBER, varParentKinds);
		registerVariable(ISymbolKind.REGULAR, varParentKinds);
	}

	private void registerStatement(final String keyword, final ISymbolKind kind,
			final IArtefact.Facet[] facets, final String omissible, final int[] parentKinds) {
		SymbolArtefact artefact = new SymbolArtefact(
				IDescription.class, false, false, false, false, kind, true,
				facets, omissible, new String[0], parentKinds,
				false, false, false, null, keyword, "gaml.compiler");
		ArtefactRegistry.addArtefact(artefact, java.util.Collections.singletonList(keyword));
	}

	private void registerVariable(final ISymbolKind kind, final int[] parentKinds) {
		IArtefact.Facet[] facets = new IArtefact.Facet[] { NAME_FACET, TYPE_FACET, INIT_FACET, OF_FACET };
		SymbolArtefact artefact = new SymbolArtefact(
				IDescription.class, false, false, false, false, kind, true,
				facets, IKeyword.NAME, new String[0], parentKinds,
				false, false, false, null, kind.name(), "gaml.compiler");
		ArtefactRegistry.addArtefact(artefact, java.util.Collections.singletonList(kind.name()));
	}
}
