package com.tangem.features.hotwallet.addexistingwallet.entry

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AddExistingWalletModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxUnitFun = true)
    private val setAccessCodeSkippedUseCase: SetAccessCodeSkippedUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val paramsContainer: ParamsContainer = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            shouldAskPermissionUseCase,
            uiMessageSender,
            trackingContextProxy,
            setAccessCodeSkippedUseCase,
            analyticsEventHandler,
            paramsContainer,
        )
    }

    @Test
    fun `GIVEN the start is not reported yet WHEN model created THEN onboarding started sent inside the context`() =
        runTest {
            // Act
            createModel(testScope = this, shouldSendOnboardingStarted = true)

            // Assert
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Started())
            }
        }

    @Test
    fun `GIVEN the start is already reported WHEN model created THEN onboarding started not sent`() = runTest {
        // Act
        createModel(testScope = this, shouldSendOnboardingStarted = false)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any<OnboardingAnalyticsEvent.Onboarding.Started>()) }
    }

    private fun createModel(testScope: TestScope, shouldSendOnboardingStarted: Boolean): AddExistingWalletModel {
        every { paramsContainer.require<AddExistingWalletComponent.Params>() } returns
            AddExistingWalletComponent.Params(shouldSendOnboardingStarted = shouldSendOnboardingStarted)

        return AddExistingWalletModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = paramsContainer,
            router = router,
            shouldAskPermissionUseCase = shouldAskPermissionUseCase,
            uiMessageSender = uiMessageSender,
            trackingContextProxy = trackingContextProxy,
            setAccessCodeSkippedUseCase = setAccessCodeSkippedUseCase,
            analyticsEventHandler = analyticsEventHandler,
        )
    }
}