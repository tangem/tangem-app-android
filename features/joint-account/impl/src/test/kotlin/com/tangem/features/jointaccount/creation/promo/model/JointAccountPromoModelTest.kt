package com.tangem.features.jointaccount.creation.promo.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.creation.JointAccountCreationComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class JointAccountPromoModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @Test
    fun `GIVEN opened from main screen WHEN model created THEN intro event carries that source`() {
        // Arrange
        val source = AppRoute.JointAccountCreation.Source.MainScreen

        // Act
        JointAccountPromoModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                JointAccountCreationComponent.Params(userWalletId = UserWalletId("011"), source = source),
            ),
            router = mockk(relaxed = true),
            analyticsEventHandler = analyticsEventHandler,
        )

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(event = JointAccountAnalyticsEvent.IntroScreenOpened(source = source))
        }
    }
}