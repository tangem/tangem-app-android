package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.SignIn
import com.tangem.domain.card.analytics.IntroductionProcess

class HotWalletContextInterceptor(
    val parent: ParamsInterceptor? = null,
) : ParamsInterceptor {

    override fun id(): String = HotWalletContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean {
        return when (event) {
            is SignIn.ScreenOpened,
            is SignIn.ButtonAddWallet,
            is SignIn.ButtonUnlockAllWithBiometric,
            is IntroductionProcess.ButtonScanCard,
            -> false
            is SignIn.ErrorBiometricUpdated -> !event.isFromUnlockAll
            else -> true
        }
    }

    override fun intercept(params: MutableMap<String, String>) {
        params[AnalyticsParam.PRODUCT_TYPE] = AnalyticsParam.ProductType.MobileWallet.value
        params.remove(AnalyticsParam.BATCH)
        params.remove(AnalyticsParam.FIRMWARE)
        params.remove(AnalyticsParam.CURRENCY)
    }

    companion object {
        fun id(): String = HotWalletContextInterceptor::class.java.simpleName
    }
}