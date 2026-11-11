package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface TangemPayOnrampFeesUM {

    data object Loading : TangemPayOnrampFeesUM

    data class Content(
        val rows: ImmutableList<FeeRow>,
        val notes: ImmutableList<TextReference> = persistentListOf(),
    ) : TangemPayOnrampFeesUM

    data class Error(val onRetryClick: () -> Unit) : TangemPayOnrampFeesUM

    @Immutable
    data class FeeRow(
        val type: String,
        val title: TextReference,
        val value: String,
    )
}

internal val TangemPayOnrampFeesUM.isEmpty: Boolean
    get() = this is TangemPayOnrampFeesUM.Content && rows.isEmpty() && notes.isEmpty()