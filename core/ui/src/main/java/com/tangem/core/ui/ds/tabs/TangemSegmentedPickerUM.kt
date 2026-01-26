package com.tangem.core.ui.ds.tabs

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * Model for Tangem segmented picker component.
 *
 * @param items                   List of TangemSegmentUM representing the segments in the picker.
 * @param initialSelectedItem     Optional TangemSegmentUM representing the initially selected segment.
 * @param hasSeparator            Boolean indicating whether there is a separator between segments.
 * @param isFixed                 Boolean indicating whether the picker has a fixed width.
 * @param isAltSurface            Boolean indicating whether to use an alternative surface style.
 */
data class TangemSegmentedPickerUM(
    val items: ImmutableList<TangemSegmentUM>,
    val initialSelectedItem: TangemSegmentUM? = null,
    val hasSeparator: Boolean = false,
    val isFixed: Boolean = false,
    val isAltSurface: Boolean = false,
)

/**
 * Model for a single segment in the Tangem segmented picker.
 *
 * @param id        String representing the unique identifier of the segment.
 * @param title     TextReference representing the title of the segment.
 */
data class TangemSegmentUM(
    val id: String,
    val title: TextReference,
)