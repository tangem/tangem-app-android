package com.tangem.features.polymarket.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

/**
 * Host model for the Polymarket feature navigation stack.
 *
 * Kept intentionally minimal — per-screen state is owned by the child components/models introduced in later tasks.
 * The feature params are not mirrored here: children that need them receive them from
 * [com.tangem.features.polymarket.impl.DefaultPolymarketComponent], which owns them, so there is no second copy
 * to keep in sync.
 */
@ModelScoped
internal class PolymarketModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    /** Initial route of the feature stack — the gate decides where the user actually lands. */
    val initialRoute: PolymarketRoute = PolymarketRoute.Onboarding
}