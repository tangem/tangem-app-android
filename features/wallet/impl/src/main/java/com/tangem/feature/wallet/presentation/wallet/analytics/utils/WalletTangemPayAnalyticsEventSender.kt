package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import javax.inject.Inject

@ModelScoped
internal class WalletTangemPayAnalyticsEventSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    private val sentEvents = mutableSetOf<String>()

    fun send(statusValue: PaymentAccountStatusValue) {
        if (screenLifecycleProvider.isBackgroundState.value) return

        val event = when (statusValue) {
            is PaymentAccountStatusValue.IssuingCard -> TangemPayAnalyticsEvents.IssuingBannerDisplayed()
            PaymentAccountStatusValue.Empty,
            is PaymentAccountStatusValue.Error,
            is PaymentAccountStatusValue.Loaded,
            PaymentAccountStatusValue.Loading,
            PaymentAccountStatusValue.NotCreated,
            is PaymentAccountStatusValue.UnderReview,
            is PaymentAccountStatusValue.Deactivated,
            -> return
        }

        if (sentEvents.add(event.id)) {
            analyticsEventHandler.send(event)
        }
    }
}