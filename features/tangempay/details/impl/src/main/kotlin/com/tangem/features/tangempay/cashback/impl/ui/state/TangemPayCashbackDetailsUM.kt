package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayCashbackDetailsUM(
    val title: TextReference,
    val rows: ImmutableList<TextReference>,
)