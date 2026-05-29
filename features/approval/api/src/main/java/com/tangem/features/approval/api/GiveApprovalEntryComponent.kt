package com.tangem.features.approval.api

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

/**
 * Entry component that wraps the two approval-flow variants:
 *
 * - [Mode.FullApproval] — original [GiveApprovalComponent] which renders the approval-type
 *   selector together with the fee selector and submits the approval transaction.
 * - [Mode.SelectOnly] — [SelectApprovalTypeComponent] which only collects the approval-type
 *   choice and returns it to the caller via its own [SelectApprovalTypeComponent.Callback].
 *
 * Callers depend only on this single factory and pass the appropriate [Mode]; the entry
 * component internally creates the corresponding child and delegates the bottom sheet
 * rendering and dismissal to it.
 */
interface GiveApprovalEntryComponent : ComposableBottomSheetComponent {

    data class Params(
        val mode: Mode,
    )

    sealed interface Mode {

        /** Full flow: approval-type selector + fee selector + transaction submission. */
        data class FullApproval(
            val params: GiveApprovalComponent.Params,
        ) : Mode

        /** Selection-only flow: returns the chosen approval type without sending anything. */
        data class SelectOnly(
            val params: SelectApprovalTypeComponent.Params,
        ) : Mode
    }

    interface Factory {
        fun create(context: AppComponentContext, params: Params): GiveApprovalEntryComponent
    }
}