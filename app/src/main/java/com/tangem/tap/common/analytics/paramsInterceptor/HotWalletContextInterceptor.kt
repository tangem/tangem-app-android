package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

class HotWalletContextInterceptor(
    val parent: ParamsInterceptor? = null,
) : ParamsInterceptor {

    override fun id(): String = HotWalletContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = true

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.PRODUCT_TYPE] = AnalyticsParam.ProductType.MobileWallet.value
    }

    companion object {
        fun id(): String = HotWalletContextInterceptor::class.java.simpleName
    }
}