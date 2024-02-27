package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendStates.FeeState> {

    override fun convert(value: Unit): SendStates.FeeState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        return SendStates.FeeState(
            feeSelectorState = FeeSelectorState.Loading,
            fee = null,
            notifications = persistentListOf(),
            rate = cryptoCurrencyStatus.value.fiatRate,
            appCurrency = appCurrencyProvider(),
            isFeeApproximate = false,
        )
    }
}