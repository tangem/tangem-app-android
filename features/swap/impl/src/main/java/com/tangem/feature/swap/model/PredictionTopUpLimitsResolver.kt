package com.tangem.feature.swap.model

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.usecase.GetPredictionTopUpMinimumUseCase
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.feature.swap.models.PredictionTopUpLimits
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

internal class PredictionTopUpLimitsResolver @Inject constructor(
    private val getPredictionTopUpMinimumUseCase: GetPredictionTopUpMinimumUseCase,
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase,
) {

    /** @return limits in units of [receiveCurrency], or `null` when the minimum or a positive USD quote is unknown */
    suspend fun resolve(userWalletId: UserWalletId, receiveCurrency: CryptoCurrency): PredictionTopUpLimits? {
        val minimumUsd = getPredictionTopUpMinimumUseCase(userWalletId) ?: return null
        val usdRate = receiveCurrency.id.rawCurrencyId
            ?.let { getCurrencyUSDQuoteUseCase(it) }
            ?.takeIf { it.signum() > 0 }
            ?: return null
        fun BigDecimal.inReceiveToken() = divide(usdRate, receiveCurrency.decimals, RoundingMode.UP)
        fun BigDecimal.formatUsd() = format {
            fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol)
        }
        val maximumUsd = PredictionTopUpLimits.MAXIMUM_USD
        return PredictionTopUpLimits(
            minimum = minimumUsd.inReceiveToken(),
            minimumText = minimumUsd.formatUsd(),
            maximum = maximumUsd.inReceiveToken(),
            maximumText = maximumUsd.formatUsd(),
        )
    }
}