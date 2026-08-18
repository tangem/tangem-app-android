package com.tangem.features.commonfeatures.impl.portfolioselector.fetcher

import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher.Mode
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope

internal class DefaultPortfolioSelectorBridge @AssistedInject constructor(
    portfolioFetcherFactory: DefaultPortfolioFetcher.Factory,
    @Assisted mode: Mode,
    @Assisted scope: CoroutineScope,
) : PortfolioSelectorBridge, PortfolioFetcher by portfolioFetcherFactory.create(mode, scope) {

    @AssistedFactory
    interface Factory : PortfolioSelectorBridge.Factory {
        override fun create(mode: Mode, scope: CoroutineScope): DefaultPortfolioSelectorBridge
    }
}