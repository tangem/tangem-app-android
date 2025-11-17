package com.tangem.domain.account.status.utils

import arrow.core.some
import arrow.core.toOption
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.operations.CryptoCurrencyStatusFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Factory that creates a flow of [CryptoCurrencyStatus] for a given [CryptoCurrency] in a [UserWallet].
 *
 * @property singleNetworkStatusSupplier Supplier for obtaining network status.
 * @property singleQuoteStatusSupplier Supplier for obtaining quote status.
 * @property singleYieldBalanceSupplier Supplier for obtaining yield balance.
 * @property stakingIdFactory Factory for creating staking IDs.
 *
[REDACTED_AUTHOR]
 */
internal class CryptoCurrencyStatusesFlowFactory @Inject constructor(
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
) {

    /**
     * Creates a flow of [CryptoCurrencyStatus] for the specified [userWallet] and [currency].
     *
     * @param userWallet The user wallet containing the currency.

     */
    fun create(userWallet: UserWallet, currency: CryptoCurrency): Flow<CryptoCurrencyStatus> {
        return getCryptoCurrencyStatusSourcesFlow(userWallet = userWallet, currency = currency)
            .map { statusSources ->
                CryptoCurrencyStatusFactory.create(
                    currency = currency,
                    maybeNetworkStatus = statusSources.networkStatus.some(),
                    maybeQuoteStatus = statusSources.quoteStatus.toOption(),
                    maybeYieldBalance = statusSources.yieldBalance.toOption(),
                )
            }
            .onEmpty {
                emit(
                    CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
                )
            }
            .distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getCryptoCurrencyStatusSourcesFlow(
        userWallet: UserWallet,
        currency: CryptoCurrency,
    ): Flow<CryptoCurrencyStatusSources> {
        val networkStatusFlow = getNetworkStatusFlow(userWalletId = userWallet.walletId, network = currency.network)

        val yieldBalanceFlow = if (userWallet.isMultiCurrency) {
            networkStatusFlow.flatMapLatest { networkStatus ->
                getYieldBalanceFlow(
                    userWalletId = userWallet.walletId,
                    currencyId = currency.id,
                    networkStatus = networkStatus,
                )
            }
        } else {
            null
        }

        val quoteStatusFlow = currency.id.rawCurrencyId?.let(::getQuoteStatusFlow)

        return combine(networkStatusFlow, yieldBalanceFlow, quoteStatusFlow)
            .distinctUntilChanged()
    }

    private fun combine(
        networkStatusFlow: Flow<NetworkStatus>,
        yieldBalanceFlow: Flow<YieldBalance?>?,
        quoteStatusFlow: Flow<QuoteStatus>?,
    ): Flow<CryptoCurrencyStatusSources> {
        return when {
            yieldBalanceFlow != null && quoteStatusFlow != null -> {
                combine(
                    flow = networkStatusFlow,
                    flow2 = yieldBalanceFlow,
                    flow3 = quoteStatusFlow,
                    transform = ::CryptoCurrencyStatusSources,
                )
            }
            yieldBalanceFlow != null -> {
                combine(flow = networkStatusFlow, flow2 = yieldBalanceFlow, transform = ::CryptoCurrencyStatusSources)
            }
            quoteStatusFlow != null -> {
                combine(flow = networkStatusFlow, flow2 = quoteStatusFlow) { networkStatus, quoteStatus ->
                    CryptoCurrencyStatusSources(networkStatus = networkStatus, quoteStatus = quoteStatus)
                }
            }
            else -> networkStatusFlow.map(::CryptoCurrencyStatusSources)
        }
    }

    private fun getNetworkStatusFlow(userWalletId: UserWalletId, network: Network): Flow<NetworkStatus> {
        return singleNetworkStatusSupplier(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = network),
        )
            .distinctUntilChanged()
    }

    private fun getQuoteStatusFlow(rawCurrencyId: CryptoCurrency.RawID): Flow<QuoteStatus> {
        return singleQuoteStatusSupplier(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
        )
            .distinctUntilChanged()
    }

    private fun getYieldBalanceFlow(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        networkStatus: NetworkStatus,
    ): Flow<YieldBalance?> {
        val stakingId = stakingIdFactory.create(
            currencyId = currencyId,
            defaultAddress = networkStatus.getAddress(),
        )
            .getOrNull()

        return if (stakingId != null) {
            singleYieldBalanceSupplier(
                params = SingleYieldBalanceProducer.Params(userWalletId = userWalletId, stakingId = stakingId),
            )
                .distinctUntilChanged()
        } else {
            flowOf(null)
        }
    }

    private data class CryptoCurrencyStatusSources(
        val networkStatus: NetworkStatus,
        val yieldBalance: YieldBalance? = null,
        val quoteStatus: QuoteStatus? = null,
    )
}