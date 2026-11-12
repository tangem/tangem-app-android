package com.tangem.features.introduction.impl.addwallet

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.IntroductionProcess.SheetDismissed
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction
import com.tangem.domain.wallets.usecase.GetHotWalletAdditionRestrictionUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.introduction.impl.analytics.SocialLoginAnalyticsEvents
import com.tangem.features.introduction.impl.model.IntroductionMobileWalletCreator
import com.tangem.features.introduction.impl.model.IntroductionSocialLoginHandler
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.res.R as CoreResR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddWalletModelTest {

    private val router: Router = mockk(relaxed = true)
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val getHotWalletAdditionRestriction: GetHotWalletAdditionRestrictionUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val socialLoginHandler: IntroductionSocialLoginHandler = mockk(relaxed = true)
    private val mobileWalletCreator: IntroductionMobileWalletCreator = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val callbacks: AddWalletComponent.ModelCallbacks = mockk(relaxed = true)

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            hotWalletFeatureToggles,
            resolveCloudRestoreBackups,
            analyticsEventHandler,
            isHotWalletCreationSupported,
            getHotWalletAdditionRestriction,
            userWalletsListRepository,
            socialLoginHandler,
            mobileWalletCreator,
            trackingContextProxy,
            uiMessageSender,
            callbacks,
        )
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        every { isHotWalletCreationSupported() } returns true
        every { isHotWalletCreationSupported.getLeastVersionName() } returns LEAST_OS_VERSION
        coEvery { getHotWalletAdditionRestriction(isSocialLogin = any()) } returns null
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet, hotWallet))
        every { socialLoginHandler.provider } returns SocialLoginProvider.Google
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Open {

        @Test
        fun `GIVEN an available provider WHEN open THEN the options step shows all four rows`() = runTest {
            // Arrange
            val model = createModel()

            // Act
            model.open()

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
            val options = model.options()
            assertThat(
                listOf(
                    options.cardOrRing.title,
                    options.socialLogin?.title,
                    options.seedPhrase?.title,
                    options.importWallet.title,
                ),
            ).containsExactly(
                resourceReference(CoreResR.string.introduction_create_wallet_hardware_title),
                resourceReference(
                    id = CoreResR.string.introduction_i_have_wallet_social_title,
                    formatArgs = wrappedList("Google"),
                ),
                resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_title),
                resourceReference(CoreResR.string.hw_import_existing_wallet),
            ).inOrder()
            assertThat(sentEvents).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN no available provider WHEN open THEN the social row is absent`() = runTest {
            // Arrange
            every { socialLoginHandler.provider } returns null
            val model = createModel()

            // Act
            model.open()

            // Assert
            val options = model.options()
            assertThat(options.socialLogin).isNull()
            assertThat(options.seedPhrase).isNotNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet opened before the provider resolves WHEN it resolves THEN the social row appears`() =
            runTest {
                // Arrange
                var provider: SocialLoginProvider? = null
                val resolved = CompletableDeferred<Unit>()
                every { socialLoginHandler.provider } answers { provider }
                coEvery { socialLoginHandler.resolveProvider() } coAnswers {
                    resolved.await()
                    provider = SocialLoginProvider.Google
                }
                val model = createModel()
                model.open()
                assertThat(model.options().socialLogin).isNull()

                // Act
                resolved.complete(Unit)
                advanceUntilIdle()

                // Assert
                assertThat(model.options().socialLogin).isNotNull()
                assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN the sheet shown WHEN open again THEN the same sheet stays`() = runTest {
            // Arrange
            val model = createModel()
            model.open()
            val shownConfig = model.uiState.value.sheetConfig

            // Act
            model.open()

            // Assert
            assertThat(model.uiState.value.sheetConfig).isSameInstanceAs(shownConfig)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardRow {

        @Test
        fun `GIVEN the options step WHEN the card row tapped THEN the tap is reported and the host starts its flow`() =
            runTest {
                // Arrange
                val model = createOpenedModel()

                // Act
                model.options().cardOrRing.onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentEvents).containsExactly(
                    Settings.ButtonAddHardwareWallet(AnalyticsParam.WalletsType.Multiple),
                )
                assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
                verify(exactly = 1) { callbacks.onAddCardClick() }
                verify(exactly = 0) { isHotWalletCreationSupported() }
                coVerify(exactly = 0) { getHotWalletAdditionRestriction(any()) }
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SocialRow {

        @Test
        fun `GIVEN no restriction WHEN the social row tapped THEN the tap is reported before checks and sign in starts`() =
            runTest {
                // Arrange
                val model = createOpenedModel()

                // Act
                model.options().requireSocialRow().onClick()
                advanceUntilIdle()

                // Assert
                coVerifyOrder {
                    analyticsEventHandler.send(
                        SocialLoginAnalyticsEvents.ButtonContinue(
                            provider = SocialLoginProvider.Google,
                            source = AnalyticsParam.ScreensSources.AddNewWallet,
                        ),
                    )
                    isHotWalletCreationSupported()
                    getHotWalletAdditionRestriction(isSocialLogin = true)
                    socialLoginHandler.start(any(), SocialLoginProvider.Google, any())
                }
                assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
                assertThat(model.options().requireSocialRow().isLoading).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN an unsupported device WHEN the social row tapped THEN a warning shows and nothing is checked`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns false
                val model = createOpenedModel()

                // Act
                model.options().requireSocialRow().onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentEvents).containsExactly(
                    SocialLoginAnalyticsEvents.ButtonContinue(
                        provider = SocialLoginProvider.Google,
                        source = AnalyticsParam.ScreensSources.AddNewWallet,
                    ),
                    SocialLoginAnalyticsEvents.MinOsNotSupported(),
                ).inOrder()
                assertThat(capturedDialog().title)
                    .isEqualTo(Dialogs.hotWalletCreationNotSupportedDialog(LEAST_OS_VERSION).title)
                coVerify(exactly = 0) { getHotWalletAdditionRestriction(any()) }
                verify(exactly = 0) { socialLoginHandler.start(any(), any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN no restriction WHEN the social row tapped THEN the hot context precedes the tap and stays`() =
            runTest {
                // Arrange
                val model = createOpenedModel()

                // Act
                model.options().requireSocialRow().onClick()
                advanceUntilIdle()

                // Assert
                coVerifyOrder {
                    trackingContextProxy.addHotWalletContext()
                    analyticsEventHandler.send(
                        SocialLoginAnalyticsEvents.ButtonContinue(
                            provider = SocialLoginProvider.Google,
                            source = AnalyticsParam.ScreensSources.AddNewWallet,
                        ),
                    )
                    socialLoginHandler.start(any(), SocialLoginProvider.Google, any())
                }
                verify(exactly = 0) { trackingContextProxy.removeContext() }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an unsupported device WHEN the social row tapped THEN the context is released after the events`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns false
                val model = createOpenedModel()

                // Act
                model.options().requireSocialRow().onClick()
                advanceUntilIdle()

                // Assert
                verifyOrder {
                    trackingContextProxy.addHotWalletContext()
                    analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.ButtonContinue>())
                    analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.MinOsNotSupported>())
                    trackingContextProxy.removeContext()
                }
                verify(exactly = 1) { trackingContextProxy.removeContext() }
                model.onDestroy()
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a restriction WHEN the social row tapped THEN the context is released before the notice`(
            restriction: HotWalletAdditionRestriction,
        ) = runTest {
            // Arrange
            coEvery { getHotWalletAdditionRestriction(isSocialLogin = true) } returns restriction
            val model = createOpenedModel()

            // Act
            model.options().requireSocialRow().onClick()
            advanceUntilIdle()

            // Assert
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.ButtonContinue>())
                trackingContextProxy.removeContext()
                analyticsEventHandler.send(ofType<NoticeMobileWalletLimit>())
            }
            verify(exactly = 1) { trackingContextProxy.removeContext() }
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            HotWalletAdditionRestriction.LimitReached,
            HotWalletAdditionRestriction.SocialWalletAlreadyAdded,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Restrictions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a restriction WHEN a creating row tapped THEN the notice and its dialog show and nothing starts`(
            model: RestrictedModel,
        ) = runTest {
            // Arrange
            coEvery { getHotWalletAdditionRestriction(isSocialLogin = model.isSocialLogin) } returns model.restriction
            val addWalletModel = createOpenedModel()

            // Act
            model.tapRow(addWalletModel.options())
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents.filterIsInstance<NoticeMobileWalletLimit>()).containsExactly(model.expectedNotice)
            assertThat(sentEvents.last()).isEqualTo(model.expectedNotice)
            val dialog = capturedDialog()
            assertThat(dialog.title).isEqualTo(model.expectedDialogTitle)
            assertThat(dialog.message).isEqualTo(model.expectedDialogMessage)
            assertThat(addWalletModel.uiState.value.sheetConfig?.isShown).isTrue()
            assertThat(addWalletModel.currentStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            coVerify(exactly = 1) { getHotWalletAdditionRestriction(isSocialLogin = model.isSocialLogin) }
            verify(exactly = 0) { socialLoginHandler.start(any(), any(), any()) }
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            verify(exactly = 0) { router.push(any()) }
            addWalletModel.onDestroy()
        }

        @Test
        fun `GIVEN the check fails WHEN the seed phrase row tapped THEN creation starts anyway`() = runTest {
            // Arrange
            coEvery { getHotWalletAdditionRestriction(isSocialLogin = false) } throws IllegalStateException("No wallets")
            val model = createOpenedModel()

            // Act
            model.options().requireSeedPhraseRow().onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                mobileWalletCreator.createAndOpen(AnalyticsParam.ScreensSources.AddNewWallet, any(), any())
            }
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the check fails WHEN the social row tapped THEN the sign-in is aborted with an error`() = runTest {
            // Arrange
            coEvery { getHotWalletAdditionRestriction(isSocialLogin = true) } throws IllegalStateException("No wallets")
            val model = createOpenedModel()

            // Act
            model.options().requireSocialRow().onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { socialLoginHandler.start(any(), any(), any()) }
            verify(exactly = 1) {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
            val dialog = capturedDialog()
            assertThat(dialog.title).isEqualTo(resourceReference(CoreResR.string.common_something_went_wrong))
            assertThat(dialog.message).isEqualTo(resourceReference(CoreResR.string.common_unknown_error))
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet dismissed during the check WHEN the check answers THEN nothing starts`() = runTest {
            // Arrange
            val answer = CompletableDeferred<HotWalletAdditionRestriction?>()
            coEvery { getHotWalletAdditionRestriction(isSocialLogin = false) } coAnswers { answer.await() }
            val model = createOpenedModel()
            model.options().requireSeedPhraseRow().onClick()
            runCurrent()
            requireNotNull(model.uiState.value.sheetConfig).onDismissRequest()

            // Act
            answer.complete(null)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            verify(exactly = 1) {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
            assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            RestrictedModel(
                name = "social login at the limit",
                tapRow = { it.requireSocialRow().onClick() },
                isSocialLogin = true,
                restriction = HotWalletAdditionRestriction.LimitReached,
                expectedNotice = NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.SocialLogin,
                    limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                ),
                expectedDialogTitle = resourceReference(CoreResR.string.hw_wallets_limit_alert_title),
                expectedDialogMessage = resourceReference(
                    id = CoreResR.string.hw_wallets_limit_alert_message,
                    formatArgs = wrappedList("5"),
                ),
            ),
            RestrictedModel(
                name = "social login with a social wallet listed",
                tapRow = { it.requireSocialRow().onClick() },
                isSocialLogin = true,
                restriction = HotWalletAdditionRestriction.SocialWalletAlreadyAdded,
                expectedNotice = NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.SocialLogin,
                    limit = NoticeMobileWalletLimit.Limit.SocialLogin,
                ),
                expectedDialogTitle = resourceReference(
                    id = CoreResR.string.social_wallet_already_added_alert_title,
                    formatArgs = wrappedList("Google"),
                ),
                expectedDialogMessage = resourceReference(
                    id = CoreResR.string.social_wallet_already_added_alert_message,
                    formatArgs = wrappedList("Google"),
                ),
            ),
            RestrictedModel(
                name = "seed phrase at the limit",
                tapRow = { requireNotNull(it.seedPhrase).onClick() },
                isSocialLogin = false,
                restriction = HotWalletAdditionRestriction.LimitReached,
                expectedNotice = NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.Create,
                    limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                ),
                expectedDialogTitle = resourceReference(CoreResR.string.hw_wallets_limit_alert_title),
                expectedDialogMessage = resourceReference(
                    id = CoreResR.string.hw_wallets_limit_alert_message,
                    formatArgs = wrappedList("5"),
                ),
            ),
            RestrictedModel(
                name = "import at the limit",
                tapRow = { it.importWallet.onClick() },
                isSocialLogin = false,
                restriction = HotWalletAdditionRestriction.LimitReached,
                expectedNotice = NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.Import,
                    limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                ),
                expectedDialogTitle = resourceReference(CoreResR.string.hw_wallets_limit_alert_title),
                expectedDialogMessage = resourceReference(
                    id = CoreResR.string.hw_wallets_limit_alert_message,
                    formatArgs = wrappedList("5"),
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SocialHost {

        @Test
        fun `GIVEN the sheet shown WHEN a bare loader reported THEN the sheet keeps the spinning row`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)

            // Act
            host.onProgress(SocialLoginProgressUM.Loader)

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
            assertThat(model.uiState.value.socialLoginProgress).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet shown WHEN a stage reported THEN the sheet steps aside for it`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)

            // Act
            host.onProgress(STAGE)

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
            assertThat(model.uiState.value.socialLoginProgress).isEqualTo(STAGE)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet shown WHEN the consent shown THEN the sheet steps aside for it`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)
            val consent: SocialLoginConsentUM = mockk()

            // Act
            host.onConsent(consent)

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
            assertThat(model.uiState.value.socialLoginConsent).isSameInstanceAs(consent)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet stepped aside WHEN the sign in finishes without a wallet THEN the options return`() =
            runTest {
                // Arrange
                val model = createOpenedModel()
                val host = startSocialLogin(model)
                host.onProgress(STAGE)

                // Act
                host.onProgress(null)
                host.onFinished()

                // Assert
                assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
                assertThat(model.uiState.value.socialLoginProgress).isNull()
                assertThat(model.options().requireSocialRow().isLoading).isFalse()
                model.onDestroy()
            }

        @Test
        fun `GIVEN the sheet stepped aside WHEN the wallet opens THEN the sheet does not return`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)
            host.onProgress(STAGE)

            // Act
            host.onWalletOpening()
            host.onFinished()

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a sign in started WHEN it finishes without a wallet THEN the hot context is released`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)

            // Act
            host.onFinished()

            // Assert
            verifyOrder {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
            verify(exactly = 1) { trackingContextProxy.removeContext() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a sign in started WHEN the wallet opens THEN the hot context is kept for it to replace`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)

            // Act
            host.onWalletOpening()
            host.onFinished()

            // Assert
            verify(exactly = 0) { trackingContextProxy.removeContext() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet still shown WHEN the sign in finishes THEN the sheet stays with the row released`() =
            runTest {
                // Arrange
                val model = createOpenedModel()
                val host = startSocialLogin(model)

                // Act
                host.onFinished()

                // Assert
                assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
                assertThat(model.options().requireSocialRow().isLoading).isFalse()
                assertThat(model.options().cardOrRing.isEnabled).isTrue()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SeedPhraseRow {

        @Test
        fun `GIVEN no restriction WHEN the seed phrase row tapped THEN the context precedes the tap and creation starts`() =
            runTest {
                // Arrange
                val model = createOpenedModel()

                // Act
                model.options().requireSeedPhraseRow().onClick()
                advanceUntilIdle()

                // Assert
                coVerifyOrder {
                    trackingContextProxy.addHotWalletContext()
                    analyticsEventHandler.send(
                        OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet(source = "Add New Wallet"),
                    )
                    isHotWalletCreationSupported()
                    getHotWalletAdditionRestriction(isSocialLogin = false)
                    mobileWalletCreator.createAndOpen(AnalyticsParam.ScreensSources.AddNewWallet, any(), any())
                }
                verify(exactly = 0) { trackingContextProxy.removeContext() }
                assertThat(model.options().requireSeedPhraseRow().isLoading).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN an unsupported device WHEN the seed phrase row tapped THEN the context is released`() = runTest {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            val model = createOpenedModel()

            // Act
            model.options().requireSeedPhraseRow().onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                trackingContextProxy.addHotWalletContext()
                trackingContextProxy.removeContext()
            }
            assertThat(capturedDialog().title)
                .isEqualTo(Dialogs.hotWalletCreationNotSupportedDialog(LEAST_OS_VERSION).title)
            coVerify(exactly = 0) { getHotWalletAdditionRestriction(any()) }
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the limit reached WHEN the seed phrase row tapped THEN the context is released`() = runTest {
            // Arrange
            coEvery {
                getHotWalletAdditionRestriction(isSocialLogin = false)
            } returns HotWalletAdditionRestriction.LimitReached
            val model = createOpenedModel()

            // Act
            model.options().requireSeedPhraseRow().onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { trackingContextProxy.removeContext() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the creation started WHEN it fails THEN the context is released and the spinner cleared`() =
            runTest {
                // Arrange
                val onFailed = slot<() -> Unit>()
                coEvery { mobileWalletCreator.createAndOpen(any(), any(), capture(onFailed)) } just runs
                val model = createOpenedModel()
                model.options().requireSeedPhraseRow().onClick()
                advanceUntilIdle()

                // Act
                onFailed.captured()

                // Assert
                verify(exactly = 1) { trackingContextProxy.removeContext() }
                assertThat(model.options().requireSeedPhraseRow().isLoading).isFalse()
                assertThat(model.options().cardOrRing.isEnabled).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN the wallet created WHEN it is about to open THEN the sheet collapses before the stack swaps`() =
            runTest {
                // Arrange
                val beforeOpen = slot<suspend () -> Unit>()
                coEvery { mobileWalletCreator.createAndOpen(any(), capture(beforeOpen), any()) } just runs
                val model = createOpenedModel()
                model.options().requireSeedPhraseRow().onClick()
                advanceUntilIdle()

                // Act
                val collapse = launch { beforeOpen.captured() }
                runCurrent()

                // Assert
                assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
                assertThat(collapse.isActive).isTrue()
                model.uiState.value.onSheetCollapsed()
                runCurrent()
                assertThat(collapse.isCompleted).isTrue()
                assertThat(sentEvents.filterIsInstance<SheetDismissed>()).isEmpty()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ImportRow {

        @Test
        fun `GIVEN no restriction WHEN the import row tapped THEN the import step opens and the request is reported`() =
            runTest {
                // Arrange
                val model = createOpenedModel()

                // Act
                model.options().importWallet.onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentEvents).containsExactly(
                    IntroductionProcess.ButtonImportExistingWallet(source = AnalyticsParam.ScreensSources.AddNewWallet),
                    OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
                ).inOrder()
                assertThat(
                    sentEvents.filterIsInstance<OnboardingAnalyticsEvent.Backup.ImportWalletRequest>()
                        .single().isSelectedWalletUnrelated,
                ).isTrue()
                assertThat(model.currentStep()).isInstanceOf(HaveWalletUM.Step.Import::class.java)
                assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
                coVerify(exactly = 1) { getHotWalletAdditionRestriction(isSocialLogin = false) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN an unsupported device WHEN the import row tapped THEN a warning shows and nothing is checked`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns false
                val model = createOpenedModel()

                // Act
                model.options().importWallet.onClick()
                advanceUntilIdle()

                // Assert
                assertThat(sentEvents).containsExactly(
                    IntroductionProcess.ButtonImportExistingWallet(source = AnalyticsParam.ScreensSources.AddNewWallet),
                )
                assertThat(capturedDialog().title)
                    .isEqualTo(Dialogs.hotWalletCreationNotSupportedDialog(LEAST_OS_VERSION).title)
                assertThat(model.currentStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
                coVerify(exactly = 0) { getHotWalletAdditionRestriction(any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the import step WHEN recovery phrase picked THEN the import flow opens with the add wallet source`() =
            runTest {
                // Arrange
                val model = createOpenedModel()
                model.options().importWallet.onClick()
                advanceUntilIdle()

                // Act
                model.importStep().onRecoveryPhraseClick()

                // Assert
                assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
                verify(exactly = 1) {
                    router.push(
                        AppRoute.AddExistingWallet(
                            mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase,
                            shouldSendOnboardingStarted = true,
                            analyticsSource = AnalyticsParam.ScreensSources.AddNewWallet,
                        ),
                    )
                }
                assertThat(sentEvents.filterIsInstance<SheetDismissed>()).isEmpty()
                model.onDestroy()
            }

        @Test
        fun `GIVEN no restriction WHEN the import row tapped THEN the tracking context is left as is`() = runTest {
            // Arrange
            val model = createOpenedModel()

            // Act
            model.options().importWallet.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { trackingContextProxy.addHotWalletContext() }
            verify(exactly = 0) { trackingContextProxy.removeContext() }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Dismissal {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the options step WHEN the viewer leaves THEN the add wallet sheet dismissal is reported`(
            model: DismissModel,
        ) = runTest {
            // Arrange
            val addWalletModel = createOpenedModel()

            // Act
            model.leave(addWalletModel)

            // Assert
            assertThat(sentEvents).containsExactly(SheetDismissed(SheetDismissed.Sheet.AddWallet, model.reason))
            assertThat(addWalletModel.uiState.value.sheetConfig?.isShown).isFalse()
            addWalletModel.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the import step WHEN the viewer leaves THEN the import sheet dismissal is reported`(
            model: DismissModel,
        ) = runTest {
            // Arrange
            val addWalletModel = createOpenedModel()
            addWalletModel.options().importWallet.onClick()
            advanceUntilIdle()
            sentEvents.clear()

            // Act
            model.leave(addWalletModel)

            // Assert
            assertThat(sentEvents).containsExactly(SheetDismissed(SheetDismissed.Sheet.ImportExisting, model.reason))
            assertThat(addWalletModel.uiState.value.sheetConfig?.isShown).isFalse()
            addWalletModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            DismissModel(
                reason = SheetDismissed.DismissReason.CloseButton,
                leave = { it.content().onCloseClick() },
            ),
            DismissModel(
                reason = SheetDismissed.DismissReason.Dismissed,
                leave = { requireNotNull(it.uiState.value.sheetConfig).onDismissRequest() },
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BusyGuards {

        @Test
        fun `GIVEN a restriction check running WHEN other rows tapped THEN nothing happens`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val options = model.options()
            options.importWallet.onClick()

            // Act
            options.cardOrRing.onClick()
            requireNotNull(options.seedPhrase).onClick()
            options.requireSocialRow().onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents).containsExactly(
                IntroductionProcess.ButtonImportExistingWallet(source = AnalyticsParam.ScreensSources.AddNewWallet),
                OnboardingAnalyticsEvent.Backup.ImportWalletRequest(AnalyticsParam.CloudBackupAvailability.Available),
            ).inOrder()
            coVerify(exactly = 1) { getHotWalletAdditionRestriction(any()) }
            verify(exactly = 0) { callbacks.onAddCardClick() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the import step sliding in WHEN an options row tapped THEN nothing starts`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val options = model.options()
            options.importWallet.onClick()
            advanceUntilIdle()
            sentEvents.clear()

            // Act
            options.cardOrRing.onClick()
            options.requireSocialRow().onClick()
            options.requireSeedPhraseRow().onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents).isEmpty()
            verify(exactly = 0) { callbacks.onAddCardClick() }
            verify(exactly = 0) { socialLoginHandler.start(any(), any(), any()) }
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            assertThat(model.currentStep()).isInstanceOf(HaveWalletUM.Step.Import::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN social login running WHEN rows tapped again THEN nothing else starts`() = runTest {
            // Arrange
            val model = createOpenedModel()
            startSocialLogin(model)
            sentEvents.clear()
            val options = model.options()

            // Act
            options.requireSocialRow().onClick()
            options.cardOrRing.onClick()
            options.requireSeedPhraseRow().onClick()
            options.importWallet.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents).isEmpty()
            verify(exactly = 1) { socialLoginHandler.start(any(), any(), any()) }
            verify(exactly = 0) { callbacks.onAddCardClick() }
            coVerify(exactly = 0) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            assertThat(model.currentStep()).isInstanceOf(HaveWalletUM.Step.Options::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN social login running WHEN the viewer closes the sheet THEN it stays open and silent`() = runTest {
            // Arrange
            val model = createOpenedModel()
            startSocialLogin(model)
            sentEvents.clear()

            // Act
            model.content().onCloseClick()
            requireNotNull(model.uiState.value.sheetConfig).onDismissRequest()

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isTrue()
            assertThat(sentEvents).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sheet stepped aside for social login WHEN open requested THEN it stays closed`() = runTest {
            // Arrange
            val model = createOpenedModel()
            val host = startSocialLogin(model)
            host.onProgress(STAGE)

            // Act
            model.open()

            // Assert
            assertThat(model.uiState.value.sheetConfig?.isShown).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a mobile wallet being created WHEN rows tapped again THEN nothing else starts`() = runTest {
            // Arrange
            val model = createOpenedModel()
            model.options().requireSeedPhraseRow().onClick()
            advanceUntilIdle()
            sentEvents.clear()
            val options = model.options()

            // Act
            options.requireSeedPhraseRow().onClick()
            options.requireSocialRow().onClick()
            options.cardOrRing.onClick()
            options.importWallet.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents).isEmpty()
            coVerify(exactly = 1) { mobileWalletCreator.createAndOpen(any(), any(), any()) }
            verify(exactly = 0) { socialLoginHandler.start(any(), any(), any()) }
            verify(exactly = 0) { callbacks.onAddCardClick() }
            model.onDestroy()
        }
    }

    internal data class RestrictedModel(
        val name: String,
        val tapRow: (HaveWalletUM.Step.Options) -> Unit,
        val isSocialLogin: Boolean,
        val restriction: HotWalletAdditionRestriction,
        val expectedNotice: NoticeMobileWalletLimit,
        val expectedDialogTitle: TextReference,
        val expectedDialogMessage: TextReference,
    ) {
        override fun toString(): String = name
    }

    internal data class DismissModel(
        val reason: SheetDismissed.DismissReason,
        val leave: (AddWalletModel) -> Unit,
    ) {
        override fun toString(): String = reason.value
    }

    private fun TestScope.createOpenedModel(): AddWalletModel = createModel().also { it.open() }

    private fun TestScope.createModel(): AddWalletModel {
        val model = AddWalletModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(AddWalletComponent.Params(callbacks = callbacks)),
            router = router,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
            resolveCloudRestoreBackups = resolveCloudRestoreBackups,
            analyticsEventHandler = analyticsEventHandler,
            isHotWalletCreationSupported = isHotWalletCreationSupported,
            getHotWalletAdditionRestriction = getHotWalletAdditionRestriction,
            userWalletsListRepository = userWalletsListRepository,
            socialLoginHandler = socialLoginHandler,
            mobileWalletCreator = mobileWalletCreator,
            trackingContextProxy = trackingContextProxy,
            uiMessageSender = uiMessageSender,
        )
        advanceUntilIdle()
        return model
    }

    private fun TestScope.startSocialLogin(model: AddWalletModel): IntroductionSocialLoginHandler.Host {
        val host = slot<IntroductionSocialLoginHandler.Host>()
        every { socialLoginHandler.start(any(), any(), capture(host)) } just runs
        model.options().requireSocialRow().onClick()
        advanceUntilIdle()
        return host.captured
    }

    private fun capturedDialog(): DialogMessage {
        val message = slot<DialogMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        return message.captured
    }

    private fun AddWalletModel.content(): HaveWalletUM =
        uiState.value.sheetConfig?.content as? HaveWalletUM ?: error("the sheet has no content")

    private fun AddWalletModel.currentStep(): HaveWalletUM.Step = content().step

    private fun AddWalletModel.options(): HaveWalletUM.Step.Options =
        currentStep() as? HaveWalletUM.Step.Options ?: error("the sheet is not on the Options step")

    private fun AddWalletModel.importStep(): HaveWalletUM.Step.Import =
        currentStep() as? HaveWalletUM.Step.Import ?: error("the sheet is not on the Import step")

    private fun HaveWalletUM.Step.Options.requireSocialRow() = requireNotNull(socialLogin)

    private fun HaveWalletUM.Step.Options.requireSeedPhraseRow() = requireNotNull(seedPhrase)

    private companion object {
        const val LEAST_OS_VERSION = "Android 10"
        val STAGE = SocialLoginProgressUM.Stage(title = stringReference("Creating"))
        val coldWallet: UserWallet = mockk<UserWallet.Cold>()
        val hotWallet: UserWallet = mockk<UserWallet.Hot>()
    }
}