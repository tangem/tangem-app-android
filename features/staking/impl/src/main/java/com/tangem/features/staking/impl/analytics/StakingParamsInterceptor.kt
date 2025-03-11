package com.tangem.features.staking.impl.analytics

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent

class StakingParamsInterceptor(private val tokenSymbol: String) : ParamsInterceptor {

    override fun id() = ID

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean {
        return event is StakingAnalyticsEvent
    }

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.TOKEN_PARAM] = tokenSymbol
    }

    companion object {
        const val ID = "StakingParamsInterceptorId"
    }
}