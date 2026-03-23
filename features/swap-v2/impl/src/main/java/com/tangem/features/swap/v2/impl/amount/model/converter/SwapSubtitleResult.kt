package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference

internal data class SwapSubtitleResult(
    val subtitleLeft: TextReference,
    val subtitleRight: TextReference,
    val subtitleEllipsisLeft: TextEllipsis,
    val subtitleEllipsisRight: TextEllipsis,
)