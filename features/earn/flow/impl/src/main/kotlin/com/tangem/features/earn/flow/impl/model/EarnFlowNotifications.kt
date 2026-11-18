package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.domain.transaction.models.isGasless
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/** Fee cards of the flow screens, built by the same factory as in Send and Staking. */
internal object EarnFlowNotifications {

    fun amount(state: EarnFlowState, isFromPosition: Boolean, onFeeReload: () -> Unit): ImmutableList<NotificationUM> =
        buildList {
            addFeeError(state, onFeeReload)
            addExceedBalance(state, isFromPosition)
        }.toImmutableList()

    fun confirm(state: EarnFlowState, isFromPosition: Boolean, onFeeReload: () -> Unit): ImmutableList<NotificationUM> =
        buildList {
            addFeeError(state, onFeeReload)
            addExceedBalance(state, isFromPosition)
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

    /**
     * A wallet deposit paying the fee from its own balance has a Max of zero once that balance does not cover the fee,
     * so the card names the fee rather than the total.
     */
    private fun MutableList<NotificationUM>.addExceedBalance(state: EarnFlowState, isFromPosition: Boolean) {
        when {
            isFromPosition -> addFeeExceedsBalance(state, isNothingLeftEnough = true)
            !addFeeExceedsBalance(state, isNothingLeftEnough = !state.isAmountSubtractAvailable) ->
                addTotalExceedsBalance(state)
        }
    }

    /**
     * The amount of a withdrawal leaves the position and arrives with the transaction, so only the fee is paid from the
     * wallet and may take all of it. A deposit sharing the balance with the fee needs something left after it.
     */
    private fun MutableList<NotificationUM>.addFeeExceedsBalance(
        state: EarnFlowState,
        isNothingLeftEnough: Boolean,
    ): Boolean {
        val selection = state.feeSelection ?: return false
        val feeAmount = selection.fee.amount.value ?: return false
        val tokenFeeStatus = (state.fee as? FeeSelectorUM.Content)
            ?.feeExtraInfo
            ?.takeIf { selection.extended?.isGasless == true }
            ?.feeCryptoCurrencyStatus
        val feeStatus = tokenFeeStatus ?: state.feeStatus ?: return false
        val balance = feeStatus.value.amount ?: return false
        val isCovered = if (isNothingLeftEnough) feeAmount <= balance else feeAmount < balance
        if (isCovered) return false
        with(feeStatus.currency) {
            add(
                NotificationUM.Error.ExceedsBalance(
                    networkIconId = networkIconResId,
                    currencyName = name,
                    feeName = name,
                    feeSymbol = symbol,
                    networkName = name,
                ),
            )
        }
        return true
    }

    private fun MutableList<NotificationUM>.addTotalExceedsBalance(state: EarnFlowState) {
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