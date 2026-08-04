package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.QuickTopUpBlockUM
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class QuickTopUpBlockFactory @Inject constructor() {

    @Suppress("LongParameterList")
    fun build(
        currencyStatus: CryptoCurrencyStatus,
        isHistoryEmpty: Boolean,
        onrampAvailability: Either<OnrampError, OnrampAvailability>,
        selectedOnrampCurrency: OnrampCurrency?,
        appCurrency: AppCurrency,
        onPresetClick: (BigDecimal, String) -> Unit,
        onOtherClick: () -> Unit,
    ): QuickTopUpBlockUM? {
        val amount = currencyStatus.value.amount
        if (amount == null || !amount.isZero()) return null

        if (!isHistoryEmpty) return null

        val isOnrampAvailable = when (val availability = onrampAvailability.getOrNull()) {
            is OnrampAvailability.Available -> true
            is OnrampAvailability.ConfirmResidency -> availability.country.onrampAvailable
            else -> false
        }
        if (!isOnrampAvailable) return null

        val code = selectedOnrampCurrency?.code ?: appCurrency.code
        val symbol = selectedOnrampCurrency?.unit ?: appCurrency.symbol

        val presets = when (code) {
            USD_CODE -> USD_PRESETS
            EUR_CODE -> EUR_PRESETS
            else -> return null
        }

        val presetAmounts = presets.map { value ->
            QuickTopUpBlockUM.QuickTopUpAmountUM(
                displayValue = stringReference("$symbol$value"),
                onClick = { onPresetClick(BigDecimal(value), code) },
            )
        }
        val otherAmount = QuickTopUpBlockUM.QuickTopUpAmountUM(
            displayValue = resourceReference(R.string.quick_top_up_chip_other),
            onClick = onOtherClick,
            isOther = true,
        )

        return QuickTopUpBlockUM(
            amounts = (presetAmounts + otherAmount).toImmutableList(),
        )
    }

    private companion object {
        const val USD_CODE = "USD"
        const val EUR_CODE = "EUR"

        val USD_PRESETS = listOf(50, 200, 700)
        val EUR_PRESETS = listOf(50, 200, 650)
    }
}