package com.tangem.features.foryou.impl.tokensummary.entity

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class TokenSummaryHeaderUM(
    val tangemIconUM: TangemIconUM,
    val title: TextReference,
    val subtitle: TextReference?,
)