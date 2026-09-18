package com.tangem.features.moneyaccount.details.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Two stages: an educational intro and the requisites. Set [Params.shouldSkipIntro] to open straight at the
 * requisites — used by callers (e.g. TangemPay) that already show their own intro.
 */
interface MoneyAccountAddFundsBottomSheetComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val requisites: List<RequisitesRow>,
        val dailyDepositLimit: String,
        val listener: MoneyAccountAddFundsListener,
        val shouldSkipIntro: Boolean = false,
        // Analytics hooks — supplied by callers that track this sheet (e.g. TangemPay VA topup); no-op otherwise.
        val onDetailsShown: () -> Unit = {},
        val onShareClicked: () -> Unit = {},
        val onFieldCopied: (fieldName: String) -> Unit = {},
    )

    data class RequisitesRow(
        val title: TextReference,
        val titleForShare: String,
        val value: String,
    )

    interface Factory : ComponentFactory<Params, MoneyAccountAddFundsBottomSheetComponent>
}

fun interface MoneyAccountAddFundsListener {
    fun onAddFundsDismiss()
}