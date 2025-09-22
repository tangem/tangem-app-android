package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.none
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
[REDACTED_AUTHOR]
 */
// TODO: Implement [REDACTED_JIRA]
@Suppress("UnusedPrivateProperty", "UnusedPrivateClass")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleAccountStatusListProducer @AssistedInject constructor(
    @Assisted private val params: SingleAccountStatusListProducer.Params,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
) : SingleAccountStatusListProducer {

    override val fallback: Option<AccountStatusList> = none()

    override fun produce(): Flow<AccountStatusList> = emptyFlow()

    private data class CryptoCurrencyStatusSources(
        val networkStatus: NetworkStatus,
        val yieldBalance: YieldBalance?,
        val quoteStatus: QuoteStatus?,
    )

    @AssistedFactory
    interface Factory : SingleAccountStatusListProducer.Factory {
        override fun create(params: SingleAccountStatusListProducer.Params): DefaultSingleAccountStatusListProducer
    }
}