package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.models.scan.ScanResponse

/**
[REDACTED_AUTHOR]
 */
class LinkedCardContextInterceptor(
    scanResponse: ScanResponse,
    cardTypeResolver: CardTypeResolver,
    val parent: LinkedCardContextInterceptor? = null,
) : ParamsInterceptor {

    private val contextInterceptor = CardContextInterceptor(scanResponse, cardTypeResolver)

    override fun id(): String = LinkedCardContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = contextInterceptor.canBeAppliedTo(event)

    override fun intercept(params: MutableMap<String, String>) = contextInterceptor.intercept(params)

    companion object {
        fun id(): String = LinkedCardContextInterceptor::class.java.simpleName
    }
}