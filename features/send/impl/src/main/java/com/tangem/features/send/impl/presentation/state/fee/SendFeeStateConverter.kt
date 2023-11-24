package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendFeeStateConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendStates.FeeState> {

    override fun convert(value: Unit): SendStates.FeeState {
        return SendStates.FeeState(
            cryptoCurrencyStatus = cryptoCurrencyStatusProvider(),
        )
    }
}