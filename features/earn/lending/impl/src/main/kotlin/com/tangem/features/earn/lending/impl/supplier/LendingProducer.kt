package com.tangem.features.earn.lending.impl.supplier

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.equalsEvmAddress
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.product.api.product.hasOutgoingPendingTransaction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface LendingProducer : FlowProducer<LendingSnapshot> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, LendingProducer>
}

/**
 * Reads the raw sources, not the wallet status list: the Lending balance contribution reads this snapshot and is a
 * part of that list, so reading it here would close a loop.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultLendingProducer @AssistedInject constructor(
    @Assisted private val params: LendingProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val accountListSupplier: SingleAccountListSupplier,
    private val networkStatusSupplier: SingleNetworkStatusSupplier,
    private val quoteStatusSupplier: SingleQuoteStatusSupplier,
    private val provider: LendingProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : LendingProducer {

    override val fallback: Option<LendingSnapshot> = LendingSnapshot.Failed.some()

    override fun produce(): Flow<LendingSnapshot> = accountListSupplier(params.userWalletId)
        .map { accounts -> accounts.resolveAssets() }
        .distinctUntilChanged()
        .flatMapLatest { resolved ->
            if (resolved.isEmpty()) return@flatMapLatest flowOf(LendingSnapshot.NoAssets)

            combine(resolved.map { assetState(it.asset, it.isWrappedTokenInWallet) }) { states ->
                LendingSnapshot(
                    assets = states.associateBy { it.asset.currency.id },
                    source = states.map(LendingAssetState::source).getResultStatusSource(),
                )
            }
        }
        .flowOn(dispatchers.default)

    private fun AccountList.resolveAssets(): List<ResolvedAsset> {
        val currencies = flattenCurrencies()
        return currencies.mapNotNull { currency ->
            val asset = provider.resolveAsset(currency) ?: return@mapNotNull null
            val wrapped = asset.wrappedToken
            ResolvedAsset(
                asset = asset,
                isWrappedTokenInWallet = wrapped != null && currencies.any { it.isSameToken(wrapped) },
            )
        }
    }

    private fun assetState(asset: LendingAsset, isWrappedTokenInWallet: Boolean): Flow<LendingAssetState> {
        val network = networkStatusSupplier(
            SingleNetworkStatusProducer.Params(userWalletId = params.userWalletId, network = asset.currency.network),
        )
        val quote: Flow<QuoteStatus?> = asset.currency.id.rawCurrencyId
            ?.let { rawId -> quoteStatusSupplier(SingleQuoteStatusProducer.Params(rawId)) }
            ?: flowOf(null)

        return combine(
            provider.market(params.userWalletId, asset),
            network,
            quote,
        ) { market, networkStatus, quoteStatus ->
            val verified = networkStatus.value as? NetworkStatus.Verified

            LendingAssetState(
                asset = asset,
                address = verified?.address?.defaultAddress?.value,
                balance = (verified?.amounts?.get(asset.currency.id) as? NetworkStatus.Amount.Loaded)?.value,
                fiatRate = (quoteStatus?.value as? QuoteStatus.Data)?.fiatRate,
                market = market,
                hasPendingTx = networkStatus.value.hasOutgoingPendingTransaction(asset.currency.id),
                isWrappedTokenInWallet = isWrappedTokenInWallet,
                source = listOf(verified?.source ?: StatusSource.CACHE, market.source).getResultStatusSource(),
            )
        }
    }

    private data class ResolvedAsset(val asset: LendingAsset, val isWrappedTokenInWallet: Boolean)

    @AssistedFactory
    interface Factory : LendingProducer.Factory {
        override fun create(params: LendingProducer.Params): DefaultLendingProducer
    }
}

private fun CryptoCurrency.isSameToken(token: CryptoCurrency.Token): Boolean =
    this is CryptoCurrency.Token && network == token.network && contractAddress.equalsEvmAddress(token.contractAddress)