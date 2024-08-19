package com.tangem.features.markets.portfolio.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ComponentScoped
internal class MarketsPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<MarketsPortfolioComponent.Params>()

    @Suppress("UnusedPrivateMember")
    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        // TODO [REDACTED_TASK_KEY]
    }

    fun setNoNetworksAvailable() {
        // TODO [REDACTED_TASK_KEY]
    }
}