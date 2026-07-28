package com.tangem.core.ui.ds2.filter

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * State of a single [TangemFilterItem] inside a [TangemFilterGroup].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/branch/0xt9Tg8x8f0Z0m2KUdLG9q/%F0%9F%92%A0-DS-Components?node-id=7385-2463&m=dev)
 *
 * The subtype selects both the content and the trailing icon of the chip:
 * [Inactive] shows a label with a chevron, [Active] shows the picked value with a clear cross, and
 * [Loading] renders a shimmer placeholder.
 */
@Immutable
sealed interface TangemFilterItemUM {

    /** Stable identity of the filter, used as the list key inside [TangemFilterGroup]. */
    val id: String

    /**
     * Filter with nothing picked yet: shows [label] and a chevron hinting that a list of options
     * opens on click.
     *
     * @param id Stable identity of the filter.
     * @param label Name of the filter (e.g. `"Network"`).
     * @param onClick Invoked when the chip is clicked — open the list of options here.
     */
    @Immutable
    data class Inactive(
        override val id: String,
        val label: TextReference,
        val onClick: () -> Unit,
    ) : TangemFilterItemUM

    /**
     * Filter with a picked value: shows [value], an optional `+N` counter, and a clear cross.
     *
     * @param id Stable identity of the filter.
     * @param value Picked value shown instead of the filter name (e.g. `"Ethereum"`).
     * @param counter Number of picked values beyond [value], rendered as `+N`. `null` hides the
     *   counter.
     * @param onClick Invoked when the chip is clicked — open the list of options here.
     * @param onClearClick Invoked when the trailing cross is clicked — drop the picked values and
     *   move the filter back to [Inactive] here.
     */
    @Immutable
    data class Active(
        override val id: String,
        val value: TextReference,
        val counter: Int? = null,
        val onClick: () -> Unit,
        val onClearClick: () -> Unit,
    ) : TangemFilterItemUM

    /**
     * Filter whose options are still being loaded: renders a fixed-width shimmer instead of
     * content and ignores clicks.
     *
     * @param id Stable identity of the filter.
     */
    @Immutable
    data class Loading(override val id: String) : TangemFilterItemUM
}