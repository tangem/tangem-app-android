package com.tangem.core.ui.ds2.expandable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Reveals what an expandable row opens, on that row's own choreography.
 *
 * `TangemAccountRow` and `TangemExpandableRow` render the heading only, so what they reveal is
 * emitted by the host. That split leaves the two halves of one movement in different hands: the card
 * interpolates its own height from the expansion progress, and without this the host would animate
 * its items on some unrelated timing — the body spreading out while the heading is still large, then
 * the heading shrinking afterwards.
 *
 * Apply it to the **group as a whole**, not to each item: clipping items one by one puts a straight
 * edge on whichever item the cut has just finished, which flashes a sharp corner every time the
 * boundary crosses one.
 *
 * It only changes height and opacity. The card's fill and rounding belong to whoever draws the card —
 * give the heading and this group one shared clip, and pass the row `shape = RectangleShape`. Rounding
 * them separately cannot work while the group is shorter than the corner radius: there is nothing for
 * a 24dp corner to sit on, and the heading's own bottom edge reads as a sharp corner for that moment.
 *
 * ```kotlin
 * Column(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(TangemTheme.colors3.bg.secondary)) {
 *     TangemAccountRow(avatar, title, isExpandable = true, isExpanded = isExpanded, shape = RectangleShape)
 *     Column(modifier = Modifier.expandableRowReveal(isExpanded)) {
 *         tokens.forEach { TangemTokenRow(it) }
 *     }
 * }
 * ```
 *
 * @param isExpanded Whether the row that owns this group is open. Pass the same value the row gets,
 * so both run off one transition.
 */
@Composable
fun Modifier.expandableRowReveal(isExpanded: Boolean): Modifier {
    val motion = rememberExpansionMotion(isExpanded = isExpanded)

    return this
        .graphicsLayer { alpha = motion.slotAlpha }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val height = (placeable.height * motion.expansion).roundToInt().coerceIn(0, placeable.height)
            layout(width = placeable.width, height = height) {
                placeable.placeRelative(x = 0, y = 0)
            }
        }
}

/** Time from a toggle until [expandableRowReveal] and [ExpandableRowRevealItem] come to rest. */
const val EXPANDABLE_ROW_REVEAL_DURATION_MILLIS = EXPANSION_DELAY_MS + EXPANSION_DURATION_MS

/**
 * Lazy-list counterpart of [expandableRowReveal], for a host that emits what the row opens as separate
 * lazy items instead of one group.
 *
 * Every item runs off the same choreography as the row, and together they fill from the top the way a
 * single revealed group would: item [index] of [count] grows while the group's revealed height passes
 * over it. Items are treated as equal in height. Once collapsed and at rest, an item composes nothing,
 * so hidden rows cost no more than an empty slot.
 *
 * @param isExpanded Whether the row that owns these items is open. Pass the same value the row gets.
 * @param index Position of this item among the revealed items, starting at 0.
 * @param count Number of revealed items.
 * @param modifier Modifier applied outside the reveal: a background or clip set here follows the
 * revealed height.
 * @param content The item.
 */
@Composable
fun ExpandableRowRevealItem(
    isExpanded: Boolean,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motion = rememberExpansionMotion(isExpanded = isExpanded)
    if (!isExpanded && motion.isSettled) return

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer { alpha = motion.slotAlpha }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fraction = (motion.expansion * count - index).coerceIn(0f, 1f)
                val height = (placeable.height * fraction).roundToInt()
                layout(width = placeable.width, height = height) {
                    placeable.placeRelative(x = 0, y = 0)
                }
            },
    ) {
        content()
    }
}