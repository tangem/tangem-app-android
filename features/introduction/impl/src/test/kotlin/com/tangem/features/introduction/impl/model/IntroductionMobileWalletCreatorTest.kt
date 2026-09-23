package com.tangem.features.introduction.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionMobileWalletCreatorTest {

    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
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
        clearMocks(createHotWalletUseCase, appsFlyerRepository, analyticsEventHandler, router, messageSender, onFailed)
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        coEvery { appsFlyerRepository.getConversionData() } returns null
    }

    @Test
    fun `GIVEN the wallet is created WHEN invoked THEN the funnel is reported and the wallet opens`() = runTest {
        // Arrange
        val wallet: UserWallet.Hot = mockk()
        coEvery {
            createHotWalletUseCase(auth = HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
        } returns wallet.right()

        // Act
        creator.createAndOpen(source = SOURCE, beforeOpen = {}, onFailed = onFailed)

        // Assert
        assertThat(sentEvents).containsExactly(
            OnboardingAnalyticsEvent.Onboarding.Started(source = SOURCE),
            OnboardingAnalyticsEvent.SeedPhrase.CreateMobileScreenOpened(source = SOURCE),
            OnboardingAnalyticsEvent.CreateWallet.ButtonCreateWallet(),
            OnboardingAnalyticsEvent.Onboarding.Finished(source = SOURCE),
            walletCreated(referralId = null),
        ).inOrder()
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        verify { messageSender wasNot Called }
        verify { onFailed wasNot Called }
    }

    @Test
    fun `GIVEN the wallet is created WHEN invoked THEN the screen settles before the stack swaps`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns mockk<UserWallet.Hot>().right()
        val order = mutableListOf<String>()
        every { router.replaceAll(AppRoute.Wallet) } answers { order += "open" }

        // Act
        creator.createAndOpen(source = SOURCE, beforeOpen = { order += "settle" }, onFailed = onFailed)

        // Assert
        assertThat(order).containsExactly("settle", "open").inOrder()
    }

    @Test
    fun `GIVEN the creation fails WHEN invoked THEN the viewer is told and the caller notified`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns IllegalStateException("boom").left()

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

    @Test
    fun `GIVEN a referral WHEN the wallet is created THEN the referral id is reported`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns mockk<UserWallet.Hot>().right()
        coEvery {
            appsFlyerRepository.getConversionData()
        } returns AppsFlyerConversionData(refcode = "ref-42", campaign = null)

        // Act
        creator.createAndOpen(source = SOURCE, beforeOpen = {}, onFailed = onFailed)

        // Assert
        assertThat(sentEvents.last()).isEqualTo(walletCreated(referralId = "ref-42"))
    }

    @Test
    fun `GIVEN the referral store fails WHEN the wallet is created THEN the wallet still opens`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns mockk<UserWallet.Hot>().right()
        coEvery { appsFlyerRepository.getConversionData() } throws IllegalStateException("corrupt store")

        // Act
        creator.createAndOpen(source = SOURCE, beforeOpen = {}, onFailed = onFailed)

        // Assert
        assertThat(sentEvents.last()).isEqualTo(walletCreated(referralId = null))
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        verify { onFailed wasNot Called }
    }

    private fun walletCreated(referralId: String?) = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
        source = SOURCE,
        creationType = AnalyticsParam.WalletCreationType.NewSeed,
        seedPhraseLength = 12,
        passPhraseState = AnalyticsParam.EmptyFull.Empty,
        referralId = referralId,
    )

    private companion object {
        val SOURCE = AnalyticsParam.ScreensSources.CreateWalletIntro
    }
}