package com.tangem.tap.features.demo

import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
interface DemoMiddleware {
    fun tryHandle(config: DemoConfig, scanResponse: ScanResponse, action: Action): Boolean
}