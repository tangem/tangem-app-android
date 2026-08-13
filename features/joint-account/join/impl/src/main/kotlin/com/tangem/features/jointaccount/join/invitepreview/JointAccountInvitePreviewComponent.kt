package com.tangem.features.jointaccount.join.invitepreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.userwallet.picker.ChooseWalletBS
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.join.invitepreview.model.JointAccountInvitePreviewModel
import com.tangem.features.jointaccount.join.invitepreview.ui.JointAccountInvitePreviewScreen
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams

internal class JointAccountInvitePreviewComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountJoinChildParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountInvitePreviewModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountInvitePreviewScreen(state = state, modifier = modifier)
        state.chooseWallet?.let { chooseWallet ->
            ChooseWalletBS(state = chooseWallet)
        }
    }
}