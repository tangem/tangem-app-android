package com.tangem.features.onramp.tokenlist.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class OnrampTokenGroupUM(
    val id: String,
    val header: Header,
    val rows: ImmutableList<OnrampTokenRowUM>,
) {

    @Immutable
    sealed interface Header {

        data class Section(val title: TextReference) : Header

        data class Account(val headerRowUM: TangemHeaderRowUM) : Header
    }
}

@Immutable
internal sealed interface OnrampTokenRowUM {
    val id: String

    data class Available(val state: TangemTokenRow.State) : OnrampTokenRowUM {
        override val id: String = state.id
    }

    data class Unavailable(
        override val id: String,
        val icon: TangemTokenIcon.UiState,
        val title: TextReference,
        val reason: TextReference,
        val fiatBalance: TextReference?,
        val cryptoBalance: TextReference?,
    ) : OnrampTokenRowUM
}