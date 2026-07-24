package com.tangem.features.polymarket.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

/**
 * Host model for the Polymarket feature navigation stack.
 *
 * Kept intentionally minimal — per-screen state is owned by the child components/models introduced in later tasks.
 */
@ModelScoped
internal class PolymarketModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketComponent.Params>()

    /** Wallet context the feature was opened for; consumed by child screens (events feed, place prediction). */
    val userWalletId: UserWalletId = params.userWalletId

    /** Initial route of the feature stack. */
    val initialRoute: PolymarketRoute = PolymarketRoute.Main
}