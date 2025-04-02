package com.tangem.features.send.v2.subcomponents.fee.model

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class SendFeeAlertFactory @Inject constructor(
    private val messageSender: UiMessageSender,
) {

    fun checkAndShowAlerts(feeSelectorUM: FeeSelectorUM.Content, onConfirmClick: () -> Unit) {
        val showFeeTooLow = checkAndShowFeeTooLow(feeSelectorUM, onConfirmClick)
        val showFeeTooHigh = checkAndShowFeeTooHigh(feeSelectorUM, onConfirmClick)

        if (!showFeeTooLow && !showFeeTooHigh) {
            onConfirmClick()
        }
    }

    /**
     * Check if custom fee is too low
     */
    private fun checkAndShowFeeTooLow(feeSelectorUM: FeeSelectorUM.Content, onConfirmClick: () -> Unit): Boolean {
        val isFeeTooLow = checkIfFeeTooLow(feeSelectorUM)
        if (isFeeTooLow) {
            messageSender.send(
                DialogMessage(
                    message = resourceReference(id = R.string.send_alert_fee_too_low_text),
                    dismissOnFirstAction = true,
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_continue),
                            onClick = onConfirmClick,
                        )
                    },
                    secondActionBuilder = { cancelAction() },
                ),
            )
        }
        return isFeeTooLow
    }

    /**
     * Check if custom fee is too high
     */
    private fun checkAndShowFeeTooHigh(feeSelectorUM: FeeSelectorUM.Content, onConfirmClick: () -> Unit): Boolean {
        val (isFeeTooHigh, diff) = checkIfFeeTooHigh(feeSelectorUM)
        if (isFeeTooHigh) {
            messageSender.send(
                DialogMessage(
                    message = resourceReference(
                        id = R.string.send_alert_fee_too_high_text,
                        formatArgs = wrappedList(diff),
                    ),
                    dismissOnFirstAction = true,
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_continue),
                            onClick = onConfirmClick,
                        )
                    },
                    secondActionBuilder = { cancelAction() },
                ),
            )
        }
        return isFeeTooHigh
    }

    fun getFeeUpdatedAlert(newFee: TransactionFee, feeUM: FeeUM, proceedAction: () -> Unit, stopAction: () -> Unit) {
        if (feeUM !is FeeUM.Content) return
        val feeSelectorUM = feeUM.feeSelectorUM as? FeeSelectorUM.Content ?: return
        val newFee = when (newFee) {
            is TransactionFee.Single -> newFee.normal
            is TransactionFee.Choosable -> {
                when (feeSelectorUM.selectedType) {
                    FeeType.Slow -> newFee.minimum
                    FeeType.Market -> newFee.normal
                    FeeType.Fast -> newFee.priority
                    FeeType.Custom -> return
                }
            }
        }

        val newFeeValue = newFee.amount.value ?: BigDecimal.ZERO
        val oldFeeValue = feeSelectorUM.selectedFee?.amount?.value ?: BigDecimal.ZERO

        if (newFeeValue > oldFeeValue) {
            messageSender.send(
                DialogMessage(
                    message = resourceReference(id = R.string.send_notification_high_fee_title),
                    dismissOnFirstAction = true,
                    firstActionBuilder = {
                        okAction { proceedAction(); onDismissRequest() }
                    },
                    secondActionBuilder = {
                        cancelAction { stopAction(); onDismissRequest() }
                    },
                ),
            )
        } else {
            proceedAction()
        }
    }

    fun getFeeUnreachableErrorState(onFeeReload: () -> Unit) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.send_fee_unreachable_error_title),
                message = resourceReference(R.string.send_fee_unreachable_error_text),
                dismissOnFirstAction = true,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.warning_button_refresh),
                        onClick = onFeeReload,
                    )
                },
            ),
        )
    }
}