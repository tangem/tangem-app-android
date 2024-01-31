package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal class SendFeeStateConverter : Converter<Unit, SendStates.FeeState> {

    override fun convert(value: Unit): SendStates.FeeState {
        return SendStates.FeeState(
            feeSelectorState = FeeSelectorState.Loading,
            isSubtractAvailable = false,
            isSubtract = false,
            isUserSubtracted = false,
            fee = null,
            receivedAmountValue = BigDecimal.ZERO,
            receivedAmount = "",
            notifications = persistentListOf(),
        )
    }
}