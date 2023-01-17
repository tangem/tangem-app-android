package com.tangem.tap.domain.scanCard.chains

import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.analytics.paramsInterceptor.BatchIdParamsInterceptor
import com.tangem.tap.common.successOr
import com.tangem.tap.store

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
class AnalyticsChain(
    private val cardScannedEvent: AnalyticsEvent?,
) : ScanCardChain {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }

        Analytics.addParamsInterceptor(BatchIdParamsInterceptor(scanResponse.card.batchId))
        cardScannedEvent?.let { Analytics.send(it) }

        return data
    }
}

class UpdateConfigManagerChain : ScanCardChain {
    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }

        store.state.globalState.tapWalletManager.updateConfigManager(scanResponse)

        return data
    }
}
