package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IntroductionModelTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(urlOpener, analyticsEventHandler)
    }

    @Test
    fun `GIVEN introduction screen WHEN model created THEN screen opened event sent`() {
        // Act
        createModel()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(ofType<IntroductionProcess.ScreenOpened>()) }
    }

    @Test
    fun `GIVEN nfc launch WHEN model created THEN screen opened event still sent`() {
        // Act
        createModel(launchMode = InitScreenLaunchMode.WithCardScan)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(ofType<IntroductionProcess.ScreenOpened>()) }
    }

    @Test
    fun `GIVEN introduction screen WHEN terms of service clicked THEN document opened`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onTermsOfServiceClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/terms-of-service/") }
    }

    @Test
    fun `GIVEN introduction screen WHEN privacy policy clicked THEN document opened`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onPrivacyPolicyClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/privacy-policy/") }
    }

    private fun createModel(launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard) = IntroductionModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(IntroductionComponent.Params(launchMode)),
        urlOpener = urlOpener,
        analyticsEventHandler = analyticsEventHandler,
    )
}