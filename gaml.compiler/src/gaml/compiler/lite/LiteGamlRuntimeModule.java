/*******************************************************************************************************
 *
 * LiteGamlRuntimeModule.java, in gaml.compiler, is part of the source code of the GAMA modeling and simulation
 * platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.compiler.lite;

import org.eclipse.xtext.service.SingletonBinding;

import gaml.compiler.GamlRuntimeModule;

/**
 * A variant of the {@link GamlRuntimeModule} that binds the global scope provider
 * to {@link LiteBuiltinGlobalScopeProvider} instead of the full {@code BuiltinGlobalScopeProvider}.
 *
 * <p>
 * This should be used for headless execution, language servers, or CI environments where
 * the heavy Java execution classes from {@code gama.core} shouldn't be loaded.
 * </p>
 */
public class LiteGamlRuntimeModule extends GamlRuntimeModule {

	@Override
	@SingletonBinding()
	public Class<? extends org.eclipse.xtext.scoping.IGlobalScopeProvider> bindIGlobalScopeProvider() {
		return LiteBuiltinGlobalScopeProvider.class;
	}

}
