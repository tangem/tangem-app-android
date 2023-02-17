package com.tangem.tap.common.extensions

import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor

/**
[REDACTED_AUTHOR]
 */
fun Analytics.addCardContext(scanResponse: ScanResponse) {
    addParamsInterceptor(CardContextInterceptor(scanResponse))
}

fun Analytics.eraseCardContext() {
    removeParamsInterceptor(CardContextInterceptor.id())
}