package com.tangem.features.approval.impl

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.GiveApprovalEntryComponent
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Default implementation of [GiveApprovalEntryComponent].
 *
 * Picks the concrete child component (full [GiveApprovalComponent] or selection-only
 * [SelectApprovalTypeComponent]) at construction time based on
 * [GiveApprovalEntryComponent.Params.mode] and delegates [BottomSheet] and [dismiss] to it.
 *
 * Callers only need to depend on [GiveApprovalEntryComponent.Factory] regardless of the
 * underlying mode.
 */
internal class DefaultGiveApprovalEntryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: GiveApprovalEntryComponent.Params,
    giveApprovalComponentFactory: GiveApprovalComponent.Factory,
    selectApprovalTypeComponentFactory: SelectApprovalTypeComponent.Factory,
) : GiveApprovalEntryComponent, AppComponentContext by appComponentContext {

    private val delegate: ComposableBottomSheetComponent = when (val mode = params.mode) {
        is GiveApprovalEntryComponent.Mode.FullApproval -> giveApprovalComponentFactory.create(
            context = child("giveApprovalEntry_full"),
            params = mode.params,
        )
        is GiveApprovalEntryComponent.Mode.SelectOnly -> selectApprovalTypeComponentFactory.create(
            context = child("giveApprovalEntry_select"),
            params = mode.params,
        )
    }

    override fun dismiss() {
        delegate.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        delegate.BottomSheet()
    }

    @AssistedFactory
    interface Factory : GiveApprovalEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: GiveApprovalEntryComponent.Params,
        ): DefaultGiveApprovalEntryComponent
    }
}