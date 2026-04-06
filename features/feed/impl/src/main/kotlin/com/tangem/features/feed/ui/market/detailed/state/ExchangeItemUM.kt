package com.tangem.features.feed.ui.market.detailed.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface ExchangeItemUM {

    val id: String

    data class Content(
        override val id: String,
        val title: TextReference,
        val subTitle: TextReference,
        val icon: TangemIconUM,
        val volumeInUsd: TextReference,
        val auditLabel: AuditLabelUM,
    ) : ExchangeItemUM

    data class Loading(override val id: String) : ExchangeItemUM
}