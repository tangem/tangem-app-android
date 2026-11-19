package com.tangem.features.walletconnect.connections.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class McpIntentsHistoryUM(
    val agentName: String,
    val content: Content,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent {

    sealed interface Content {

        data object Loading : Content

        data class Data(val groups: ImmutableList<DayGroup>, val nextPage: NextPage) : Content
    }

    data class DayGroup(val key: String, val title: TextReference, val rows: ImmutableList<Row>)

    /** [status] and [onClick] are `null` for a row that is not an intent. */
    data class Row(
        val key: String,
        val kind: Kind,
        val time: String,
        val status: Status?,
        val onClick: (() -> Unit)?,
    )

    enum class Kind { Swap, Send, AgentConnected }

    enum class Status { New, Viewed }

    sealed interface NextPage {

        data object End : NextPage

        data object Loading : NextPage

        data class Ready(val page: Int, val onLoadMore: () -> Unit) : NextPage

        data class Error(val onRetry: () -> Unit) : NextPage
    }
}