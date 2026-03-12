@file:Suppress("all")

package com.tangem.core.ui.components.sheetscaffold

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue.*
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/bottom-sheets/overview" class="external"
 * target="_blank">Material Design standard bottom sheet scaffold</a>.
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * This component provides API to put together several material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * @param sheetPeekHeight the height of the bottom sheet when it is collapsed
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param scaffoldState the state of the bottom sheet scaffold
 * @param topBar top app bar of the screen.
 * @param snackbarHost component to host [TangemTopSnackbar]s that are pushed to be shown via
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
    sheetPeekHeight: Dp,
    modifier: Modifier = Modifier,
    scaffoldState: TangemBottomSheetScaffoldState = rememberTangemBottomSheetScaffoldState(),
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = TangemTheme.colors.background.secondary,
    contentColor: Color = contentColorFor(containerColor),
    bottomSheet: @Composable () -> Unit = {},
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
        bottomSheet = bottomSheet,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomSheet(
    state: TangemSheetState,
    peekHeight: Dp,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetSwipeEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical

    val peekHeightPx = with(LocalDensity.current) { peekHeight.toPx() }
    val nestedScroll = Modifier.conditionalCompose(sheetSwipeEnabled) {
        nestedScroll(
            remember(state.anchoredDraggableState) {
                consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                    sheetState = state,
                    orientation = orientation,
                    onFling = { scope.launch { state.settle(it) } },
                )
            },
        )
    }

    Column(
        modifier = Modifier
            .widthIn(max = sheetMaxWidth)
            .fillMaxWidth()
            .requiredHeightIn(min = peekHeight)
            .then(nestedScroll)
            .bottomSheetDraggableAnchor(
                state = state,
                orientation = orientation,
                peekHeightPx = peekHeightPx,
            )
            .anchoredDraggable(
                state = state.anchoredDraggableState,
                orientation = orientation,
                enabled = sheetSwipeEnabled,
            )
            .then(modifier),
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
        contents = listOf(
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
            val snackbarOffsetY = when (sheetState.currentValue) {
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

private fun Modifier.bottomSheetDraggableAnchor(
    state: TangemSheetState,
    orientation: Orientation,
    peekHeightPx: Float,
): Modifier {
    return draggableAnchors(
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
                    Hidden -> if (newAnchors.hasPositionFor(Hidden)) Hidden else oldTarget
                    PartiallyExpanded ->
                        when {
                            newAnchors.hasPositionFor(PartiallyExpanded) -> PartiallyExpanded
                            newAnchors.hasPositionFor(Expanded) -> Expanded
                            newAnchors.hasPositionFor(Hidden) -> Hidden
                            else -> oldTarget
                        }
                    Expanded ->
                        when {
                            newAnchors.hasPositionFor(Expanded) -> Expanded
                            newAnchors.hasPositionFor(PartiallyExpanded) -> PartiallyExpanded
                            newAnchors.hasPositionFor(Hidden) -> Hidden
                            else -> oldTarget
                        }
                }
            newAnchors to newTarget
        },
    )
}