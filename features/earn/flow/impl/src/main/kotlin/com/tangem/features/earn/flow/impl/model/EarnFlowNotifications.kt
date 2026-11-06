package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/** Fee cards of the flow screens, built by the same factory as in Send and Staking. */
internal object EarnFlowNotifications {

    fun amount(state: EarnFlowState, onFeeReload: () -> Unit): ImmutableList<NotificationUM> = buildList {
        addFeeError(state, onFeeReload)
        addExceedBalance(state)
    }.toImmutableList()

    fun confirm(state: EarnFlowState, onFeeReload: () -> Unit): ImmutableList<NotificationUM> = buildList {
        addFeeError(state, onFeeReload)
        addFeeCoverage(state)
    }.toImmutableList()

    private fun MutableList<NotificationUM>.addFeeError(state: EarnFlowState, onFeeReload: () -> Unit) {
        val error = state.fee as? FeeSelectorUM.Error ?: return
        addFeeUnreachableNotification(
            feeError = error.error,
            tokenName = state.status?.currency?.name.orEmpty(),
            onReload = onFeeReload,
        )
    }

    private fun MutableList<NotificationUM>.addExceedBalance(state: EarnFlowState) {
        val selection = state.feeSelection ?: return
        addExceedBalanceNotification(
            feeAmount = selection.fee.amount.value ?: BigDecimal.ZERO,
            sendingAmount = state.sendingAmount ?: return,
            isSubtractionAvailable = state.isAmountSubtractAvailable,
            cryptoCurrencyStatus = state.status ?: return,
        )
    }

    private fun MutableList<NotificationUM>.addFeeCoverage(state: EarnFlowState) {
        val entered = state.enteredAmount ?: return
        val sending = state.sendingAmount ?: return
        addFeeCoverageNotification(
            isFeeCoverage = sending < entered,
            enteredAmountValue = entered,
            sendingValue = sending,
            appCurrency = state.appCurrency ?: return,
            cryptoCurrencyStatus = state.status ?: return,
        )
    }
}