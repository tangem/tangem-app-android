package com.tangem.features.markets.model.utils

import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import timber.log.Timber

internal fun logStatus(tag: String, status: PaginationStatus<List<TokenMarket>>) {
    Timber.tag(tag).d(
        """
        Status
        $status
        """.trimIndent(),
    )
}

internal fun logAction(tag: String, action: BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>) {
    when (action) {
        is BatchAction.Reload -> Timber.tag(tag).d(
            """
            Reload = ${action.requestParams}
            """.trimIndent(),
        )
        is BatchAction.UpdateBatches -> Timber.tag(tag).d(
            """
            To update:
            keys: ${action.keys.toList()}
            updateType: ${action.updateRequest.javaClass.simpleName}
            """.trimIndent(),
        )
        else -> Timber.tag(tag).d(
            """
            $action 
            """.trimIndent(),
        )
    }
}

internal fun logUpdateResults(
    tag: String,
    updateResult: Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>,
) {
    val sec = when (val s = updateResult.second) {
        is BatchUpdateResult.Success -> "Success"
        is BatchUpdateResult.Error -> s.throwable.toString()
    }

    Timber.tag(tag).d(
        """
        updateResults
        request: ${updateResult.first}
        result: $sec
        """.trimIndent(),
    )
}