package com.tangem.features.txhistory.joint.operation.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.ui.JointOperationSignersTopNavigation
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.features.txhistory.ui.TxHistoryDetailsTopNavigation

/** One sheet for both screens: stepping into the signers and back keeps it on screen */
@Composable
internal fun JointOperationContentSheet(
    state: JointOperationUM.Content?,
    contentStack: ChildStack<JointOperationRoutes, ComposableContentComponent>,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = state != null,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        title = {
            when (contentStack.active.configuration) {
                JointOperationRoutes.Operation -> state?.let { content ->
                    TxHistoryDetailsTopNavigation(header = content.operation.header, onCloseClick = onCloseClick)
                }
                JointOperationRoutes.Signers -> JointOperationSignersTopNavigation(
                    membersCount = state?.signers?.membersCount,
                    onBackClick = onBackClick,
                    onCloseClick = onCloseClick,
                )
            }
        },
        content = {
            Children(
                stack = contentStack,
                animation = stackAnimation(fade(animationSpec = tween(durationMillis = ANIMATION_MILLIS))),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) { child ->
                child.instance.Content(Modifier.fillMaxWidth())
            }
        },
    )
}

private const val ANIMATION_MILLIS = 100