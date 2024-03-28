package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendStates.FeeState> {

    override fun convert(value: Unit): SendStates.FeeState {
        return SendStates.FeeState(
            feeSelectorState = FeeSelectorState.Error,
            fee = null,
            notifications = persistentListOf(),
            rate = feeCryptoCurrencyStatusProvider().value.fiatRate,
            appCurrency = appCurrencyProvider(),
            isFeeApproximate = false,
        )
    }
}
