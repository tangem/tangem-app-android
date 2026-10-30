package com.tangem.features.tangempay.hotwallet

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.media.LoopingVideoController
import com.tangem.core.media.VideoFrameSize
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.model.TangemPayOnboardingStarter
import com.tangem.features.tangempay.onboarding.impl.R
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayHotWalletOnboardingModelTest {

    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk() {
        every { getLeastVersionName() } returns "Android 10"
    }
    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk(relaxed = true)
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val video: LoopingVideoController = mockk(relaxed = true)
    private val videoFactory: LoopingVideoController.Factory = mockk {
        every { create(any(), any(), any(), any(), any()) } returns video
    }

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val testUserWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns testUserWalletId
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnLegalLinkClick {

        @BeforeEach
        fun resetMocks() {
            clearMocks(urlOpener)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN legal link WHEN clicked THEN its url opened`(testModel: LegalLinkTestModel) = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            testModel.click(model.uiState.value)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith(testModel.expectedUrl) }) }
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            LegalLinkTestModel(
                link = "terms, fees and limits",
                click = { it.onTermsClick() },
                expectedUrl = TangemPayConstants.TERMS_AND_LIMITS_URL,
            ),
            LegalLinkTestModel(
                link = "terms of service",
                click = { it.onTermsOfServiceClick() },
                expectedUrl = "https://www.paera.com/tangempay",
            ),
            LegalLinkTestModel(
                link = "privacy policy",
                click = { it.onPrivacyPolicyClick() },
                expectedUrl = "https://tangem.com/docs/en/pay-privacy-policy.pdf",
            ),
        )
    }

    data class LegalLinkTestModel(
        val link: String,
        val click: (TangemPayHotWalletOnboardingUM) -> Unit,
        val expectedUrl: String,
    )

    @Nested
    inner class OnGetCardClick {

        @Test
        fun `GIVEN hot wallet creation not supported WHEN onGetCardClick THEN wallet creation not attempted`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns false
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                verify { uiMessageSender.send(any()) }
                verify { router.replaceCurrent(AppRoute.Home()) }
                coVerify(exactly = 0) { createHotWalletUseCase(any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation succeeds WHEN onGetCardClick THEN navigate to TangemPayOnboarding`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns true
                coEvery { createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12) } returns testUserWallet.right()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                verify {
                    router.replaceAll(
                        AppRoute.TangemPayOnboarding(
                            mode = AppRoute.TangemPayOnboarding.Mode.FirstSetup(testUserWalletId),
                        ),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation fails WHEN onGetCardClick THEN error dialog sent`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns true
                coEvery {
                    createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12)
                } returns RuntimeException("error").left()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.isLoading).isFalse()
                verify { uiMessageSender.send(match<DialogMessage> { true }) }
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN wallet creation in progress WHEN onGetCardClick again THEN one wallet created`() = runTest {
            // Arrange
            every { isHotWalletCreationSupported() } returns true
            coEvery { createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12) } returns testUserWallet.right()
            val model = createModel(testScope = this)
            model.uiState.value.onGetCardClick.invoke()

            // Act
            model.uiState.value.onGetCardClick.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { createHotWalletUseCase(any(), any()) }
            verify(exactly = 1) { router.replaceAll(*anyVararg()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN wallet creation failed WHEN onGetCardClick again THEN creation retried`() = runTest {
            // Arrange
            every { isHotWalletCreationSupported() } returns true
            coEvery {
                createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12)
            } returns RuntimeException("error").left()
            val model = createModel(testScope = this)
            model.uiState.value.onGetCardClick.invoke()
            advanceUntilIdle()

            // Act
            model.uiState.value.onGetCardClick.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { createHotWalletUseCase(any(), any()) }
            model.onDestroy()
        }
    }

    @Nested
    inner class Analytics {

        @Test
        fun `GIVEN bundle screen WHEN model created THEN screen opened sent`() = runTest {
            // Act
            val model = createModel(testScope = this)

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.ShortForHwActivationScreenOpened()) }
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN bundle screen WHEN terms clicked THEN view terms sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onTermsClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.ViewTermsClicked()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN bundle screen WHEN legal links clicked THEN no event sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onTermsOfServiceClick()
            model.uiState.value.onPrivacyPolicyClick()

            // Assert
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }
    }

    @Nested
    inner class Video {

        @Test
        fun `GIVEN pay bundle introduction on WHEN model created THEN the bundle video is created`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isPayBundleIntroductionEnabled } returns true

            // Act
            val model = createModel(testScope = this)

            // Assert
            verify(exactly = 1) {
                videoFactory.create(
                    scope = any(),
                    videoRes = R.raw.tangem_pay_bundle_background,
                    frameSize = VideoFrameSize(width = 1080, height = 1920),
                    diagnosticsSubject = any(),
                    onFailed = any(),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN pay bundle introduction off WHEN model created THEN no player is created`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isPayBundleIntroductionEnabled } returns false

            // Act
            val model = createModel(testScope = this)

            // Assert
            verify(exactly = 0) { videoFactory.create(any(), any(), any(), any(), any()) }
            assertThat(model.videoState).isNull()
            model.onDestroy()
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN the bundle video WHEN the screen resumes or pauses THEN the video is told`(isRunning: Boolean) =
            runTest {
                // Arrange
                every { tangemPayFeatureToggles.isPayBundleIntroductionEnabled } returns true
                val model = createModel(testScope = this)

                // Act
                model.setVideoRunning(isRunning = isRunning)

                // Assert
                verify(exactly = 1) { video.setRunning(isRunning = isRunning) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the bundle video WHEN model destroyed THEN the video is released`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isPayBundleIntroductionEnabled } returns true
            val model = createModel(testScope = this)

            // Act
            model.onDestroy()

            // Assert
            verify(exactly = 1) { video.release() }
        }
    }

    @Nested
    inner class UnifiedOnboarding {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN money accounts toggle WHEN model created THEN unified onboarding follows it`(isEnabled: Boolean) =
            runTest {
                // Arrange
                every { moneyAccountFeatureToggles.isMoneyAccountsEnabled } returns isEnabled

                // Act
                val model = createModel(testScope = this)

                // Assert
                assertThat(model.uiState.value.isUnifiedOnboarding).isEqualTo(isEnabled)
                model.onDestroy()
            }
    }

    private fun createModel(testScope: TestScope): TangemPayHotWalletOnboardingModel {
        return TangemPayHotWalletOnboardingModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            urlOpener = urlOpener,
            onboardingStarter = TangemPayOnboardingStarter(
                router = router,
                uiMessageSender = uiMessageSender,
                repository = mockk(relaxed = true),
                produceInitialDataUseCase = mockk(relaxed = true),
                getCustomerOffersUseCase = mockk(relaxed = true),
                createHotWalletUseCase = createHotWalletUseCase,
                isHotWalletCreationSupported = isHotWalletCreationSupported,
            ),
            analytics = analytics,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            moneyAccountFeatureToggles = moneyAccountFeatureToggles,
            videoFactory = videoFactory,
        )
    }
}