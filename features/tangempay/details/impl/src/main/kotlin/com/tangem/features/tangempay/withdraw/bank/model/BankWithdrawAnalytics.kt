package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AmountErrorReason
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.RecipientType
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.SendMethod
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.math.BigDecimal

internal class BankWithdrawAnalytics @AssistedInject constructor(
    @Assisted private val sourceAccount: BankWithdrawSourceAccount,
    private val analytics: AnalyticsEventHandler,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) {

    fun onRecipientListOpened() = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactsScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientListOpened(),
    )

    fun onRecipientSelected(rail: BankRail?) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactOnContactsScreenClicked(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientSelected(rail = rail.ifMxnEnabled()),
    )

    fun onSavedRecipientPicked(rail: BankRail?) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientSelected(rail = rail).ifMxnEnabled(),
    )

    fun onAmountScreenOpened(rail: BankRail?) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpAmountScreenOpened(rail = rail.ifMxnEnabled()),
    )

    fun onAmountErrorShown(rail: BankRail, reason: AmountErrorReason) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpAmountErrorShown(rail = rail, reason = reason)
            .ifMxnEnabled(),
    )

    fun onRecipientFormOpened(draft: BankWithdrawDraft) {
        val rail = draft.destination?.rail ?: return
        send(
            paymentAccountEvent = null,
            moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientFormOpened(
                rail = rail,
                recipientType = draft.recipientType(),
            ).ifMxnEnabled(),
        )
    }

    fun onRecipientSaved(rail: BankRail, draft: BankWithdrawDraft) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientSaved(
            rail = rail,
            recipientType = draft.recipientType(),
        ).ifMxnEnabled(),
    )

    fun onRecipientSaveFailed(rail: BankRail, errorCode: Int?) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientSaveFailed(rail = rail, errorCode = errorCode)
            .ifMxnEnabled(),
    )

    fun onConfirmationScreenOpened(draft: BankWithdrawDraft) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpConfirmationScreenOpened(
            rail = draft.destination?.rail.ifMxnEnabled(),
        ),
    )

    fun onQuoteShown(rail: BankRail, amount: BigDecimal) = send(
        paymentAccountEvent = null,
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpQuoteShown(
            rail = rail,
            amountBand = MoneyAccountAnalyticsEvents.AmountBand.of(amount),
        ).ifMxnEnabled(),
    )

    fun onSendClicked(draft: BankWithdrawDraft) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.OfframpRecipientTransferConfirmed(
            rail = draft.destination?.rail.ifMxnEnabled(),
        ),
    )

    fun onSendAttempted(draft: BankWithdrawDraft) =
        send(paymentAccountEvent = null, moneyAccountEvent = draft.toSendConfirmed())

    fun onSendFailed(errorCode: Int?, rail: BankRail?) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened(),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.SendFailed(
            errorCode = errorCode,
            method = rail.ifMxnEnabled()?.let(SendMethod::of),
        ),
    )

    fun onSendSucceeded(rail: BankRail?) = send(
        paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = rail),
        moneyAccountEvent = MoneyAccountAnalyticsEvents.SendSucceeded(
            method = rail.ifMxnEnabled()?.let(SendMethod::of),
        ),
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

    private fun <T> T.ifMxnEnabled(): T? = takeIf { moneyAccountFeatureToggles.isMxnEnabled }

    private fun BankWithdrawDraft.toSendConfirmed(): MoneyAccountAnalyticsEvents.SendConfirmed? {
        val amount = amount ?: return null
        val rail = destination?.rail ?: return null
        if (rail == BankRail.SPEI && !moneyAccountFeatureToggles.isMxnEnabled) return null
        return MoneyAccountAnalyticsEvents.SendConfirmed(amountUsdc = amount, method = SendMethod.of(rail))
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceAccount: BankWithdrawSourceAccount): BankWithdrawAnalytics
    }

    private fun BankWithdrawDraft.recipientType(): RecipientType = when {
        entry == BankWithdrawEntry.NewBusiness -> RecipientType.BUSINESS
        destination?.rail == BankRail.SPEI -> RecipientType.OWN
        newRecipient.isThirdParty == true -> RecipientType.SOMEONE_ELSE
        else -> RecipientType.OWN
    }
}