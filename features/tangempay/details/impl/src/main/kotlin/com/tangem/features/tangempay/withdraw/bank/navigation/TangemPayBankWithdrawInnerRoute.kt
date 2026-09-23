package com.tangem.features.tangempay.withdraw.bank.navigation

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayBankWithdrawInnerRoute : Route {

    @Serializable
    data object Recipients : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object Amount : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object TransferType : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object AccountType : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object RecipientDetails : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object BankDetails : TangemPayBankWithdrawInnerRoute()

    @Serializable
    data object Preview : TangemPayBankWithdrawInnerRoute()

    /** Terminal screen of a signed order; it is outside the step order, so it carries no counter. */
    @Serializable
    data object Success : TangemPayBankWithdrawInnerRoute()

    /**
     * A step reopened from the confirm screen to correct one answer. It is deliberately absent from the
     * wizard's step order, so Continue returns to the confirm screen instead of advancing the wizard,
     * while the header still numbers it as the step it edits.
     */
    @Serializable
    data class Edit(val target: BankWithdrawEditTarget) : TangemPayBankWithdrawInnerRoute()
}