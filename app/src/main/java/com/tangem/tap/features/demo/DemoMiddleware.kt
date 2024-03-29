package com.tangem.tap.features.demo

import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 21/02/2022.
 */
interface DemoMiddleware {
    fun tryHandle(config: DemoConfig, scanResponse: ScanResponse, action: Action): Boolean
}
