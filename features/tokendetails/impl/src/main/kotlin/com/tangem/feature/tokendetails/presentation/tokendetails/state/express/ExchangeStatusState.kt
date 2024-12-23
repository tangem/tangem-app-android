package com.tangem.feature.tokendetails.presentation.tokendetails.state.express

import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import javax.annotation.concurrent.Immutable

@Deprecated("Use ExpressStatusBlock from common")
@Immutable
internal data class ExchangeStatusState(
    val status: ExchangeStatus,
    val text: TextReference,
    val isActive: Boolean,
    val isDone: Boolean,
)