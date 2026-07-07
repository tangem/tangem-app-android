package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.QuickTopUpBlockUM
import com.tangem.features.tokendetails.TokenDetailsFeatureToggles
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class QuickTopUpBlockFactory @Inject constructor(
    private val featureToggles: TokenDetailsFeatureToggles,
) {

    fun build(
        currencyStatus: CryptoCurrencyStatus,
        isHistoryEmpty: Boolean,
        onrampAvailability: Either<OnrampError, OnrampAvailability>,
        onPresetClick: (BigDecimal, String) -> Unit,
        onOtherClick: () -> Unit,
    ): QuickTopUpBlockUM? {
        if (!featureToggles.isQuickTopUpEnabled) return null

        val amount = currencyStatus.value.amount
        if (amount == null || !amount.isZero()) return null

        if (!isHistoryEmpty) return null

        val currency = when (val availability = onrampAvailability.getOrNull()) {
            is OnrampAvailability.Available -> availability.currency
            is OnrampAvailability.ConfirmResidency -> {
                if (!availability.country.onrampAvailable) return null
                availability.country.defaultCurrency
            }
            else -> return null
        }

        val presets = when (currency.code) {
            USD_CODE -> USD_PRESETS
            EUR_CODE -> EUR_PRESETS
            else -> return null
        }

        val presetAmounts = presets.map { value ->
            QuickTopUpBlockUM.QuickTopUpAmountUM(
                displayValue = stringReference("${currency.unit}$value"),
                onClick = { onPresetClick(BigDecimal(value), currency.code) },
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