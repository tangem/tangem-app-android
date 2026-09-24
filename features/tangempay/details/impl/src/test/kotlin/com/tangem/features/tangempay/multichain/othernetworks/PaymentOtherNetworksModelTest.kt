package com.tangem.features.tangempay.multichain.othernetworks

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class PaymentOtherNetworksModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @Test
    fun `WHEN model created THEN the 1 to 1 swap popup showed event is sent`() {
        // Act
        createModel()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Multichain.OtherWaySwapPopupShowed()) }
    }

    private fun createModel(): PaymentOtherNetworksModel {
        return PaymentOtherNetworksModel(
            paramsContainer = MutableParamsContainer(PaymentOtherNetworksComponent.Params(onDismiss = {})),
            analytics = analytics,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }
}