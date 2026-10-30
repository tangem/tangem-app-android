package com.tangem.feature.swap.mcp.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface ExchangeIntentUM {

    val primaryButton: ExchangeIntentButtonUM

    /** What the agent asks for, to be accepted. */
    data class Offer(
        val fromToken: TangemTokenIcon.UiState,
        val toToken: TangemTokenIcon.UiState,
        val title: TextReference,
        val agent: ExchangeIntentAgentUM,
        val description: TextReference,
        override val primaryButton: ExchangeIntentButtonUM,
    ) : ExchangeIntentUM

    /** An intent that cannot be offered: it has already been processed, or it cannot be reached. */
    data class Status(
        val variant: TangemModalStatus.Variant,
        val title: TextReference,
        val subtitle: TextReference?,
        override val primaryButton: ExchangeIntentButtonUM,
    ) : ExchangeIntentUM
}

@Immutable
internal sealed interface ExchangeIntentAgentUM {

    val iconUrl: String?
    val label: TextReference

    /** An agent whose host is verified; [label] is that host. */
    data class Verified(override val iconUrl: String?, override val label: TextReference) : ExchangeIntentAgentUM

    /** An agent without a verified host; [label] is a generic name, not a host identity. */
    data class Unknown(override val iconUrl: String?, override val label: TextReference) : ExchangeIntentAgentUM
}

@Immutable
internal data class ExchangeIntentButtonUM(
    val text: TextReference,
    val onClick: () -> Unit,
)