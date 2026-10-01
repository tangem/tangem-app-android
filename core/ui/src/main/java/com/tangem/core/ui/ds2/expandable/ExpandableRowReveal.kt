package com.tangem.core.ui.ds2.expandable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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