package com.tangem.tap.features.demo

import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.scan.ScanResponse

object DemoHelper {
    val config = DemoConfig

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isTestDemoCard(scanResponse: ScanResponse): Boolean = config.isTestDemoCardId(scanResponse.card.cardId)

    fun isDemoCardId(cardId: String): Boolean = config.isDemoCardId(cardId)
}