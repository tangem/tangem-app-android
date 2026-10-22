package com.tangem.feature.swap.mcp

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Builds the Swap route that carries out an [ExchangeIntent] with the currencies it was matched to: the pair, the
 * amount and the provider are filled in, and the swap is bound to the intent, so Express links the transaction it
 * creates to it.
 *
 * Swap takes an amount only for the currency it swaps from, so an intent that fixes the amount of the other side opens
 * Swap without one. The amount is in the units of the intent's own asset, not of the matched currency.
 */
internal class ExchangeIntentSwapRouteConverter @Inject constructor(
    private val amountConverter: ExchangeIntentAmountConverter,
) : Converter<ExchangeIntentSwapRouteConverter.Input, AppRoute.Swap> {

    data class Input(
        val intent: ExchangeIntent,
        val userWalletId: UserWalletId,
        val from: CryptoCurrency,
        val to: CryptoCurrency,
    )

    override fun convert(value: Input): AppRoute.Swap = AppRoute.Swap(
        userWalletId = value.userWalletId,
        fromCryptoCurrency = value.from,
        toCryptoCurrency = value.to,
        screenSource = AnalyticsParam.ScreensSources.Main.value,
        fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
        fromAmount = value.intent.prefilledFromAmount(),
        providerId = value.intent.providerId,
        intentId = value.intent.id,
    )

    /** Swap prefills only the amount of the currency it swaps from, so an intent that fixes the other side has none. */
    private fun ExchangeIntent.prefilledFromAmount(): BigDecimal? {
        if (amountType != SwapAmountType.From) return null

        return amountConverter.convert(this)
            ?.takeIf { it.signum() > 0 }
            ?.stripTrailingZeros()
    }
}