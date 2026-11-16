package com.tangem.features.allservices.impl.entries

import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * One row of the All Services screen.
 *
 * An entry owns everything about itself: the data it loads, whether it is shown ([isVisible]), how it
 * draws ([Content]) and where a click leads. The screen only orders entries into sections and knows
 * nothing about what any of them shows.
 */
internal interface AllServicesEntry : ComposableContentComponent {

    /** Stable identity, used both for the entry's child context and for the list's item key. */
    val key: String

    /**
     * Whether the row is on screen. Read synchronously on the first frame and on every change, so it

     * corrects later.
     */
    val isVisible: StateFlow<Boolean>
}