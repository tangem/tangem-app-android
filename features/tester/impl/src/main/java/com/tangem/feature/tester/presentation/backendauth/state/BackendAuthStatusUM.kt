package com.tangem.feature.tester.presentation.backendauth.state

import androidx.annotation.DrawableRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Content state of the Backend Authentication status screen.
 *
 * Grouped into [Section]s: each section shows a set of status [StatusRow]s followed by the action
 * buttons that operate on them.
 *
 * @property onBackClick   invoked when back button is pressed
 * @property sections      status/action groups to display
 * @property onCopyClick   invoked with a row whose [StatusRow.copyValue] is non-null to copy the full value
 * @property runningAction label of the action currently running (shows progress, disables others)
 */
internal data class BackendAuthStatusUM(
    val onBackClick: () -> Unit = {},
    val sections: ImmutableList<Section> = persistentListOf(),
    val onCopyClick: (StatusRow) -> Unit = {},
    val runningAction: String? = null,
) {

    /** A group of related status rows and the actions that operate on them. */
    data class Section(
        val rows: ImmutableList<StatusRow>,
        val actions: ImmutableList<Action> = persistentListOf(),
    )

    /**
     * A single label → value status row.
     *
     * @property value     displayed value (may be shortened for keys/tokens)
     * @property copyValue full value to copy; when non-null a copy action is shown
     * @property subtitle  optional extra line under the value (e.g. token expiry)
     */
    data class StatusRow(
        val label: String,
        val value: String,
        val copyValue: String? = null,
        val subtitle: String? = null,
        val iconActions: ImmutableList<IconAction> = persistentListOf(),
    )

    /** A full-width action button (mutates auth state, then refreshes the panel). */
    data class Action(val label: String, val onClick: () -> Unit)

    /** A compact icon action shown inline at the end of a row (like the copy icon). */
    data class IconAction(
        val label: String,
        @DrawableRes val iconRes: Int,
        val isProgressShown: Boolean = true,
        val onClick: () -> Unit,
    )
}