package com.tangem.features.onramp.main.entity.factory.amount

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.features.onramp.main.entity.OnrampAmountSecondaryFieldUM
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampProviderBlockUM
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

internal class OnrampAmountFieldChangeConverter(
    private val currentStateProvider: Provider<OnrampMainComponentUM>,
) : Converter<String, OnrampMainComponentUM> {

    override fun convert(value: String): OnrampMainComponentUM {
        val state = currentStateProvider()
        if (state !is OnrampMainComponentUM.Content) return state

        if (value.isEmpty()) return state.emptyState()

        val amountState = state.amountBlockState
        val amountTextField = amountState.amountFieldModel
        val fiatDecimal = value.parseBigDecimalOrNull() ?: BigDecimal.ZERO
        val isDoneActionEnabled = !fiatDecimal.isNullOrZero()
        val amountFieldModel = amountState.amountFieldModel.copy(
            fiatValue = value,
            fiatAmount = amountTextField.fiatAmount.copy(value = fiatDecimal),
            keyboardOptions = KeyboardOptions(
                imeAction = if (isDoneActionEnabled) ImeAction.Done else ImeAction.None,
                keyboardType = KeyboardType.Number,
            ),
        )

        return state.copy(
            amountBlockState = amountState.copy(
                amountFieldModel = amountFieldModel,
                secondaryFieldModel = OnrampAmountSecondaryFieldUM.Loading,
            ),
            providerBlockState = OnrampProviderBlockUM.Loading,
        )
    }

    private fun OnrampMainComponentUM.Content.emptyState(): OnrampMainComponentUM.Content {
        val amountFieldModel = amountBlockState.amountFieldModel.copy(
            value = "",
            fiatValue = "",
            cryptoAmount = amountBlockState.amountFieldModel.cryptoAmount.copy(value = BigDecimal.ZERO),
            fiatAmount = amountBlockState.amountFieldModel.fiatAmount.copy(value = BigDecimal.ZERO),
            isError = false,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.None,
                keyboardType = KeyboardType.Number,
            ),
        )
        return copy(
            amountBlockState = amountBlockState.copy(
                amountFieldModel = amountFieldModel,
                secondaryFieldModel = OnrampAmountSecondaryFieldUM.Content(TextReference.EMPTY),
            ),
            buyButtonConfig = buyButtonConfig.copy(enabled = false),
            providerBlockState = OnrampProviderBlockUM.Empty,
        )
    }
}