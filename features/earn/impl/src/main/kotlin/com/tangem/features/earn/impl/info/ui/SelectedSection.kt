package com.tangem.features.earn.impl.info.ui

import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab

internal const val ABOVE_VIEWPORT = Int.MIN_VALUE
internal const val BELOW_VIEWPORT = Int.MAX_VALUE

private const val REFERENCE_LINE_SHARE = 3

/**
 * The last section whose top has reached [referenceLine], the first one while the page is above all of them.
 * [sectionTops] is ordered by position and not empty.
 *
 * A short last section can never reach the line, so the end of the page selects it.
 */
internal fun selectedSection(sectionTops: Map<EarnInfoTab, Int>, referenceLine: Int, isAtEnd: Boolean): EarnInfoTab {
    if (isAtEnd) return sectionTops.keys.last()

    return sectionTops.entries
        .lastOrNull { (_, top) -> top <= referenceLine }
        ?.key
        ?: sectionTops.keys.first()
}

/**
 * The line a section has to reach to count as the current one: a third of the space the stuck chips leave, measured
 * from the chips. The top edge under the chips is out of reach of a short tail, the upper third is not.
 *
 * @param viewportHeight the bottom of the space the list shows content in, in item offsets, see [visibleListHeight]
 */
internal fun referenceLine(chipsBottom: Int, viewportHeight: Int): Int =
    chipsBottom + (viewportHeight - chipsBottom) / REFERENCE_LINE_SHARE

/**
 * The list runs under the header and the footer, its content paddings. Item offsets start at the header's bottom edge,
 * so the content shows from offset 0 to the footer's top edge, which is this height.
 */
internal fun visibleListHeight(viewportHeight: Int, beforeContentPadding: Int, afterContentPadding: Int): Int =
    viewportHeight - beforeContentPadding - afterContentPadding

/**
 * Whether the chips have reached the header's bottom edge, offset 0, or are scrolled under it. Chips that are not laid
 * out have reached it once the list starts after them.
 */
internal fun areChipsPinned(chipsOffset: Int?, chipsIndex: Int, firstVisibleIndex: Int): Boolean =
    if (chipsOffset != null) chipsOffset <= 0 else firstVisibleIndex > chipsIndex

/**
 * Where the chip row floating over the list goes, from the top of the list: over its item while that is below the
 * header, under the header once pinned, and at [viewportHeight], out of sight, while the item is below the list.
 */
internal fun chipsRowTop(
    chipsOffset: Int?,
    chipsIndex: Int,
    firstVisibleIndex: Int,
    navigationHeight: Int,
    viewportHeight: Int,
): Int = when {
    areChipsPinned(chipsOffset = chipsOffset, chipsIndex = chipsIndex, firstVisibleIndex = firstVisibleIndex) ->
        navigationHeight
    chipsOffset != null -> navigationHeight + chipsOffset
    else -> viewportHeight
}

/**
 * The top edge of every section in the list coordinates. A section that is not laid out is above the viewport when
 * the list starts after it ([firstVisibleIndex]), below it otherwise, and [ABOVE_VIEWPORT] / [BELOW_VIEWPORT] put it
 * on that side of any line. [visibleOffsets] maps the index of a laid out item to its offset.
 */
internal fun sectionTops(
    sectionIndices: Map<EarnInfoTab, Int>,
    visibleOffsets: Map<Int, Int>,
    firstVisibleIndex: Int,
): Map<EarnInfoTab, Int> = sectionIndices.mapValues { (_, index) ->
    visibleOffsets[index] ?: if (index < firstVisibleIndex) ABOVE_VIEWPORT else BELOW_VIEWPORT
}