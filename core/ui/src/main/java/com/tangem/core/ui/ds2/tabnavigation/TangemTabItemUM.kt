package com.tangem.core.ui.ds2.tabnavigation

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface TangemTabItemUM {

    /** Stable identity of the tab, used as the list key in the tab row. */
    val id: String

    @Immutable
    data class Content(
        override val id: String,
        val label: TextReference,
        val counter: TextReference? = null,
        val iconStart: TangemIconUM? = null,
        val isSelected: Boolean = false,
        val onClick: () -> Unit,
    ) : TangemTabItemUM

    @Immutable
    data class Loading(override val id: String) : TangemTabItemUM
}