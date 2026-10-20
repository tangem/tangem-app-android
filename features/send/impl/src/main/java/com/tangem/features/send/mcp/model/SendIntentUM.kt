package com.tangem.features.send.mcp.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface SendIntentUM {

    data object Loading : SendIntentUM

    data class Offer(
        val token: TangemTokenIcon.UiState,
        val title: TextReference,
        val agent: SendIntentAgentUM,
        val description: TextReference,
        val primaryButton: SendIntentButtonUM,
    ) : SendIntentUM
}

@Immutable
internal sealed interface SendIntentAgentUM {

    val iconUrl: String?
    val label: TextReference

    /** An agent whose host is verified; [label] is that host. */
    data class Verified(override val iconUrl: String?, override val label: TextReference) : SendIntentAgentUM

    /** An agent without a verified host; [label] is a generic name, not a host identity. */
    data class Unknown(override val iconUrl: String?, override val label: TextReference) : SendIntentAgentUM
}

@Immutable
internal data class SendIntentButtonUM(
    val text: TextReference,
    val onClick: () -> Unit,
)