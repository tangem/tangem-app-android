package com.tangem.data.collectibles.account.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusProducer
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
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
 */
internal class DefaultCollectiblesAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: CollectiblesAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val collectiblesAccountStore: CollectiblesAccountStore,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesAccountStatusProducer {

    override val fallback: Option<CollectiblesAccountStatusValue>
        get() = CollectiblesAccountStatusValue.Error.Unavailable.some()

    override fun produce(): Flow<CollectiblesAccountStatusValue> {
        return combine(
            flow = collectiblesAccountStore.get(userWalletId = params.userWalletId),
            flow2 = rate(CollectiblesVaultCurrencies.USDC_RAW_ID),
        ) { stored, usdcRate ->
            priced(stored = stored, usdcRate = usdcRate)
        }
            .flowOn(dispatchers.default)
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
     * unavailable — neither may claim the vault is empty. A balance no quote can price yet is loading too: an
     * unpriced balance blanks the whole wallet total, not just this account.
     */
    private fun priced(stored: CollectiblesAccountStatusValue?, usdcRate: Rate): CollectiblesAccountStatusValue {
        val value = stored ?: return CollectiblesAccountStatusValue.NotOnboarded
        if (value is CollectiblesAccountStatusValue.MissedDerivation) return value

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