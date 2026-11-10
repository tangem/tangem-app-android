package com.tangem.features.earn.impl.info.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab

/** Where a list rests: its first visible item and how far that item is scrolled. */
internal data class ListPosition(val index: Int, val scrollOffset: Int)

/**
 * The chip the user tapped. A short tail cannot scroll its section to the chips, so the scroll alone would pick another
 * chip: the tapped one stays [tab] until the list rests somewhere else than where the tap's scroll left it.
 *
 * The scroll of the tap is not a user scroll: positions reported before [onScrollEnded] are ignored. A drag that
 * moves nothing (the list is at its end) keeps the chip. A scroll interrupted by the user releases it.
 * A newer tap replaces the older one, and reports of the older tap's scroll are told apart by the tap id.
 */
internal class ClickedSection {

    var tab: EarnInfoTab? by mutableStateOf(null)
        private set

    private var lastTapId = 0
    private var restedAt: ListPosition? = null

    /** @return the id of the tap, the scroll it started reports back with it */
    fun onTap(tab: EarnInfoTab): Int {
        this.tab = tab
        restedAt = null
        lastTapId += 1
        return lastTapId
    }

    fun onScrollEnded(tapId: Int, position: ListPosition) {
        if (tapId == lastTapId) restedAt = position
    }

    fun onScrollInterrupted(tapId: Int) {
        if (tapId == lastTapId) release()
    }

    fun onPositionChanged(position: ListPosition) {
        val rest = restedAt ?: return
        if (position != rest) release()
    }

    private fun release() {
        tab = null
        restedAt = null
    }
}