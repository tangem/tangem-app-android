package com.tangem.core.ui.ds.topbar.collapsing

import android.content.res.Configuration
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.topbar.collapsing.entity.TangemCollapsingAppBarState
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
fun TangemCollapsingTopBar(
    state: TangemCollapsingAppBarState,
    collapsingPart: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            collapsingPart()
            body()
        },
    ) { measurables, constraints ->

        val collapsingConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
        )
        val collapsingPlaceable = measurables[0].measure(collapsingConstraints)

        val bodyConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxHeight = (constraints.maxHeight - collapsingConstraints.minHeight).coerceAtLeast(0),
        )
        val bodyPlaceable = measurables[1].measure(bodyConstraints)

        val minHeight = 0.dp.roundToPx()
        val maxHeight = collapsingPlaceable.height + minHeight

        val offset = state.heightOffset.roundToInt().coerceAtLeast(-maxHeight)

        val width = max(
            collapsingPlaceable.width,
            bodyPlaceable.width,
        ).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = max(
            collapsingPlaceable.height,
            bodyPlaceable.height,
        ).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width = width, height = height) {
            bodyPlaceable.placeRelative(0, collapsingPlaceable.height + offset)
            collapsingPlaceable.placeRelative(0, offset)
        }
    }
}

/**
 * A scroll behavior for a collapsing top app bar that collapses when scrolling up and expands when scrolling down.
 *
 * @property state              The state of the collapsing app bar.
 * @property snapAnimationSpec  The animation spec used for snapping the app bar to its collapsed or
 *                              expanded state after a fling. If null, no snapping will occur.
 * @property flingAnimationSpec The decay animation spec used for fling gestures.
 *                              If null, fling gestures will not be handled.
 * @property nestedScrollConnection Nested scroll connection
 */
@Stable
data class TangemCollapsingAppBarBehavior(
    val state: TangemCollapsingAppBarState,
    val snapAnimationSpec: AnimationSpec<Float>?,
    val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val nestedScrollConnection: NestedScrollConnection,
)

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemCollapsingTopBar_Preview() {
    TangemThemePreviewRedesign {
        val collapsingHeight = 200.dp
        val behavior = rememberTangemExitUntilCollapsedScrollBehavior(
            expandedHeight = collapsingHeight,
        )
        TangemCollapsingTopBar(
            state = behavior.state,
            collapsingPart = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(collapsingHeight)
                        .background(Color.Red),
                )
            },
            body = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Blue)
                        .nestedScroll(behavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState()),
                )
            },
        )
    }
}
// endregion