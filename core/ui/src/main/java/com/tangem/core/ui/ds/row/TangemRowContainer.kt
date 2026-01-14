package com.tangem.core.ui.ds.row

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import com.tangem.core.ui.res.TangemTheme
import kotlin.math.max

/**
 * A custom layout composable that arranges its children in a row with specific layout IDs.
 */
internal enum class TangemRowLayoutId {
    HEAD, START_TOP, END_TOP, START_BOTTOM, END_BOTTOM, TAIL, EXTRA_TOP
}

/**
 * A custom layout composable that arranges its children in a row with specific layout IDs.
 *
 * @param modifier          Modifier to be applied to the layout.
 * @param contentPadding    Padding values to be applied around the content.
 * @param content           Composable content to be laid out within the TangemRow.
 */
@Suppress("LongMethod")
@Composable
internal fun TangemRowContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(TangemTheme.dimens2.x3),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val localDirection = LocalLayoutDirection.current
    val verticalPadding = with(density) { TangemTheme.dimens2.x1.roundToPx() }
    val contentTopPadding = with(density) { contentPadding.calculateTopPadding().roundToPx() }
    val contentBottomPadding = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
    val contentStartPadding = with(density) { contentPadding.calculateLeftPadding(localDirection).roundToPx() }
    val contentEndPadding = with(density) { contentPadding.calculateRightPadding(localDirection).roundToPx() }
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        val layoutWidth = constraints.maxWidth - contentStartPadding - contentEndPadding

        val startTopMinWidth = (layoutWidth * TITLE_MIN_WIDTH_COEFFICIENT).toInt()
        val startBottomMinWidth = (layoutWidth * PRICE_MIN_WIDTH_COEFFICIENT).toInt()

        // Head composable measurement
        val headPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.HEAD,
            constraints = constraints,
        )

        // Tail composable measurement
        val tailPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.TAIL,
            constraints = constraints,
        )

        val availableWidthForBody = layoutWidth - headPlaceable.widthOrZero() - tailPlaceable.widthOrZero()

        // End top composable width must take the whole free space but is not greater the Start top min size
        val endTopPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.END_TOP,
            constraints = constraints.copy(
                minWidth = 0,
                maxWidth = availableWidthForBody - startTopMinWidth,
            ),
        )

        // End bottom composable width must take the whole free space but is not greater the Start bottom min size
        val endBottomPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.END_BOTTOM,
            constraints = constraints.copy(
                minWidth = 0,
                maxWidth = availableWidthForBody - startBottomMinWidth,
            ),
        )

        /* Start top composable will take take the whole REMAINING width space width but no less than minimum */
        val startTopPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.START_TOP,
            constraints = constraints.copy(
                minWidth = startTopMinWidth,
                maxWidth = max(
                    a = startTopMinWidth,
                    b = availableWidthForBody - endTopPlaceable.widthOrZero(),
                ),
            ),
        )

        /* Start bottom composable will take take the whole REMAINING width space width but no less than minimum */
        val startBottomPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.START_BOTTOM,
            constraints = constraints.copy(
                minWidth = startBottomMinWidth,
                maxWidth = max(
                    a = startBottomMinWidth,
                    b = availableWidthForBody - endBottomPlaceable.widthOrZero(),
                ),
            ),
        )

        val extraTopPlaceable = measurables.measure(
            layoutId = TangemRowLayoutId.EXTRA_TOP,
            constraints = constraints,
        )

        val mainLayoutHeight = maxOf(
            headPlaceable.heightOrZero(),
            tailPlaceable.heightOrZero(),
            startTopPlaceable.heightOrZero() + startBottomPlaceable.heightOrZero() + verticalPadding,
            endTopPlaceable.heightOrZero() + endBottomPlaceable.heightOrZero() + verticalPadding,
        )

        val mainContentTopPadding = if (extraTopPlaceable != null) {
            extraTopPlaceable.heightOrZero()
        } else {
            contentTopPadding
        }

        val layoutHeight = mainLayoutHeight + mainContentTopPadding + contentBottomPadding

        layout(width = constraints.maxWidth, height = layoutHeight) {
            extraTopPlaceable?.placeRelative(x = 0, y = 0)

            headPlaceable?.placeRelative(
                x = contentStartPadding,
                y = mainContentTopPadding + (mainLayoutHeight - headPlaceable.height).div(other = 2),
            )

            startTopPlaceable?.placeRelative(
                x = contentStartPadding + headPlaceable.widthOrZero(),
                y = mainContentTopPadding + if (startBottomPlaceable == null) {
                    (mainLayoutHeight - startTopPlaceable.height).div(2)
                } else {
                    0
                },
            )

            startBottomPlaceable?.placeRelative(
                x = contentStartPadding + headPlaceable.widthOrZero(),
                y = mainContentTopPadding + if (startTopPlaceable == null) {
                    (mainLayoutHeight - startBottomPlaceable.height).div(2)
                } else {
                    startTopPlaceable.heightOrZero() + verticalPadding
                },
            )

            endTopPlaceable?.placeRelative(
                x = layoutWidth - endTopPlaceable.widthOrZero() - tailPlaceable.widthOrZero() + contentEndPadding,
                y = mainContentTopPadding + if (endBottomPlaceable == null) {
                    (mainLayoutHeight - endTopPlaceable.height).div(2)
                } else {
                    0
                },
            )

            endBottomPlaceable?.placeRelative(
                x = layoutWidth - endBottomPlaceable.widthOrZero() - tailPlaceable.widthOrZero() + contentEndPadding,
                y = mainContentTopPadding + if (endTopPlaceable == null) {
                    (mainLayoutHeight - endBottomPlaceable.height).div(2)
                } else {
                    endTopPlaceable.heightOrZero() + verticalPadding
                },
            )

            tailPlaceable?.placeRelative(
                x = layoutWidth - tailPlaceable.width + contentEndPadding,
                y = mainContentTopPadding + (mainLayoutHeight - tailPlaceable.height).div(other = 2),
            )
        }
    }
}

private const val TITLE_MIN_WIDTH_COEFFICIENT = 0.3
private const val PRICE_MIN_WIDTH_COEFFICIENT = 0.32

private fun List<Measurable>.measure(layoutId: TangemRowLayoutId, constraints: Constraints): Placeable? {
    return firstOrNull { it.layoutId == layoutId }?.measure(constraints)
}

private fun Placeable?.widthOrZero(): Int = this?.width ?: 0
private fun Placeable?.heightOrZero(): Int = this?.height ?: 0