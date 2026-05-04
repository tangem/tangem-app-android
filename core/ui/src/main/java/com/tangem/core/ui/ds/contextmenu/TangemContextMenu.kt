package com.tangem.core.ui.ds.contextmenu

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.PopUpMenuTestTags
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.max
import kotlin.math.min

/**
 * Just copy paste [DropdownMenu] from material3 with deleting vertical paddings.
 */
@Composable
fun TangemContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    properties: PopupProperties = PopupProperties(focusable = true),
    positionProvider: PopupPositionProvider? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider = positionProvider ?: DropdownMenuPositionProvider(
            offset,
            density,
        ) { parentBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
        }

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties,
        ) {
            DropdownMenuContent(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
                modifier = modifier,
                content = content,
            )
        }
    }
}

private const val IN_TRANSITION_DURATION = 120
private const val OUT_TRANSITION_DURATION = 75

@Suppress("ReusedModifierInstance", "MagicNumber")
@Composable
private fun DropdownMenuContent(
    expandedStates: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Menu open/close animation.
    val transition = rememberTransition(expandedStates, "DropDownMenu")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = IN_TRANSITION_DURATION,
                    easing = LinearOutSlowInEasing,
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = OUT_TRANSITION_DURATION - 1,
                )
            }
        },
        label = "",
    ) { isExpanded ->
        if (isExpanded) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0.8f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = OUT_TRANSITION_DURATION)
            }
        },
        label = "",
    ) { isExpanded ->
        if (isExpanded) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0f
        }
    }
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens2.x5))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = transformOriginState.value
            },
        elevation = CardDefaults.cardElevation(),
    ) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(TangemTheme.dimens2.x5))
                .background(TangemTheme.colors2.contextMenu.background)
                .testTag(PopUpMenuTestTags.CONTAINER),
            content = content,
        )
    }
}

private fun calculateTransformOrigin(parentBounds: IntRect, menuBounds: IntRect): TransformOrigin {
    val pivotX = when {
        menuBounds.left >= parentBounds.right -> 0f
        menuBounds.right <= parentBounds.left -> 1f
        menuBounds.width == 0 -> 0f
        else -> {
            val intersectionCenter =
                (max(parentBounds.left, menuBounds.left) + min(parentBounds.right, menuBounds.right)) / 2
            (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
        }
    }
    val pivotY = when {
        menuBounds.top >= parentBounds.bottom -> 0f
        menuBounds.bottom <= parentBounds.top -> 1f
        menuBounds.height == 0 -> 0f
        else -> {
            val intersectionCenter =
                (max(parentBounds.top, menuBounds.top) + min(parentBounds.bottom, menuBounds.bottom)) / 2
            (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
        }
    }
    return TransformOrigin(pivotX, pivotY)
}

private val MenuVerticalMargin = 48.dp

@Immutable
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> },
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft,
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight,
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull { element ->
            element >= verticalMargin &&
                element + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(
                left = x,
                top = y,
                right = x + popupContentSize.width,
                bottom = y + popupContentSize.height,
            ),
        )
        return IntOffset(x, y)
    }
}

/**
 * A [PopupPositionProvider] that centers the popup horizontally on the screen
 * and positions it below the anchor. If there is not enough space below,
 * it positions the popup above the anchor. If there is no space in either direction,
 * it reports the required vertical shift via [onAnchorShiftRequired] so the caller
 * can move the anchor upward to make room below.
 */
@Immutable
class CenteredContextMenuPositionProvider(
    private val contentOffset: DpOffset,
    private val density: Density,
    private val onAnchorShiftRequired: (Int) -> Unit = {},
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }
        val x = (windowSize.width - popupContentSize.width) / 2

        val yBelow = anchorBounds.bottom + contentOffsetY
        val yAbove = anchorBounds.top - contentOffsetY - popupContentSize.height

        val isFitsBelow = yBelow + popupContentSize.height <= windowSize.height
        val isFitsAbove = yAbove >= 0

        val y = when {
            isFitsBelow -> {
                onAnchorShiftRequired(0)
                yBelow
            }
            isFitsAbove -> {
                onAnchorShiftRequired(0)
                yAbove
            }
            else -> {
                // Neither fits — calculate how much the anchor must shift up
                // so the popup fits below. Place popup at bottom edge of screen.
                val desiredY = windowSize.height - popupContentSize.height
                val shift = yBelow - desiredY
                onAnchorShiftRequired(shift)
                desiredY
            }
        }

        return IntOffset(x, y)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemContextMenu_Preview() {
    TangemThemePreviewRedesign {
        val hazeState = rememberHazeState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors2.surface.level1)
                .hazeSourceTangem(state = hazeState, zIndex = -1f),
        ) {
            TangemContextMenu(
                expanded = true,
                onDismissRequest = { },
                modifier = Modifier.hazeEffectTangem(state = hazeState),
            ) {
                TangemContextMenuCheckboxItem(
                    title = stringReference("Sort by balance"),
                    isChecked = true,
                    onClick = {},
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = TangemTheme.colors2.border.neutral.quaternary,
                )
                TangemContextMenuCheckboxItem(
                    title = stringReference("Group tokens"),
                    isChecked = false,
                    onClick = {},
                )
            }
        }
    }
}
// endregion