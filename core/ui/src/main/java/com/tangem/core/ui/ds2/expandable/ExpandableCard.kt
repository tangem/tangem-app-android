@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Slot ids of the [ExpandableCard] custom layout.
 *
 * `INLINE_*` are the compact copies that sit after the title while the row is expanded — the account
 * row uses two (dot, balance), the expandable row four (dot, value, dot, caption). `CONTENT_SLOT` and
 * `HEADING_SLOT` replace the structured children when the caller supplies its own content.
 */
internal enum class ExpandableCardLayoutId {
    AVATAR,
    TITLE,
    TITLE_ANCHOR,
    SUBTITLE,
    END_TOP,
    END_BOTTOM,
    END_SINGLE,
    INLINE_1,
    INLINE_2,
    INLINE_3,
    INLINE_4,
    ARROW,
    CONTENT,
    CONTENT_SLOT,
    HEADING_SLOT,
}

/** How much of the row's inner width the collapsed end column may take before the title shrinks. */
private const val END_CLUSTER_MAX_WIDTH_FRACTION = 0.6

internal val ExpandableCardShape = RoundedCornerShape(24.dp)

private val InlineIds = listOf(
    ExpandableCardLayoutId.INLINE_1,
    ExpandableCardLayoutId.INLINE_2,
    ExpandableCardLayoutId.INLINE_3,
    ExpandableCardLayoutId.INLINE_4,
)

/**
 * Card container shared by the design system's expandable rows.
 *
 * A single custom [Layout] owns both states instead of swapping two layouts, because the avatar and
 * the title travel continuously between them: each child is placed at the point interpolated by
 * [ExpansionMotion.expansion] between its collapsed and its expanded anchor, and the card height is
 * interpolated the same way. Children that belong to one state only are still placed on every frame
 * — they are hidden by their own alpha, not by being absent, so their position keeps tracking the
 * ones that move.
 *
 * @param contentGap Space between the start and end columns of the collapsed row. The account row
 * uses 12dp, the expandable row 8dp.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@Composable
internal fun ExpandableCard(
    motion: ExpansionMotion,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    contentGap: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isInteractive = onClick != null || onLongClick != null

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(ExpandableCardShape)
        .background(TangemTheme.colors3.bg.secondary)
        .conditionalCompose(isFocused) {
            border(
                width = 2.dp,
                color = TangemTheme.colors3.interaction.focusRing.brand,
                shape = ExpandableCardShape,
            )
        }
        .conditionalCompose(isInteractive) {
            combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick ?: {},
            )
        }

    Layout(content = content, modifier = cardModifier) { measurables, constraints ->
        val progress = motion.expansion
        val padding = 16.dp.roundToPx()
        val avatarGap = 8.dp.roundToPx()
        val columnGap = contentGap.roundToPx()
        val inlineGap = 2.dp.roundToPx()
        val avatarCollapsed = 40.dp.roundToPx()
        val avatarExpanded = 16.dp.roundToPx()
        val expandedLineHeight = 20.dp.roundToPx()
        val expandedBottomPadding = 8.dp.roundToPx()

        val width = constraints.maxWidth
        val innerWidth = max(0, width - padding * 2)
        val loose = Constraints(maxWidth = width)

        val avatar = measurables.measureById(ExpandableCardLayoutId.AVATAR, loose)
        val arrow = measurables.measureById(ExpandableCardLayoutId.ARROW, loose)

        val endMaxWidth = (innerWidth * END_CLUSTER_MAX_WIDTH_FRACTION).toInt()
        val endConstraints = Constraints(maxWidth = endMaxWidth)
        val inline = InlineIds.mapNotNull { measurables.measureById(it, endConstraints) }
        val endTop = measurables.measureById(ExpandableCardLayoutId.END_TOP, endConstraints)
        val endBottom = measurables.measureById(ExpandableCardLayoutId.END_BOTTOM, endConstraints)
        val endSingle = measurables.measureById(ExpandableCardLayoutId.END_SINGLE, endConstraints)

        val endWidth = maxOf(endSingle.widthOrZero(), endTop.widthOrZero(), endBottom.widthOrZero())
        val startWidthCollapsed = max(0, innerWidth - avatarCollapsed - avatarGap - columnGap - endWidth)
        val inlineWidth = inline.sumOf { it.width + inlineGap }
        val titleWidthExpanded = max(
            0,
            innerWidth - avatarExpanded - avatarGap - arrow.widthOrZero() - avatarGap - inlineWidth,
        )
        val titleScale = motion.titleScale
        val titleBudgetExpanded = (titleWidthExpanded / ROW_TITLE_EXPANDED_SCALE).roundToInt()
        val titleMaxWidth = if (motion.isTitleCaption) {
            titleWidthExpanded
        } else {
            lerp(startWidthCollapsed, titleBudgetExpanded, progress)
        }

        val title = measurables.measureById(
            id = ExpandableCardLayoutId.TITLE,
            constraints = Constraints(maxWidth = max(0, titleMaxWidth)),
        )
        val subtitle = measurables.measureById(
            id = ExpandableCardLayoutId.SUBTITLE,
            constraints = Constraints(maxWidth = startWidthCollapsed),
        )
        val titleAnchor = measurables.measureById(
            id = ExpandableCardLayoutId.TITLE_ANCHOR,
            constraints = Constraints(maxWidth = max(0, titleWidthExpanded)),
        )
        val contentSlot = measurables.measureById(
            id = ExpandableCardLayoutId.CONTENT_SLOT,
            constraints = Constraints(maxWidth = max(0, innerWidth - avatarCollapsed - avatarGap)),
        )
        val headingSlot = measurables.measureById(
            id = ExpandableCardLayoutId.HEADING_SLOT,
            constraints = Constraints(maxWidth = max(0, titleWidthExpanded)),
        )
        val slot = measurables.measureById(
            id = ExpandableCardLayoutId.CONTENT,
            constraints = Constraints(minWidth = width, maxWidth = width),
        )

        val structuredStartHeight = title.heightOrZero() +
            if (subtitle != null) inlineGap + subtitle.height else 0
        val startClusterHeight = contentSlot?.height ?: structuredStartHeight
        val endClusterHeight = when {
            contentSlot != null -> 0
            endSingle != null -> endSingle.height
            endTop != null && endBottom != null -> endTop.height + inlineGap + endBottom.height
            else -> max(endTop.heightOrZero(), endBottom.heightOrZero())
        }
        val mainHeight = maxOf(avatarCollapsed, startClusterHeight, endClusterHeight)
        val collapsedHeight = mainHeight + padding * 2

        val lineHeight = maxOf(
            expandedLineHeight,
            arrow.heightOrZero(),
            headingSlot.heightOrZero(),
            (title.heightOrZero() * titleScale).roundToInt(),
        )
        val expandedHeadingHeight = padding + lineHeight + expandedBottomPadding
        val expandedHeight = expandedHeadingHeight + slot.heightOrZero()

        val height = lerp(collapsedHeight, expandedHeight, progress)
        val headingBottom = lerp(collapsedHeight, expandedHeadingHeight, progress)

        layout(width = width, height = max(0, height)) {
            val avatarCenterY = lerp(padding + mainHeight / 2, padding + lineHeight / 2, progress)
            avatar?.placeRelative(x = padding, y = avatarCenterY - avatar.height / 2)

            val titleX = lerp(
                padding + avatarCollapsed + avatarGap,
                padding + avatarExpanded + avatarGap,
                progress,
            )
            val startClusterTop = padding + (mainHeight - startClusterHeight) / 2
            val titleCenterY = lerp(
                startClusterTop + title.heightOrZero() / 2,
                padding + lineHeight / 2,
                progress,
            )
            val titleY = titleCenterY - title.heightOrZero() / 2
            title?.placeRelative(x = titleX, y = titleY)

            subtitle?.placeRelative(x = titleX, y = titleY + title.heightOrZero() + inlineGap)

            contentSlot?.placeRelative(x = titleX, y = startClusterTop)
            headingSlot?.placeRelative(x = titleX, y = titleCenterY - headingSlot.height / 2)

            val endRight = width - padding
            val endClusterTop = padding + (mainHeight - endClusterHeight) / 2
            endSingle?.placeRelative(x = endRight - endSingle.width, y = endClusterTop)
            endTop?.placeRelative(x = endRight - endTop.width, y = endClusterTop)
            endBottom?.placeRelative(
                x = endRight - endBottom.width,
                y = if (endTop != null) endClusterTop + endTop.height + inlineGap else endClusterTop,
            )

            val titleRight = lerp(
                padding + avatarCollapsed + avatarGap + title.widthOrZero(),
                padding + avatarExpanded + avatarGap + titleAnchor.widthOrZero(),
                progress,
            )
            var inlineX = titleRight + inlineGap
            inline.forEach { placeable ->
                placeable.placeRelative(x = inlineX, y = titleCenterY - placeable.height / 2)
                inlineX += placeable.width + inlineGap
            }

            arrow?.placeRelative(
                x = endRight - arrow.width,
                y = lerp(
                    padding + (mainHeight - arrow.height) / 2,
                    padding + (lineHeight - arrow.height) / 2,
                    progress,
                ),
            )

            slot?.placeRelative(x = 0, y = headingBottom)
        }
    }
}

private fun List<Measurable>.measureById(id: ExpandableCardLayoutId, constraints: Constraints): Placeable? =
    firstOrNull { it.layoutId == id }?.measure(constraints)

internal fun Placeable?.widthOrZero(): Int = this?.width ?: 0

internal fun Placeable?.heightOrZero(): Int = this?.height ?: 0