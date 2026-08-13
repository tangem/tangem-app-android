package com.tangem.data.polymarket.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.polymarket.flow.PredictionAccountStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

/**
 * Produces the prediction account status by pairing the cached status with the collateral's current rate.
 *
 * The rate is not cached with the status: it is a rate into the app's selected fiat currency, which the user can
 * change while the cached balance stays valid. So the store keeps the balance and the structure, and the rate is
 * mixed in here, on every emission.
 *
 * The first emission must not wait for either source. The store emits on subscription, and the rate flow starts
 * with [CollateralRate.Pending] instead of waiting for the quote — otherwise this producer would be silent until
 * the quote arrived, and this status is combined with the other accounts', where one silent source stalls them all.
 */
internal class DefaultPredictionAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: PredictionAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val statusStore: PredictionAccountStatusStore,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionAccountStatusProducer {

    override val fallback: Option<PredictionAccountStatusValue>
        get() = PredictionAccountStatusValue.Error.Unavailable.some()

    override fun produce(): Flow<PredictionAccountStatusValue> {
        return combine(
            statusStore.get(userWalletId = params.userWalletId),
            collateralRate(),
        ) { stored, rate ->
            priced(stored = stored, rate = rate)
        }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private fun collateralRate(): Flow<CollateralRate> {
        return singleQuoteStatusSupplier(SingleQuoteStatusProducer.Params(rawCurrencyId = COLLATERAL_CURRENCY_ID))
            .map { quote ->
                when (val value = quote.value) {
                    is QuoteStatus.Data -> CollateralRate.Known(value.fiatRate)
                    is QuoteStatus.Empty -> CollateralRate.Missing
                }
            }
            .onStart { emit(CollateralRate.Pending) }
            .distinctUntilChanged()
    }

    /**
     * A balance nobody can price is reported without a rate, which resolves to a failed contribution — the same
     * treatment a token without a quote gets in the wallet total. While the quote has merely not arrived yet, the
     * status stays [PredictionAccountStatusValue.Loading] instead, so a momentary gap cannot blank the total.
     */
    private fun priced(stored: PredictionAccountStatusValue?, rate: CollateralRate): PredictionAccountStatusValue {
        val value = stored ?: PredictionAccountStatusValue.Loading
        if (value !is PredictionAccountStatusValue.Active) return value

        return when (rate) {
            CollateralRate.Pending -> PredictionAccountStatusValue.Loading
            CollateralRate.Missing -> value.copy(fiatRate = null)
            is CollateralRate.Known -> value.copy(fiatRate = rate.value)
        }
    }

    private sealed interface CollateralRate {

        /** The quote has not been asked for yet, or has not answered. */
        data object Pending : CollateralRate

        /** The quote answered and carries no rate. */
        data object Missing : CollateralRate

        data class Known(val value: BigDecimal) : CollateralRate
    }

    @AssistedFactory
    interface Factory : PredictionAccountStatusProducer.Factory {
        override fun create(params: PredictionAccountStatusProducer.Params): DefaultPredictionAccountStatusProducer
    }
}