package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.reportMeasuredHeight
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes

private const val STEP_FADE_MILLIS = 100

@Composable
internal fun TokenReceiveContentModal(
    contentStack: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        title = {
            ModalTopNavigation(
                route = contentStack.active.configuration,
                onBackClick = onBackClick,
                onCloseClick = onCloseClick,
            )
        },
        content = {
            IntrinsicFreeBox {
                Children(
                    stack = contentStack,
                    animation = stackAnimation(fade(animationSpec = tween(durationMillis = STEP_FADE_MILLIS))),
                    modifier = Modifier.fillMaxWidth(),
                ) { child ->
                    child.instance.Content(Modifier.fillMaxWidth())
                }
            }
        },
    )
}

@Composable
private fun ModalTopNavigation(route: TokenReceiveRoutes, onBackClick: () -> Unit, onCloseClick: () -> Unit) {
    when (route) {
        TokenReceiveRoutes.Warning -> TangemTopNavigation(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
        TokenReceiveRoutes.ReceiveAssets -> TangemTopNavigation(
            title = resourceReference(R.string.domain_receive_assets_navigation_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            onClose = onCloseClick,
        )
        is TokenReceiveRoutes.QrCode -> TangemTopNavigation(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            startButton = { TangemButton.Back(onClick = onBackClick) },
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
    }
}

@Composable
private fun IntrinsicFreeBox(content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = Modifier.reportMeasuredHeight(),
        measurePolicy = IntrinsicFreeMeasurePolicy,
    )
}

private object IntrinsicFreeMeasurePolicy : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        return layout(width, height) {
            placeables.forEach { it.placeRelative(x = 0, y = 0) }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
}