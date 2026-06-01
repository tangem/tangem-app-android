package com.tangem.features.approval.api

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Selection-only variant of [GiveApprovalComponent].
 *
 * Shows the same approval-type selector UI (LIMITED vs UNLIMITED) but does NOT submit the
 * approval transaction. Instead, the chosen [ApproveType] is returned to the caller via
 * [Callback.onApproveTypeSelected] when the user confirms. The caller is responsible for any
 * downstream action (e.g. building the transaction, sending it, navigation).
 *
 * Intended for flows where the approval-type choice has to be collected separately from the
 * actual fee selection / transaction submission step.
 */
interface SelectApprovalTypeComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val amountFooter: TextReference,
        val initialApproveType: ApproveType = ApproveType.LIMITED,
        val spenderAddress: String,
        val callback: Callback,
    )

    interface Callback {
        fun onApproveTypeSelected(spenderAddress: String, approveType: ApproveType)
        fun onCancelClick()
    }

    interface Factory {
        fun create(context: AppComponentContext, params: Params): SelectApprovalTypeComponent
    }
}