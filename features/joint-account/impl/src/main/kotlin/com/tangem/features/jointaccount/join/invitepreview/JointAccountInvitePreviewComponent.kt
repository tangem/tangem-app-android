package com.tangem.features.jointaccount.join.invitepreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.join.confirmation.ui.JointAccountJoinConfirmationBS
import com.tangem.features.jointaccount.join.invitepreview.model.JointAccountInvitePreviewModel
import com.tangem.features.jointaccount.join.invitepreview.ui.JointAccountInvitePreviewScreen
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams

internal class JointAccountInvitePreviewComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountJoinChildParams,
    childFactory: JointAccountInvitePreviewChildFactory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountInvitePreviewModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "jointAccountJoinBottomSheetSlot",
        childFactory = { config, componentContext ->
            childFactory.createChild(
                config = config,
                appComponentContext = childByContext(componentContext),
                model = model,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        JointAccountInvitePreviewScreen(state = state, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()

        (state as? JointAccountInvitePreviewUM.Content)?.confirmation?.let { confirmation ->
            JointAccountJoinConfirmationBS(state = confirmation)
        }
    }
}