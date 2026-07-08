package gaml.compiler.lite;

import gama.api.gaml.types.*;
import gama.api.types.object.GamaGenericObjectType;

/**
 * Initialises the minimal set of built-in GAML types for the lite compiler mode.
 *
 * <p>
 * In the full platform the type system is populated by {@code GamlAdditions} classes loaded via
 * {@code GamaBundleLoader.buildContributions()}.  That code path depends on the OSGi extension registry
 * and on heavy {@code gama.core} classes, neither of which are available in lite mode.
 * </p>
 *
 * <p>
 * This class creates singleton instances of every built-in type declared in {@code gama.api} and
 * registers them through {@link Types#addRegularType} + {@link Types#cache}, exactly as
 * {@link gama.api.additions.AbstractGamlAdditions#_type} does in the full system.
 * </p>
 *
 * <p>
 * Once all types are registered, {@link Types#init()} is called to build the type hierarchy.
 * </p>
 */
public class LiteTypeInitializer {

	private final ITypesManager manager;

	public LiteTypeInitializer() {
		manager = Types.getBuiltInTypeManager();
	}

	public void initialize() {
		registerType(new GamaIntegerType(manager));
		registerType(new GamaFloatType(manager));
		registerType(new GamaBoolType(manager));
		registerType(new GamaStringType(manager));
		registerType(new GamaPointType(manager));
		registerType(new GamaGeometryType(manager));
		registerType(new GamaTopologyType(manager));
		registerType(new GamaColorType(manager));
		registerType(new GamaDateType(manager));
		registerType(new GamaFontType(manager));
		registerType(new GamaMessageType(manager));
		registerType(new GamaActionType(manager));
		registerType(new GamaSkillType(manager));
		registerType(new GamaFieldType(manager));
		registerType(new GamaMetaType(manager));
		registerType(new GamaPathType(manager));
		registerType(new GamaDirectoryType(manager));

		registerType(new GamaListType(manager));
		registerType(new GamaMapType(manager));
		registerType(new GamaMatrixType(manager));
		registerType(new GamaPairType(manager));
		registerType(new GamaGraphType(manager));
		registerType(new GamaContainerType(manager));
		registerType(new GamaSpeciesType(manager));
		registerType(new GamaDataFrameType(manager));

		registerType(new GamaFileType(manager));

		registerType(new GamaGenericAgentType(manager));
		registerType(new GamaGenericObjectType(manager));
		registerType(new GamaNoType(manager));

		Types.init();
	}

	private void registerType(final IType<?> type) {
		Types.addRegularType(type.getName(), type, "gama.api");
		Types.cache(type);
	}

}
