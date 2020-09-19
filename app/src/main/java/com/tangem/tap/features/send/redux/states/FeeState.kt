package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.tap.features.send.redux.FeeAction
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
enum class FeeType {
    SINGLE, LOW, NORMAL, PRIORITY
}

data class FeeState(
        val selectedFeeType: FeeType = FeeType.NORMAL,
        val feeList: List<Amount>? = null,
        val currentFee: Value<Amount>? = null,
        val feeIsIncluded: Boolean = false,
        val mainLayoutIsVisible: Boolean = false,
        val controlsLayoutIsVisible: Boolean = false,
        val feeChipGroupIsVisible: Boolean = true,
        val includeFeeSwitcherIsEnabled: Boolean = true,
        val error: FeeAction.Error? = null,
) : SendScreenState {

    override val stateId: StateId = StateId.FEE

    fun isReady(): Boolean = error == null && currentFee != null

    fun getCurrentFee(): BigDecimal = currentFee?.value?.value ?: BigDecimal.ZERO
}