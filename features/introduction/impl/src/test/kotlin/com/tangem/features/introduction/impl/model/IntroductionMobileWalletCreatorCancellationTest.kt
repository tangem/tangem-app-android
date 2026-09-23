package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionMobileWalletCreatorCancellationTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase = mockk()
    private val appCoroutineScope: AppCoroutineScope = mockk()

    private val createHotWalletUseCase = CreateHotWalletUseCase(
        tangemHotSdk = tangemHotSdk,
        hotUserWalletBuilderFactory = hotUserWalletBuilderFactory,
        saveWalletUseCase = saveWalletUseCase,
        syncWalletWithRemoteUseCase = syncWalletWithRemoteUseCase,
        appCoroutineScope = appCoroutineScope,
    )

    private val appsFlyerRepository: AppsFlyerRepository = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val onFailed: () -> Unit = mockk(relaxed = true)

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val creator = IntroductionMobileWalletCreator(
        createHotWalletUseCase = createHotWalletUseCase,
        referral = IntroductionReferral(appsFlyerRepository),
        analyticsEventHandler = analyticsEventHandler,
        router = router,
        messageSender = messageSender,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemHotSdk, appsFlyerRepository, analyticsEventHandler, router, messageSender, onFailed)
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        coEvery { appsFlyerRepository.getConversionData() } returns null
    }

    @Test
    fun `GIVEN the creation is suspended WHEN the launching scope is cancelled THEN no dialog is shown`() = runTest {
        // Arrange
        coEvery { tangemHotSdk.generateWallet(any(), any()) } coAnswers { awaitCancellation() }

        // Act
        val job = launch { creator.createAndOpen(source = SOURCE, beforeOpen = {}, onFailed = onFailed) }
        runCurrent()
        job.cancelAndJoin()

        // Assert
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(HotAuth.NoAuth, MnemonicType.Words12) }
        assertThat(job.isCancelled).isTrue()
        verify { messageSender wasNot Called }
        verify { onFailed wasNot Called }
        verify { router wasNot Called }
        assertThat(sentEvents).containsExactly(
            OnboardingAnalyticsEvent.Onboarding.Started(source = SOURCE),
            OnboardingAnalyticsEvent.SeedPhrase.CreateMobileScreenOpened(source = SOURCE),
            OnboardingAnalyticsEvent.CreateWallet.ButtonCreateWallet(),
        ).inOrder()
    }

    @Test
    fun `GIVEN the real use case fails with a plain exception WHEN invoked THEN the dialog is shown`() = runTest {
        // Arrange
        coEvery { tangemHotSdk.generateWallet(any(), any()) } throws IllegalStateException("boom")

        // Act
        creator.createAndOpen(source = SOURCE, beforeOpen = {}, onFailed = onFailed)

        // Assert
        verify(exactly = 1) { messageSender.send(any<DialogMessage>()) }
        verify(exactly = 1) { onFailed() }
        verify { router wasNot Called }
        assertThat(sentEvents).containsExactly(
            OnboardingAnalyticsEvent.Onboarding.Started(source = SOURCE),
            OnboardingAnalyticsEvent.SeedPhrase.CreateMobileScreenOpened(source = SOURCE),
            OnboardingAnalyticsEvent.CreateWallet.ButtonCreateWallet(),
        ).inOrder()
    }

    private companion object {
        val SOURCE = AnalyticsParam.ScreensSources.CreateWalletIntro
    }
}