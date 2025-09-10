package com.tangem.tap.features.demo

import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.redux.AppState

object DemoHelper {
    val config = DemoConfig()

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isTestDemoCard(scanResponse: ScanResponse): Boolean = config.isTestDemoCardId(scanResponse.card.cardId)

    fun isDemoCardId(cardId: String): Boolean = config.isDemoCardId(cardId)

    fun tryHandle(appState: () -> AppState?): Boolean {
        val scanResponse = getScanResponse(appState) ?: return false
        if (!scanResponse.isDemoCard()) return false

        return false
    }

    private fun getScanResponse(appState: () -> AppState?): ScanResponse? {
        return appState()?.globalState?.scanResponse
    }
}