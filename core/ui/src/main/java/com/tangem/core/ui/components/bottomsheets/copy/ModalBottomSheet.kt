/*

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tangem.core.ui.components.bottomsheets.copy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.bottomsheets.copy.internal.DragHandleWithTooltip
import com.tangem.core.ui.components.bottomsheets.copy.internal.ModalBottomSheetDialog
import com.tangem.core.ui.components.bottomsheets.copy.internal.ModalBottomSheetProperties
import com.tangem.core.ui.components.bottomsheets.copy.internal.StandardMotionTokens
import com.tangem.core.ui.components.sheetscaffold.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * [Material Design modal bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 *   animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param contentWindowInsets callback which provides window insets to be passed to the bottom sheet
 *   content via [Modifier.windowInsetsPadding]. [ModalBottomSheet] will pre-emptively consume top
 *   insets based on it's current offset. This keeps content outside of the expected window insets
 *   at any position.
 * @param properties [ModalBottomSheetProperties] for further customization of this modal bottom
 *   sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
@Suppress(
    "LongParameterList",
    "LongMethod",
    "MagicNumber",
    "ComposableEventParameterNaming",
    "ComposableParametersOrdering",
    "ReusedModifierInstance",
)
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: TangemSheetState = rememberTangemStandardBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    peekHeightDp: Dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        scope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
    }
    val settleToDismiss: (velocity: Float) -> Unit = {
        scope
            .launch { sheetState.settle(it) }
            .invokeOnCompletion { if (!sheetState.isVisible) onDismissRequest() }
    }

    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }

    ModalBottomSheetDialog(
        properties = properties,
        contentColor = contentColor,
        onDismissRequest = {
            if (sheetState.currentValue == TangemSheetValue.Expanded && sheetState.hasPartiallyExpandedState) {
                // Smoothly animate away predictive back transformations since we are not fully
                // dismissing. We don't need to do this in the else below because we want to
                // preserve the predictive back transformations (scale) during the hide animation.
                scope.launch { predictiveBackProgress.animateTo(0f) }
                scope.launch { sheetState.partialExpand() }
            } else { // Is expanded without collapsed state or is collapsed.
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            }
        },
        predictiveBackProgress = predictiveBackProgress,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .semantics { isTraversalGroup = true },
        ) {
            Scrim(
                color = scrimColor,
                onDismissRequest = animateToDismiss,
                visible = sheetState.targetValue != TangemSheetValue.Hidden,
                dismissEnabled = properties.shouldDismissOnClickOutside,
            )
            ModalBottomSheetContent(
                predictiveBackProgress = predictiveBackProgress,
                scope = scope,
                animateToDismiss = animateToDismiss,
                settleToDismiss = settleToDismiss,
                modifier = modifier,
                sheetState = sheetState,
                sheetMaxWidth = sheetMaxWidth,
                sheetGesturesEnabled = sheetGesturesEnabled,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                peekHeightDp = peekHeightDp,
                dragHandle = dragHandle,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

@Composable
@ExperimentalMaterial3Api
@Suppress(
    "LongParameterList",
    "LongMethod",
    "MagicNumber",
    "ComposableEventParameterNaming",
    "ComposableParametersOrdering",
)
internal fun BoxScope.ModalBottomSheetContent(
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
    animateToDismiss: () -> Unit,
    settleToDismiss: (velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: TangemSheetState = rememberTangemStandardBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    peekHeightDp: Dp,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit,
) {
    val orientation = Orientation.Vertical
    val peekHeightPx = with(LocalDensity.current) { peekHeightDp.toPx() }

    Surface(
        modifier =
        modifier
            .align(Alignment.TopCenter)
            .widthIn(max = sheetMaxWidth)
            .fillMaxWidth()
            .then(
                if (sheetGesturesEnabled) {
                    Modifier.nestedScroll(
                        remember(sheetState) {
                            consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                sheetState = sheetState,
                                orientation = Orientation.Vertical,
                                onFling = settleToDismiss,
                            )
                        },
                    )
                } else {
                    Modifier
                },
            )
            .bottomSheetDraggableAnchor(sheetState, Orientation.Vertical, peekHeightPx)
            .anchoredDraggable(
                state = sheetState.anchoredDraggableState,
                orientation = orientation,
                enabled = sheetGesturesEnabled,
            )
            .consumeWindowInsets(WindowInsets(top = sheetState.offset.toInt().coerceAtLeast(0)))
            .graphicsLayer {
                val sheetOffset = sheetState.anchoredDraggableState.offset
                val sheetHeight = size.height
                if (!sheetOffset.isNaN() && !sheetHeight.isNaN() && sheetHeight != 0f) {
                    val progress = predictiveBackProgress.value
                    scaleX = calculatePredictiveBackScaleX(progress)
                    scaleY = calculatePredictiveBackScaleY(progress)
                    @Suppress("MagicNumber")
                    transformOrigin =
                        TransformOrigin(0.5f, (sheetOffset + sheetHeight) / sheetHeight)
                }
            },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
                .graphicsLayer {
                    val progress = predictiveBackProgress.value
                    val predictiveBackScaleX = calculatePredictiveBackScaleX(progress)
                    val predictiveBackScaleY = calculatePredictiveBackScaleY(progress)

                    // Preserve the original aspect ratio and alignment of the child content.
                    scaleY =
                        if (predictiveBackScaleY != 0f) {
                            predictiveBackScaleX / predictiveBackScaleY
                        } else {
                            1f
                        }
                    transformOrigin = PredictiveBackChildTransformOrigin
                },
        ) {
            if (dragHandle != null) {
                DragHandleWithTooltip {
                    Box(
                        modifier =
                        Modifier
                            .clickable {
                                when (sheetState.currentValue) {
                                    TangemSheetValue.Expanded -> animateToDismiss()
                                    TangemSheetValue.PartiallyExpanded -> scope.launch { sheetState.expand() }
                                    else -> scope.launch { sheetState.show() }
                                }
                            },
                    ) {
                        dragHandle()
                    }
                }
            }
            content()
        }
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

@Composable
private fun Scrim(color: Color, onDismissRequest: () -> Unit, visible: Boolean, dismissEnabled: Boolean) {
    // TODO Load the motionScheme tokens from the component tokens file
    if (color.isSpecified) {
        val alpha by
            animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = StandardMotionTokens.SpringDefaultEffectsDamping,
                    stiffness = StandardMotionTokens.SpringDefaultEffectsStiffness,
                ),
            )
        val dismissSheet =
            if (dismissEnabled) {
                Modifier
                    .pointerInput(onDismissRequest) { detectTapGestures { onDismissRequest() } }
            } else {
                Modifier
            }
        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissSheet),
        ) {
            drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
        }
    }
}

private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
private val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)