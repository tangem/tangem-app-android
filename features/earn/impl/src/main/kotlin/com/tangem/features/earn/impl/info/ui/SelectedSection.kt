package com.tangem.features.earn.impl.info.ui

import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab

internal const val ABOVE_VIEWPORT = Int.MIN_VALUE
internal const val BELOW_VIEWPORT = Int.MAX_VALUE

private const val REFERENCE_LINE_SHARE = 3

/**
 * The last section whose top has reached [referenceLine], else the first one; [sectionTops] is ordered and not empty.
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
 * A third of the space under the chips: unlike the edge right under them, a short tail can still reach it.
 * [viewportHeight] is the bottom of the visible content in item offsets, see [visibleListHeight].
 */
internal fun referenceLine(chipsBottom: Int, viewportHeight: Int): Int =
    chipsBottom + (viewportHeight - chipsBottom) / REFERENCE_LINE_SHARE

/** Item offsets start under the header (the top content padding), so the footer's top edge is at this offset. */
internal fun visibleListHeight(viewportHeight: Int, beforeContentPadding: Int, afterContentPadding: Int): Int =
    viewportHeight - beforeContentPadding - afterContentPadding

/** Pinned at offset 0 (the header bottom) or above; chips not laid out are pinned once the list starts after them. */
internal fun areChipsPinned(chipsOffset: Int?, chipsIndex: Int, firstVisibleIndex: Int): Boolean =
    if (chipsOffset != null) chipsOffset <= 0 else firstVisibleIndex > chipsIndex

/** Over its list item, under the header once pinned, out of sight at [viewportHeight] while the item is below. */
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
 * The top of every section in item offsets; a section that is not laid out gets [ABOVE_VIEWPORT] when the list starts
 * after it and [BELOW_VIEWPORT] otherwise.
 */
internal fun sectionTops(
    sectionIndices: Map<EarnInfoTab, Int>,
    visibleOffsets: Map<Int, Int>,
    firstVisibleIndex: Int,
): Map<EarnInfoTab, Int> = sectionIndices.mapValues { (_, index) ->
    visibleOffsets[index] ?: if (index < firstVisibleIndex) ABOVE_VIEWPORT else BELOW_VIEWPORT
}