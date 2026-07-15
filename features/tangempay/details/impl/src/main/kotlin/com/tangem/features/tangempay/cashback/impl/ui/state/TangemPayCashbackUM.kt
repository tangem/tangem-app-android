package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class TangemPayCashbackUM(
    val title: TextReference,
    val subtitle: TextReference,
    val isEmpty: Boolean,
    val banner: Banner?,
) {

    @Immutable
    data class Banner(
        val text: TextReference,
        val type: Type,
    ) {
        enum class Type { Info, Error }
    }
}