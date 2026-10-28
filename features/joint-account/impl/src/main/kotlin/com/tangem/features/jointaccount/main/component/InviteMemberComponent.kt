package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.ui.InviteMemberBS

internal class InviteMemberComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {
    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        InviteMemberBS(message = params.message, onShareClick = params.onShareClick, onDismiss = ::dismiss)
    }

    data class Params(
        val message: TextReference,
        val onShareClick: (message: String) -> Unit,
        val onDismiss: () -> Unit,
    )
}