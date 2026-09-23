package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute

/** Step order of the bank withdraw wizard per entry point; the header counts only the steps in [NUMBERED_STEPS]. */
internal object BankWithdrawFlow {

    private val NUMBERED_STEPS = setOf(
        TangemPayBankWithdrawInnerRoute.Amount,
        TangemPayBankWithdrawInnerRoute.TransferType,
        TangemPayBankWithdrawInnerRoute.AccountType,
        TangemPayBankWithdrawInnerRoute.RecipientDetails,
        TangemPayBankWithdrawInnerRoute.BankDetails,
    )

    private val SAVED_STEPS = listOf(
        TangemPayBankWithdrawInnerRoute.Amount,
        TangemPayBankWithdrawInnerRoute.Preview,
    )

    fun steps(entry: BankWithdrawEntry): List<TangemPayBankWithdrawInnerRoute> = when (entry) {
        BankWithdrawEntry.NewPersonal -> listOf(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.TransferType,
            TangemPayBankWithdrawInnerRoute.AccountType,
            TangemPayBankWithdrawInnerRoute.RecipientDetails,
            TangemPayBankWithdrawInnerRoute.BankDetails,
            TangemPayBankWithdrawInnerRoute.Preview,
        )
        BankWithdrawEntry.NewBusiness -> listOf(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.AccountType,
            TangemPayBankWithdrawInnerRoute.RecipientDetails,
            TangemPayBankWithdrawInnerRoute.BankDetails,
            TangemPayBankWithdrawInnerRoute.Preview,
        )
        is BankWithdrawEntry.Saved -> SAVED_STEPS
        BankWithdrawEntry.SeeAll -> listOf(
            TangemPayBankWithdrawInnerRoute.Recipients,
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        )
    }

    /**
     * [steps] of what the draft has become: once a suggested recipient replaces the one being typed, a new-recipient
     * entry continues as the saved-recipient flow, so Continue on the amount leads to the preview and not back to
     * the forms.
     */
    fun steps(draft: BankWithdrawDraft): List<TangemPayBankWithdrawInnerRoute> =
        if (draft.recipient != null && draft.entry.startsNewRecipient()) SAVED_STEPS else steps(draft.entry)

    fun initialStep(entry: BankWithdrawEntry): TangemPayBankWithdrawInnerRoute = steps(entry).first()

    fun nextStep(
        draft: BankWithdrawDraft,
        current: TangemPayBankWithdrawInnerRoute,
    ): TangemPayBankWithdrawInnerRoute? {
        val steps = steps(draft)
        val index = steps.indexOf(current)
        return if (index == -1) null else steps.getOrNull(index + 1)
    }

    /**
     * Position of [step] among the counted steps, or null when the draft shows no counter: a recipient picked from
     * the suggestions drops the remaining forms, so their numbering no longer describes what is left to fill.
     */
    fun stepCounter(draft: BankWithdrawDraft, step: TangemPayBankWithdrawInnerRoute): StepCounter? {
        if (!draft.entry.startsNewRecipient() || draft.recipient != null) return null
        val numbered = steps(draft.entry).filter { it in NUMBERED_STEPS }
        val index = numbered.indexOf(step.toNumberedStep())
        return if (index == -1) null else StepCounter(current = index + 1, total = numbered.size)
    }

    data class StepCounter(val current: Int, val total: Int)

    /** Correcting an answer reopens a wizard step, so it is counted as the step it edits. */
    private fun TangemPayBankWithdrawInnerRoute.toNumberedStep(): TangemPayBankWithdrawInnerRoute = when (this) {
        is TangemPayBankWithdrawInnerRoute.Edit -> when (target) {
            BankWithdrawEditTarget.Amount -> TangemPayBankWithdrawInnerRoute.Amount
            BankWithdrawEditTarget.RecipientDetails -> TangemPayBankWithdrawInnerRoute.RecipientDetails
            BankWithdrawEditTarget.BankDetails -> TangemPayBankWithdrawInnerRoute.BankDetails
        }
        else -> this
    }

    private fun BankWithdrawEntry.startsNewRecipient(): Boolean =
        this == BankWithdrawEntry.NewPersonal || this == BankWithdrawEntry.NewBusiness
}

/**
 * The stack after a saved recipient replaces the one being typed: the forms above the amount describe a recipient
 * that is no longer used, so they leave the stack and the preview opens on top of the amount.
 */
internal fun List<TangemPayBankWithdrawInnerRoute>.cutToAmountThenPreview(): List<TangemPayBankWithdrawInnerRoute> =
    dropLastWhile { it != TangemPayBankWithdrawInnerRoute.Amount }
        .ifEmpty { listOf(TangemPayBankWithdrawInnerRoute.Amount) }
        .plus(TangemPayBankWithdrawInnerRoute.Preview)