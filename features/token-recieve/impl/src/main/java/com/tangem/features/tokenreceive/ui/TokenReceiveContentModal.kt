package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.DialogWindowProvider
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.reportMeasuredHeight
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes

private const val STEP_FADE_MILLIS = 100

@Composable
internal fun TokenReceiveContentModal(
    contentStack: ChildStack<TokenReceiveRoutes, ComposableContentComponent>,
    onShareClick: () -> Unit,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val isInsideDialogWindow = LocalView.current.parent is DialogWindowProvider
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onCloseClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        presentation = if (isInsideDialogWindow) TangemModal.Presentation.Dialog else TangemModal.Presentation.Auto,
        scrollableContent = false,
        title = {
            ModalTopNavigation(
                route = contentStack.active.configuration,
                onShareClick = onShareClick,
                onBackClick = onBackClick,
                onCloseClick = onCloseClick,
            )
        },
        content = {
            IntrinsicFreeBox(isFullHeight = contentStack.active.configuration == TokenReceiveRoutes.ReceiveAssets) {
                Children(
                    stack = contentStack,
                    animation = stackAnimation(fade(animationSpec = tween(durationMillis = STEP_FADE_MILLIS))),
                    modifier = Modifier.fillMaxSize(),
                ) { child ->
                    child.instance.Content(Modifier.fillMaxSize())
                }
            }
        },
    )
}

@Composable
private fun ModalTopNavigation(
    route: TokenReceiveRoutes,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    when (route) {
        TokenReceiveRoutes.Warning -> TangemTopNavigation(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
        TokenReceiveRoutes.ReceiveAssets -> TangemTopNavigation(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            startButton = {
                TangemButton(
                    variant = TangemButton.Variant.Material,
                    size = TangemButton.Size.X11,
                    iconStart = TangemIconUM.Icon(Icons.ic_share_android_20),
                    contentDescription = stringResourceSafe(R.string.common_share),
                    onClick = onShareClick,
                )
            },
            endButton = { TangemButton.Close(onClick = onCloseClick) },
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
private fun IntrinsicFreeBox(isFullHeight: Boolean, content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = if (isFullHeight) Modifier.windowHeightIntrinsics() else Modifier.reportMeasuredHeight(),
        measurePolicy = IntrinsicFreeMeasurePolicy,
    )
}

@Composable
private fun Modifier.windowHeightIntrinsics(): Modifier {
    return this then WindowHeightIntrinsicsElement(height = LocalWindowInfo.current.containerSize.height)
}

private data class WindowHeightIntrinsicsElement(val height: Int) : ModifierNodeElement<WindowHeightIntrinsicsNode>() {

    override fun create() = WindowHeightIntrinsicsNode(height)

    override fun update(node: WindowHeightIntrinsicsNode) {
        node.height = height
    }
}

private class WindowHeightIntrinsicsNode(var height: Int) : LayoutModifierNode, Modifier.Node() {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = height
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