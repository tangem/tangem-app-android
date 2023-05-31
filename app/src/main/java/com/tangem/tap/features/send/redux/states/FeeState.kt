package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.features.wallet.redux.ProgressState
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
enum class FeeType {
    SINGLE, LOW, NORMAL, PRIORITY;
}

fun FeeType.convertToAnalyticsFeeType(): AnalyticsParam.FeeType {
    return when (this) {
        FeeType.SINGLE -> AnalyticsParam.FeeType.Fixed
        FeeType.LOW -> AnalyticsParam.FeeType.Min
        FeeType.NORMAL -> AnalyticsParam.FeeType.Normal
        FeeType.PRIORITY -> AnalyticsParam.FeeType.Max
    }
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
    val progressState: ProgressState = ProgressState.Done,
) : SendScreenState {

    override val stateId: StateId = StateId.FEE

    fun isReady(): Boolean = currentFee != null

    fun getCurrentFeeValue(): BigDecimal = currentFee?.value ?: BigDecimal.ZERO
}