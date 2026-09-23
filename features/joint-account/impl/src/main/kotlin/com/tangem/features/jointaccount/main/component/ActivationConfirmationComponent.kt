package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.jointaccount.main.ui.JointAccountActivationConfirmationBS
import kotlinx.coroutines.flow.StateFlow

/**
 * "Activate account" confirmation modal.
 *
 * Presented as a bottom-sheet child slot over the members screen. Internal to the feature: hosted only
 * by [DefaultJointAccountMembersComponent], so it is constructed directly rather than through an api
 * contract / Dagger factory.
 */
internal class ActivationConfirmationComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val isConfirming by params.isConfirming.collectAsStateWithLifecycle()

        JointAccountActivationConfirmationBS(
            walletInteractionIconRes = params.walletInteractionIconRes,
            isConfirming = isConfirming,
            onConfirmClick = params.onConfirmClick,
            onDismiss = ::dismiss,
        )
    }

    /**
     * @property walletInteractionIconRes wallet-interaction icon on the confirm button; `null` for wallets
     * without one (hot).
     * @property isConfirming             the signing and the backend call are in flight — the confirm
     * button shows a loader.
     * @property onConfirmClick           proceed with the activation (leads into the NFC signing session).
     * @property onDismiss                close the sheet; cancelling, swiping it away or the close icon all
     * act the same.
     */
    data class Params(
        val walletInteractionIconRes: Int?,
        val isConfirming: StateFlow<Boolean>,
        val onConfirmClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}