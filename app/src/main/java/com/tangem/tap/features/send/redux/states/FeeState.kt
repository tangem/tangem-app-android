package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.tap.features.send.redux.FeeAction
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 11/09/2020.
 */
enum class FeeType {
    SINGLE, LOW, NORMAL, PRIORITY
}

enum class FeePrecision(val symbol: String) {
    PRECISE(""), CAN_BE_LOWER("<")
}

data class FeeState(
        val selectedFeeType: FeeType = FeeType.NORMAL,
        val feeList: List<Amount>? = null,
        val currentFee: Amount? = null,
        val feeIsIncluded: Boolean = false,
        val mainLayoutIsVisible: Boolean = false,
        val controlsLayoutIsVisible: Boolean = false,
        val feeChipGroupIsVisible: Boolean = true,
        val includeFeeSwitcherIsEnabled: Boolean = true,
        val error: FeeAction.Error? = null,
        val feePrecision: FeePrecision = FeePrecision.PRECISE
) : SendScreenState {

    override val stateId: StateId = StateId.FEE

    fun isReady(): Boolean = error == null && currentFee != null

    fun getCurrentFeeValue(): BigDecimal = currentFee?.value ?: BigDecimal.ZERO
}