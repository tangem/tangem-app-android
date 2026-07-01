package com.tangem.features.virtualaccount.details.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Bottom sheet showing Virtual Account bank-transfer requisites (beneficiary, bank, account & routing numbers).
 *
 * Two stages: an educational intro and the requisites. Set [Params.shouldSkipIntro] to open straight at the
 * requisites — used by callers (e.g. TangemPay) that already show their own intro.
 */
interface VirtualAccountAddFundsBottomSheetComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val requisites: List<RequisitesRow>,
        val dailyDepositLimit: String,
        val listener: VirtualAccountAddFundsListener,
        val shouldSkipIntro: Boolean = false,
    )

    data class RequisitesRow(
        val title: String,
        val titleForShare: String,
        val value: String,
    )

    interface Factory : ComponentFactory<Params, VirtualAccountAddFundsBottomSheetComponent>
}

fun interface VirtualAccountAddFundsListener {
    fun onAddFundsDismiss()
}