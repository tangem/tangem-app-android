package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
class LinkedCardContextInterceptor(
    private val contextInterceptor: CardContextInterceptor,
    val parent: ParamsInterceptor? = null,
) : ParamsInterceptor {

    override fun id(): String = LinkedCardContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = contextInterceptor.canBeAppliedTo(event)

    override fun intercept(params: MutableMap<String, String>) = contextInterceptor.intercept(params)

    companion object {
        fun id(): String = LinkedCardContextInterceptor::class.java.simpleName
    }
}