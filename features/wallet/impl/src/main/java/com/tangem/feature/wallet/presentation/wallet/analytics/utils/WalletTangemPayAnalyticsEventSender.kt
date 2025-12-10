package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import javax.inject.Inject

@ModelScoped
internal class WalletTangemPayAnalyticsEventSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    private val sentEvents = mutableSetOf<TangemPayAnalyticsEvents>()

    fun send(customerInfo: MainScreenCustomerInfo) {
        if (screenLifecycleProvider.isBackgroundState.value) return

        val cardInfo = customerInfo.info.cardInfo
        val productInstance = customerInfo.info.productInstance

        // TODO: TangemPay refactor analytics
        // when statement copied from TangemPayUpdateInfoStateTransformer. Be careful when editing
        val event = when {
            customerInfo.orderStatus == OrderStatus.CANCELED -> return // ignore cancelled state on analytics
            !customerInfo.info.isKycApproved -> return // ignore kyc not approved state on analytics
            cardInfo != null && productInstance != null -> TangemPayAnalyticsEvents.MainScreenOpened()
            else -> TangemPayAnalyticsEvents.IssuingBannerDisplayed()
        }

        if (sentEvents.add(event)) {
            analyticsEventHandler.send(event)
        }
    }
}