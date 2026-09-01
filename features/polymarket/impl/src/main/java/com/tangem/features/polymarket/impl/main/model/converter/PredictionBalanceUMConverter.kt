package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.polymarket.impl.main.ui.state.PredictionBalanceUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class PredictionBalanceUMConverter(
    private val appCurrency: AppCurrency,
    private val collateral: CryptoCurrency,
    private val onAddFundsClick: () -> Unit,
    private val onWithdrawClick: () -> Unit,
) : Converter<PredictionAccountStatusValue, PredictionBalanceUM> {

    override fun convert(value: PredictionAccountStatusValue): PredictionBalanceUM {
        val active = value as? PredictionAccountStatusValue.Active
        val isLoading = value is PredictionAccountStatusValue.Loading

        return PredictionBalanceUM(
            amount = value.toAmount(),
            addFunds = PredictionBalanceUM.ActionUM(
                isEnabled = active != null && active.isTradingAllowed,
                isLoading = isLoading,
                onClick = onAddFundsClick,
            ),
            withdraw = PredictionBalanceUM.ActionUM(
                isEnabled = active != null && active.balance > BigDecimal.ZERO,
                isLoading = isLoading,
                onClick = onWithdrawClick,
            ),
        )
    }

    private fun PredictionAccountStatusValue.toAmount(): PredictionBalanceUM.AmountUM = when {
        this is PredictionAccountStatusValue.Loading -> PredictionBalanceUM.AmountUM.Loading
        else -> {
            val fiat = fiatBalance
            val crypto = (this as? PredictionAccountStatusValue.Active)?.balance

            if (fiat == null || crypto == null) {
                PredictionBalanceUM.AmountUM.Unknown
            } else {
                PredictionBalanceUM.AmountUM.Known(
                    fiat = fiat.formatFiat(),
                    crypto = crypto.formatCrypto(),
                    isFlickering = source == StatusSource.CACHE,
                )
            }
        }
    }

    private fun BigDecimal.formatFiat(): TextReference = stringReference(
        value = format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
    )

    private fun BigDecimal.formatCrypto(): TextReference = stringReference(
        value = format { crypto(cryptoCurrency = collateral) },
    )
}