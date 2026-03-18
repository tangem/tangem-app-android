package com.tangem.feature.wallet.child.organizetokens.entity

import androidx.compose.runtime.Immutable

/**
 * Rounding mode of the [DraggableItem]
 *
 * @property isShowGap if true then item should have padding on rounded side
 * */
@Immutable
internal sealed class RoundingModeUM {
    abstract val isShowGap: Boolean

    /**
     * In this mode, item is not rounded
     * */
    object None : RoundingModeUM() {
        override val isShowGap: Boolean = false
    }

    /**
     * In this mode, item should have a rounded top side
     *
     * @property isShowGap if true then item should have top padding
     * */
    data class Top(override val isShowGap: Boolean = false) : RoundingModeUM()

    /**
     * In this mode, item should have a rounded bottom side
     *
     * @property isShowGap if true then item should have bottom padding
     * */
    data class Bottom(override val isShowGap: Boolean = false) : RoundingModeUM()

    /**
     * In this mode, item should have a rounded all sides
     *
     * @property isShowGap if true then item should have top and bottom padding
     * */
    data class All(override val isShowGap: Boolean = false) : RoundingModeUM()
}