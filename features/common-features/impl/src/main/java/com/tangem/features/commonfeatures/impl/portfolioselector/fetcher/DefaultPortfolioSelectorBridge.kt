package com.tangem.features.commonfeatures.impl.portfolioselector.fetcher

import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher.Data
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher.Mode
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

internal class DefaultPortfolioSelectorBridge @AssistedInject constructor(
    portfolioFetcherFactory: DefaultPortfolioFetcher.Factory,
    @Assisted mode: Mode,
    @Assisted scope: CoroutineScope,
    @Assisted isJointAccountsIncluded: Boolean,
) : PortfolioSelectorBridge {

    private val fetcher: PortfolioFetcher = portfolioFetcherFactory.create(mode, scope)

    override val data: Flow<Data> = if (isJointAccountsIncluded) {
        fetcher.data
    } else {
        fetcher.data.map { it.withoutJointAccounts() }
    }

    override val mode: StateFlow<Mode> get() = fetcher.mode

    override fun updateMode(mode: Mode) {
        fetcher.updateMode(mode)
    }

    private fun Data.withoutJointAccounts(): Data = copy(
        balances = balances.mapValues { (_, balance) ->
            balance.copy(
                accountsBalance = balance.accountsBalance.copy(
                    accountStatuses = balance.accountsBalance.accountStatuses.filterNot { it is AccountStatus.Joint },
                ),
            )
        },
    )

    @AssistedFactory
    interface Factory : PortfolioSelectorBridge.Factory {
        override fun create(
            mode: Mode,
            scope: CoroutineScope,
            isJointAccountsIncluded: Boolean,
        ): DefaultPortfolioSelectorBridge
    }
}