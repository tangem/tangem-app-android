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

    /**
     * A step reopened from the confirm screen to correct one answer. It is deliberately absent from the
     * wizard's step order, so it carries no step counter and Continue returns to the confirm screen
     * instead of advancing the wizard.
     */
    @Serializable
    data class Edit(val target: BankWithdrawEditTarget) : TangemPayBankWithdrawInnerRoute()
}