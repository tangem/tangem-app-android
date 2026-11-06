@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.menu

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min

/**
 * Popup window of [TangemMenu]: positions the menu, leaves room for its shadow and runs the open/close animation.
 * Stays in composition after [isExpanded] turns `false` until the close animation ends.
 *
 * @param content The menu container. Apply the passed modifier to the item list: it fades and blurs the list.
 */
@Suppress("LongParameterList")
@Composable
internal fun TangemMenuPopup(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    variant: TangemMenu.Variant,
    offset: DpOffset,
    positionProvider: PopupPositionProvider?,
    properties: PopupProperties,
    content: @Composable (listModifier: Modifier) -> Unit,
) {
    val visibility = remember { MutableTransitionState(initialState = false) }
    visibility.targetState = isExpanded
    // isIdle too: a menu dismissed mid-open is neither shown nor opening, yet still has to animate out.
    if (!visibility.currentState && !visibility.targetState && visibility.isIdle) return

    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val maxShadowInsetPx = with(LocalDensity.current) { MenuShadowInset.roundToPx() }
    val shadowInsets = remember { ShadowInsets() }
    val pivot = remember { mutableStateOf(MenuPivot(x = 0.5f, y = 0f, slideDirection = -1f)) }
    val resolvedPositionProvider = rememberMenuPositionProvider(
        positionProvider = positionProvider,
        variant = variant,
        offset = offset,
        shadowInsets = shadowInsets,
        onPivotChange = { pivot.value = it },
    )

    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = resolvedPositionProvider,
        properties = properties,
    ) {
        val transition = rememberTransition(visibility, label = "TangemMenu")
        val menuProgress by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 350, easing = if (targetState) EaseOutBack else EaseInBack) },
            label = "menuProgress",
        ) { isShown -> if (isShown) 1f else 0f }
        val listProgress by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 350, easing = if (targetState) EaseOut else EaseIn) },
            label = "listProgress",
        ) { isShown -> if (isShown) 1f else 0f }

        val isClosing = !transition.targetState

        // The popup window is the menu plus room for its shadow, which would be cut at the window edge otherwise.
        // The animation layer spans that room too: a faded layer is drawn offscreen at its own bounds, so a layer
        // tight around the menu would crop the shadow while the menu fades.
        Box(
            modifier = Modifier
                .menuTransform(pivot = { pivot.value }, shadowInsets = shadowInsets, progress = { menuProgress })
                .shadowMarginTaps(isClosing = isClosing, onTap = { currentOnDismissRequest() })
                .shadowInset(maxInsetPx = maxShadowInsetPx, shadowInsets = shadowInsets),
        ) {
            CompositionLocalProvider(LocalTangemMenuClosing provides isClosing) {
                content(
                    Modifier
                        .pointerInput(Unit) { detectTapGestures {} }
                        .listTransform(progress = { listProgress }),
                )
            }
        }
    }
}

@Composable
private fun rememberMenuPositionProvider(
    positionProvider: PopupPositionProvider?,
    variant: TangemMenu.Variant,
    offset: DpOffset,
    shadowInsets: ShadowInsets,
    onPivotChange: (MenuPivot) -> Unit,
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(positionProvider, variant, offset, density, shadowInsets) {
        ShadowInsetPositionProvider(
            delegate = positionProvider ?: with(density) {
                AnchoredMenuPositionProvider(
                    gapPx = when (variant) {
                        TangemMenu.Variant.Button -> 8.dp
                        TangemMenu.Variant.Row -> 12.dp
                    }.roundToPx(),
                    offset = IntOffset(x = offset.x.roundToPx(), y = offset.y.roundToPx()),
                    windowMarginPx = 48.dp.roundToPx(),
                    onPositionCalculated = { anchorBounds, menuBounds ->
                        onPivotChange(MenuPivot.of(anchorBounds = anchorBounds, menuBounds = menuBounds))
                    },
                )
            },
            shadowInsets = shadowInsets,
        )
    }
}

/** Grows, slides and fades the shadow-padded menu from the [pivot] as [progress] goes from 0 to 1. */
private fun Modifier.menuTransform(
    pivot: () -> MenuPivot,
    shadowInsets: ShadowInsets,
    progress: () -> Float,
): Modifier = graphicsLayer {
    val menuPivot = pivot()
    val menuProgress = progress()
    val menuWidth = size.width - 2 * shadowInsets.horizontalPx
    val menuHeight = size.height - 2 * shadowInsets.verticalPx
    transformOrigin = TransformOrigin(
        pivotFractionX = (shadowInsets.horizontalPx + menuPivot.x * menuWidth) / size.width,
        pivotFractionY = (shadowInsets.verticalPx + menuPivot.y * menuHeight) / size.height,
    )
    val scale = lerp(start = 0.8f, stop = 1f, fraction = menuProgress)
    scaleX = scale
    scaleY = scale
    translationY = (1f - menuProgress) * menuPivot.slideDirection * 16.dp.toPx()
    alpha = menuProgress.coerceIn(0f, 1f)
}

/** Taps on the shadow margin land inside the popup window, so they are routed to [onTap] by hand. */
private fun Modifier.shadowMarginTaps(isClosing: Boolean, onTap: () -> Unit): Modifier = pointerInput(isClosing) {
    if (!isClosing) detectTapGestures { onTap() }
}

/**
 * Surrounds the menu with room for its shadow. The menu is measured against the whole window, and the margin
 * shrinks where the window is too narrow or short for it: a clipped shadow beats a menu squeezed below its
 * minimum width. The margin actually used is written to [shadowInsets].
 */
private fun Modifier.shadowInset(maxInsetPx: Int, shadowInsets: ShadowInsets): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val horizontal = if (constraints.hasBoundedWidth) {
            ((constraints.maxWidth - placeable.width) / 2).coerceIn(0, maxInsetPx)
        } else {
            maxInsetPx
        }
        val vertical = if (constraints.hasBoundedHeight) {
            ((constraints.maxHeight - placeable.height) / 2).coerceIn(0, maxInsetPx)
        } else {
            maxInsetPx
        }
        shadowInsets.horizontalPx = horizontal
        shadowInsets.verticalPx = vertical
        layout(width = placeable.width + 2 * horizontal, height = placeable.height + 2 * vertical) {
            placeable.place(x = horizontal, y = vertical)
        }
    }

/** Fades and un-blurs the item list as [progress] goes from 0 to 1. */
private fun Modifier.listTransform(progress: () -> Float): Modifier = graphicsLayer {
    val listProgress = progress()
    alpha = listProgress
    val blurRadius = (1f - listProgress) * 32.dp.toPx()
    renderEffect = if (blurRadius > 0f) {
        BlurEffect(radiusX = blurRadius, radiusY = blurRadius, edgeTreatment = TileMode.Decal)
    } else {
        null
    }
}

/** Covers the material shadow: 40dp blur plus its 8dp downward offset. */
private val MenuShadowInset = 48.dp

/** Shadow margin around the menu as last laid out by [shadowInset], in px. */
private class ShadowInsets {
    var horizontalPx: Int = 0
    var verticalPx: Int = 0
}

/**
 * Point of the menu, as fractions of its size, that the menu grows from: the side facing the anchor, centered on
 * the anchor's overlap with the menu. [slideDirection] moves the hidden menu towards the anchor.
 */
@Immutable
private data class MenuPivot(val x: Float, val y: Float, val slideDirection: Float) {

    companion object {

        fun of(anchorBounds: IntRect, menuBounds: IntRect): MenuPivot {
            val x = when {
                menuBounds.width == 0 || menuBounds.left >= anchorBounds.right -> 0f
                menuBounds.right <= anchorBounds.left -> 1f
                else -> {
                    val overlapCenter = (max(anchorBounds.left, menuBounds.left) +
                        min(anchorBounds.right, menuBounds.right)) / 2
                    (overlapCenter - menuBounds.left).toFloat() / menuBounds.width
                }
            }
            val isAboveAnchor = menuBounds.bottom <= anchorBounds.top
            return MenuPivot(
                x = x,
                y = if (isAboveAnchor) 1f else 0f,
                slideDirection = if (isAboveAnchor) 1f else -1f,
            )
        }
    }
}

/**
 * Places the menu [gapPx] below the anchor, end-aligned with it. Flips above the anchor when the menu does not fit
 * below, and to start alignment when it does not fit towards the start; [offset] is mirrored along with each flip.
 * Reports the anchor and the resulting menu bounds to [onPositionCalculated].
 */
private class AnchoredMenuPositionProvider(
    private val gapPx: Int,
    private val offset: IntOffset,
    private val windowMarginPx: Int,
    private val onPositionCalculated: (anchorBounds: IntRect, menuBounds: IntRect) -> Unit,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = horizontalPosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            width = popupContentSize.width,
        )
        val y = verticalPosition(anchorBounds = anchorBounds, windowSize = windowSize, height = popupContentSize.height)
        onPositionCalculated(anchorBounds, IntRect(offset = IntOffset(x, y), size = popupContentSize))
        return IntOffset(x, y)
    }

    private fun horizontalPosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        width: Int,
    ): Int {
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val alignedToRight = anchorBounds.right - width - offset.x
        val alignedToLeft = anchorBounds.left + offset.x
        val endAligned = if (isLtr) alignedToRight else alignedToLeft
        val startAligned = if (isLtr) alignedToLeft else alignedToRight
        val maxX = (windowSize.width - width).coerceAtLeast(0)
        return sequenceOf(endAligned, startAligned).firstOrNull { it in 0..maxX } ?: endAligned.coerceIn(0, maxX)
    }

    private fun verticalPosition(anchorBounds: IntRect, windowSize: IntSize, height: Int): Int {
        val below = anchorBounds.bottom + gapPx + offset.y
        val above = anchorBounds.top - gapPx - offset.y - height
        val minY = windowMarginPx
        val maxY = (windowSize.height - windowMarginPx - height).coerceAtLeast(minY)
        return when {
            below in minY..maxY -> below
            above in minY..maxY -> above
            windowSize.height - anchorBounds.bottom >= anchorBounds.top -> maxY
            else -> minY
        }
    }
}

/** Places the shadow-padded popup window so that the menu inside it lands where [delegate] puts the menu. */
private class ShadowInsetPositionProvider(
    private val delegate: PopupPositionProvider,
    private val shadowInsets: ShadowInsets,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val insetX = shadowInsets.horizontalPx
        val insetY = shadowInsets.verticalPx
        val menuSize = IntSize(
            width = popupContentSize.width - 2 * insetX,
            height = popupContentSize.height - 2 * insetY,
        )
        val menuPosition = delegate.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = menuSize,
        )
        return IntOffset(x = menuPosition.x - insetX, y = menuPosition.y - insetY)
    }
}