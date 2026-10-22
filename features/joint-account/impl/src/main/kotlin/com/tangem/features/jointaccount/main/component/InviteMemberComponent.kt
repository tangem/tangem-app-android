package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.main.ui.InviteMemberBS

/**
 * "Invite Member" sheet: how joining works and the invite message, shared from the sheet.
 *
 * Internal to the feature: hosted only by [DefaultJointAccountMembersComponent], so it is constructed
 * directly rather than through an api contract / Dagger factory.
 */
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

    /**
     * @property message      the invite text shown in the sheet.
     * @property onShareClick send the resolved [message] via the system share sheet.
     * @property onDismiss    close the sheet.
     */
    data class Params(
        val message: TextReference,
        val onShareClick: (message: String) -> Unit,
        val onDismiss: () -> Unit,
    )
}