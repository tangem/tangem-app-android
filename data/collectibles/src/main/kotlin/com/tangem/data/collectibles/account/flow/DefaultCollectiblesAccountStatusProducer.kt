package com.tangem.data.collectibles.account.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.isCollectibles
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusProducer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

/**
 * Produces the collectibles account status by pairing the cached status with the rate of what the vault holds.
 *
 * The rate is not cached with the amount: it is a rate into the app's selected fiat currency, which the user can
 * change while a cached amount stays valid. So the store keeps the amount, and the rate is mixed in here, on
 * every emission.
 *
 * The rate flow starts with [Rate.Pending] rather than waiting for a quote, so the first emission is immediate —
 * the store already emits on subscription for the same reason.
 *
 * [CollectiblesAccountStatusValue.MissedDerivation] is never stored. Like a token's, it is worked out on every
 * emission: the account exists — recorded here or listed by the backend — and this device holds no owner key.
 */
@Suppress("LongParameterList")
internal class DefaultCollectiblesAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: CollectiblesAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val collectiblesAccountStore: CollectiblesAccountStore,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val collectiblesOwnerDeriver: CollectiblesOwnerDeriver,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesAccountStatusProducer {

    override val fallback: Option<CollectiblesAccountStatusValue>
        get() = CollectiblesAccountStatusValue.Error.Unavailable.some()

    override fun produce(): Flow<CollectiblesAccountStatusValue> {
        return combine(
            flow = collectiblesAccountStore.get(userWalletId = params.userWalletId),
            flow2 = isListedOnBackend(),
            flow3 = isOwnerKeyMissing(),
            flow4 = rate(CollectiblesVaultCurrencies.USDC_RAW_ID),
        ) { stored, isListed, isOwnerKeyMissing, usdcRate ->
            when {
                stored is CollectiblesAccountStatusValue.NotOnboarded -> CollectiblesAccountStatusValue.NotOnboarded
                !isListed && !stored.isAccountCreated -> CollectiblesAccountStatusValue.NotOnboarded
                isOwnerKeyMissing -> CollectiblesAccountStatusValue.MissedDerivation
                else -> priced(stored = stored, usdcRate = usdcRate)
            }
        }
            .flowOn(dispatchers.default)
    }

    /** The saved document is read first: the flow of it stays silent while there is none */
    private fun isListedOnBackend(): Flow<Boolean> {
        return flow {
            emit(walletAccountsFetcher.getSaved(userWalletId = params.userWalletId).hasCollectiblesRecord())
            emitAll(walletAccountsFetcher.get(userWalletId = params.userWalletId).map { it.hasCollectiblesRecord() })
        }
            .distinctUntilChanged()
    }

    private fun GetWalletAccountsResponse?.hasCollectiblesRecord(): Boolean {
        return this?.accounts.orEmpty().any(WalletAccountDTO::isCollectibles)
    }

    /** A locked wallet is not asked: its keys are out of reach, which says nothing about the derivation */
    private fun isOwnerKeyMissing(): Flow<Boolean> {
        return userWalletsListRepository.userWallets
            .map { wallets ->
                val userWallet = wallets?.find { it.walletId == params.userWalletId }
                userWallet != null && !userWallet.isLocked &&
                    collectiblesOwnerDeriver.storedOwner(params.userWalletId) == null
            }
            .distinctUntilChanged()
    }

    private fun rate(rawCurrencyId: CryptoCurrency.RawID): Flow<Rate> {
        return singleQuoteStatusSupplier(SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId))
            .map { quote ->
                when (val value = quote.value) {
                    is QuoteStatus.Data -> Rate.Known(value.fiatRate)
                    // Empty is what the quote producer emits for a currency nobody has fetched yet, so it is
                    // "no rate so far", never "no rate exists"
                    is QuoteStatus.Empty -> Rate.Pending
                }
            }
            .onStart { emit(Rate.Pending) }
            .distinctUntilChanged()
    }

    /**
     * A record with nothing read yet is loading, and one whose refresh failed with nothing cached is
     * unavailable — neither may claim the vault is empty. An account listed by the backend but not recorded here
     * yet is loading as well, until the fetcher reads its vault. A balance no quote can price yet is loading too:
     * an unpriced balance blanks the whole wallet total, not just this account.
     */
    private fun priced(stored: CollectiblesAccountStatusValue?, usdcRate: Rate): CollectiblesAccountStatusValue {
        val value = stored ?: return CollectiblesAccountStatusValue.Loading

        val balance = value.balance ?: return when (value.source) {
            StatusSource.ONLY_CACHE -> CollectiblesAccountStatusValue.Error.Unavailable
            else -> CollectiblesAccountStatusValue.Loading
        }

        val priced = balance.copy(usdcFiatRate = (usdcRate as? Rate.Known)?.value)

        if (priced.fiatAmount == null) return CollectiblesAccountStatusValue.Loading

        return value.copySealed(vaultBalance = priced)
    }

    private sealed interface Rate {

        data object Pending : Rate

        data class Known(val value: BigDecimal) : Rate
    }

    @AssistedFactory
    interface Factory : CollectiblesAccountStatusProducer.Factory {
        override fun create(params: CollectiblesAccountStatusProducer.Params): DefaultCollectiblesAccountStatusProducer
    }
}