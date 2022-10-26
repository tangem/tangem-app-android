package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
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
    val currentFee: Amount? = null,
    val feeIsIncluded: Boolean = false,
    val feeIsApproximate: Boolean = false,
    val mainLayoutIsVisible: Boolean = false,
    val controlsLayoutIsVisible: Boolean = false,
    val feeChipGroupIsVisible: Boolean = true,
    val includeFeeSwitcherIsEnabled: Boolean = true,
) : SendScreenState {

    override val stateId: StateId = StateId.FEE

    fun isReady(): Boolean = currentFee != null

    fun getCurrentFeeValue(): BigDecimal = currentFee?.value ?: BigDecimal.ZERO
}