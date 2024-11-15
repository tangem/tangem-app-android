package com.tangem.features.onramp.main.entity.factory.amount

import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.utils.Provider

internal class OnrampAmountStateFactory(private val currentStateProvider: Provider<OnrampMainComponentUM>) {

    private val onrampAmountFieldChangeConverter = OnrampAmountFieldChangeConverter(
        currentStateProvider = currentStateProvider,
    )

    fun getOnAmountValueChange(value: String) = onrampAmountFieldChangeConverter.convert(value)

    fun getUpdatedCurrencyState(currency: OnrampCurrency): OnrampMainComponentUM {
        val currentState = currentStateProvider()
        if (currentState !is OnrampMainComponentUM.Content) return currentState

        val amountState = currentState.amountBlockState
        return currentState.copy(
            amountBlockState = amountState.copy(
                currencyUM = amountState.currencyUM.copy(
                    code = currency.code,
                    iconUrl = currency.image,
                    precision = currency.precision,
                ),
                amountFieldModel = amountState.amountFieldModel.copy(
                    fiatAmount = amountState.amountFieldModel.fiatAmount.copy(
                        currencySymbol = currency.code,
                        decimals = currency.precision,
                        type = AmountType.FiatType(currency.code),
                    ),
                ),
            ),
        )
    }
}