package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TangemPayCashbackAccrualsUM(
    val title: TextReference,
    val infoRows: ImmutableList<InfoRow>,
    val docRows: ImmutableList<DocRow>,
) {

    @Immutable
    data class InfoRow(
        val title: TextReference,
        val description: TextReference,
    )

    @Immutable
    data class DocRow(
        val title: TextReference,
        val onClick: () -> Unit,
    )
}