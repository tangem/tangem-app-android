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
}