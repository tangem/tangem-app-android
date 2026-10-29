package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount

internal class BankWithdrawAnalytics(
    private val sourceAccount: BankWithdrawSourceAccount,
    private val analytics: AnalyticsEventHandler,
) {

    fun onRecipientListOpened() = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactsScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientListOpened(),
    )

    fun onRecipientSelected() = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactOnContactsScreenClicked(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientSelected(),
    )

    fun onAmountScreenOpened() = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpAmountScreenOpened(),
    )

    fun onConfirmationScreenOpened() = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpConfirmationScreenOpened(),
    )

    fun onSendClicked(draft: BankWithdrawDraft) {
        send(
            paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked(),
            moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientTransferConfirmed(),
        )
        send(paymentAccountEvent = null, moneyAccountEvent = draft.toSendConfirmed())
    }

    fun onSendFailed() = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.SendFailed(),
    )

    fun onSendSucceeded(rail: BankRail?) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = rail),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.SendSucceeded(),
    )

    fun sendPaymentAccountOnly(event: TangemPayAnalyticsEvents) =
        send(paymentAccountEvent = event, moneyAccountEvent = null)

    private fun send(paymentAccountEvent: TangemPayAnalyticsEvents?, moneyAccountEvent: MoneyAccountAnalyticsEvents?) {
        val event = when (sourceAccount) {
            is BankWithdrawSourceAccount.Payment -> paymentAccountEvent
            is BankWithdrawSourceAccount.Money -> moneyAccountEvent
        }
        event?.let(analytics::send)
    }

    private fun BankWithdrawDraft.toSendConfirmed(): MoneyAccountAnalyticsEvents.SendConfirmed? {
        val amount = amount ?: return null
        val method = when (destination?.rail) {
            BankRail.ACH -> MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_ACH
            BankRail.WIRE -> MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_WIRE
            null -> return null
        }
        return MoneyAccountAnalyticsEvents.SendConfirmed(amountUsdc = amount, method = method)
    }
}