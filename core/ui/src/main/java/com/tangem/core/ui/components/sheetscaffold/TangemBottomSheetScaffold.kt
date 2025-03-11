@file:Suppress("all")

package com.tangem.core.ui.components.sheetscaffold

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.zIndex
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue.*
import com.tangem.core.ui.extensions.softLayerShadow
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.toPx
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/bottom-sheets/overview" class="external"
 * target="_blank">Material Design standard bottom sheet scaffold</a>.
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * This component provides API to put together several material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * @param sheetContent the content of the bottom sheet
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param scaffoldState the state of the bottom sheet scaffold
 * @param sheetPeekHeight the height of the bottom sheet when it is collapsed
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetShape the shape of the bottom sheet
 * @param sheetContainerColor the background color of the bottom sheet
 * @param sheetSwipeEnabled whether the sheet swiping is enabled and should react to the user's
 *   input
 * @param topBar top app bar of the screen, typically a [SmallTopAppBar]
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param containerColor the color used for the background of this scaffold. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@Composable
fun TangemBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: TangemBottomSheetScaffoldState = rememberTangemBottomSheetScaffoldState(),
    sheetPeekHeight: Dp,
    sheetMaxWidth: Dp = 640.dp,
    sheetShape: Shape = TangemTheme.shapes.bottomSheetLarge,
    sheetContainerColor: Color = Color.White,
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = TangemTheme.colors.background.secondary,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomSheetScaffoldLayout(
        modifier = modifier,
        topBar = topBar,
        body = { content(PaddingValues(bottom = sheetPeekHeight)) },
        snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
        sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
        sheetState = scaffoldState.bottomSheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        bottomSheet = {
            StandardBottomSheet(
                state = scaffoldState.bottomSheetState,
                peekHeight = sheetPeekHeight,
                sheetMaxWidth = sheetMaxWidth,
                sheetSwipeEnabled = sheetSwipeEnabled,
                shape = sheetShape,
                containerColor = sheetContainerColor,
                content = sheetContent,
            )
        },
    )
}

/**
 * State of the [TangemBottomSheetScaffold] composable.
 *
 * @param bottomSheetState the state of the persistent bottom sheet
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@Stable
class TangemBottomSheetScaffoldState(
    val bottomSheetState: TangemSheetState,
    val snackbarHostState: SnackbarHostState,
)

/**
 * Create and [remember] a [TangemBottomSheetScaffoldState].
 *
 * @param bottomSheetState the state of the standard bottom sheet. See
 *   [rememberTangemStandardBottomSheetState]
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@Composable
fun rememberTangemBottomSheetScaffoldState(
    bottomSheetState: TangemSheetState = rememberTangemStandardBottomSheetState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): TangemBottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        TangemBottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState,
        )
    }
}

/**
 * Create and [remember] a [TangemSheetState] for [TangemBottomSheetScaffold].
 *
 * @param initialValue the initial value of the state. Should be either [PartiallyExpanded] or
 *   [Expanded] if [skipHiddenState] is true
 * @param confirmValueChange optional callback invoked to confirm or veto a pending state change
 * @param [skipHiddenState] whether Hidden state is skipped for [TangemBottomSheetScaffold]
 */
@Composable
fun rememberTangemStandardBottomSheetState(
    initialValue: TangemSheetValue = PartiallyExpanded,
    confirmValueChange: (TangemSheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = true,
) = rememberSheetState(
    confirmValueChange = confirmValueChange,
    initialValue = initialValue,
    skipHiddenState = skipHiddenState,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StandardBottomSheet(
    state: TangemSheetState,
    peekHeight: Dp,
    sheetMaxWidth: Dp,
    sheetSwipeEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical

    val peekHeightPx = with(LocalDensity.current) { peekHeight.toPx() }
    val nestedScroll =
        if (sheetSwipeEnabled) {
            Modifier.nestedScroll(
                remember(state.anchoredDraggableState) {
                    consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = state,
                        orientation = orientation,
                        onFling = { scope.launch { state.settle(it) } },
                    )
                },
            )
        } else {
            Modifier
        }

    Column(
        modifier = Modifier
            .widthIn(max = sheetMaxWidth)
            .fillMaxWidth()
            .requiredHeightIn(min = peekHeight)
            .then(nestedScroll)
            .draggableAnchors(
                state = state.anchoredDraggableState,
                orientation = orientation,
                anchors = { sheetSize, constraints ->
                    val layoutHeight = constraints.maxHeight.toFloat()
                    val sheetHeight = sheetSize.height.toFloat()

                    val newAnchors = DraggableAnchors {
                        if (!state.skipPartiallyExpanded) {
                            PartiallyExpanded at (layoutHeight - peekHeightPx)
                        }
                        if (sheetHeight != peekHeightPx) {
                            Expanded at maxOf(layoutHeight - sheetHeight, 0f)
                        }
                        if (!state.skipHiddenState) {
                            Hidden at layoutHeight
                        }
                    }
                    val newTarget =
                        when (val oldTarget = state.anchoredDraggableState.targetValue) {
                            Hidden -> if (newAnchors.hasAnchorFor(Hidden)) Hidden else oldTarget
                            PartiallyExpanded ->
                                when {
                                    newAnchors.hasAnchorFor(PartiallyExpanded) -> PartiallyExpanded
                                    newAnchors.hasAnchorFor(Expanded) -> Expanded
                                    newAnchors.hasAnchorFor(Hidden) -> Hidden
                                    else -> oldTarget
                                }
                            Expanded ->
                                when {
                                    newAnchors.hasAnchorFor(Expanded) -> Expanded
                                    newAnchors.hasAnchorFor(PartiallyExpanded) -> PartiallyExpanded
                                    newAnchors.hasAnchorFor(Hidden) -> Hidden
                                    else -> oldTarget
                                }
                        }
                    newAnchors to newTarget
                },
            )
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = orientation,
                enabled = sheetSwipeEnabled,
            )
            .softLayerShadow(
                radius = 8.dp,
                color = Color.Black.copy(
                    alpha = if (isSystemInDarkTheme()) .16f else .08f
                ),
                shape = shape,
                offset = DpOffset(x = 0.dp, y = (-4).dp),
                isAlphaContentClip = true
            )
            .background(containerColor, shape)
            .clip(shape),
    ) {
        content()
    }
}

@Composable
private fun BottomSheetScaffoldLayout(
    modifier: Modifier,
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sheetOffset: () -> Float,
    sheetState: TangemSheetState,
    containerColor: Color,
    contentColor: Color,
) {
    Layout(
        contents =
        listOf<@Composable () -> Unit>(
            topBar ?: {},
            {
                Surface(
                    modifier = modifier,
                    color = containerColor,
                    contentColor = contentColor,
                    content = body,
                )
            },
            bottomSheet,
            snackbarHost,
        ),
    ) {
            (topBarMeasurables, bodyMeasurables, bottomSheetMeasurables, snackbarHostMeasurables),
            constraints,
        ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sheetPlaceables = bottomSheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            val sheetWidth = sheetPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val sheetOffsetX = Integer.max(0, (layoutWidth - sheetWidth) / 2)

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    PartiallyExpanded -> sheetOffset().roundToInt() - snackbarHeight
                    Expanded,
                    Hidden,
                        -> layoutHeight - snackbarHeight
                }

            // Placement order is important for elevation
            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(sheetOffsetX, 0) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}