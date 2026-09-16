package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
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
        is BankWithdrawEntry.Saved -> listOf(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        )
        BankWithdrawEntry.SeeAll -> listOf(
            TangemPayBankWithdrawInnerRoute.Recipients,
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        )
    }

    fun initialStep(entry: BankWithdrawEntry): TangemPayBankWithdrawInnerRoute = steps(entry).first()

    fun nextStep(
        entry: BankWithdrawEntry,
        current: TangemPayBankWithdrawInnerRoute,
    ): TangemPayBankWithdrawInnerRoute? {
        val steps = steps(entry)
        val index = steps.indexOf(current)
        return if (index == -1) null else steps.getOrNull(index + 1)
    }

    /** Position of [step] among the counted steps, or null when the entry shows no counter. */
    fun stepCounter(entry: BankWithdrawEntry, step: TangemPayBankWithdrawInnerRoute): StepCounter? {
        val isCounted = entry == BankWithdrawEntry.NewPersonal || entry == BankWithdrawEntry.NewBusiness
        if (!isCounted) return null
        val numbered = steps(entry).filter { it in NUMBERED_STEPS }
        val index = numbered.indexOf(step)
        return if (index == -1) null else StepCounter(current = index + 1, total = numbered.size)
    }

    data class StepCounter(val current: Int, val total: Int)
}