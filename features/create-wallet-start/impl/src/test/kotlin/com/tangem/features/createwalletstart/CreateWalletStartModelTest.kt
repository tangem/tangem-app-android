package com.tangem.features.createwalletstart

import arrow.core.left
import arrow.core.right
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.createwalletstart.entity.CreateWalletStartUM
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateWalletStartModelTest {

    private val router: Router = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val scanCardProcessor: ScanCardProcessor = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk()
    private val coldUserWalletBuilder: ColdUserWalletBuilder = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val appsFlyerStore: AppsFlyerStore = mockk()
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles = mockk()

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val testScanResponse: ScanResponse = mockk(relaxed = true)
    private val testColdWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns testUserWalletId
    }

    @BeforeEach
    fun setUp() {
        coEvery { appsFlyerStore.get() } returns null
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns false
        every { coldUserWalletBuilderFactory.create(any()) } returns coldUserWalletBuilder
        coEvery { coldUserWalletBuilder.build() } returns testColdWallet
        every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns false
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } just Runs
    }

    @Test
    fun `GIVEN ColdWallet mode WHEN onScanClick THEN ButtonScanCard event sent AND scan called`() = runTest {
        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(
                match<IntroductionProcess.ButtonScanCard> {
                    it.source == AnalyticsParam.ScreensSources.CreateWalletIntro
                },
            )
        }
        coVerify {
            scanCardProcessor.scan(
                analyticsSource = AnalyticsParam.ScreensSources.Intro,
                shouldCheckIsAlreadyActivated = true,
                cardId = null,
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `GIVEN HotWallet mode WHEN onScanClick THEN ButtonScanCard event sent AND scan called`() = runTest {
        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.HotWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(
                match<IntroductionProcess.ButtonScanCard> {
                    it.source == AnalyticsParam.ScreensSources.CreateWalletIntro
                },
            )
        }
        coVerify {
            scanCardProcessor.scan(
                analyticsSource = AnalyticsParam.ScreensSources.Intro,
                shouldCheckIsAlreadyActivated = true,
                cardId = null,
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `GIVEN ColdWallet AND hot wallet not supported WHEN otherMethodClick THEN dialog sent`() = runTest {
        every { isHotWalletCreationSupported() } returns false
        every { isHotWalletCreationSupported.getLeastVersionName() } returns "13"

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.otherMethodClick.invoke()
        advanceUntilIdle()

        verify { trackingContextProxy.addHotWalletContext() }
        verify {
            analyticsEventHandler.send(
                match<OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet> { true },
            )
        }
        verify { uiMessageSender.send(match<DialogMessage> { true }) }
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN HotWallet AND hot wallet supported WHEN onPrimaryButtonClick THEN CreateMobileWallet pushed`() =
        runTest {
            every { isHotWalletCreationSupported() } returns true

            val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.HotWallet)
            advanceUntilIdle()

            model.uiState.value.onPrimaryButtonClick.invoke()
            advanceUntilIdle()

            verify { trackingContextProxy.addHotWalletContext() }
            verify {
                analyticsEventHandler.send(
                    match<OnboardingAnalyticsEvent.Onboarding.ButtonMobileWallet> { true },
                )
            }
            verify {
                router.push(
                    route = AppRoute.CreateMobileWallet(
                        source = AnalyticsParam.ScreensSources.CreateWalletIntro,
                    ),
                    onComplete = any(),
                )
            }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

    @Test
    fun `GIVEN ColdWallet mode WHEN onPrimaryButtonClick THEN buy link opened`() = runTest {
        val testUrl = "https://buy.tangem.com"
        coEvery {
            generateBuyTangemCardLinkUseCase.invoke(GenerateBuyTangemCardLinkUseCase.Source.Creation)
        } returns testUrl

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onPrimaryButtonClick.invoke()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(
                match<Basic.ButtonBuy> { true },
            )
        }
        coVerify { generateBuyTangemCardLinkUseCase.invoke(GenerateBuyTangemCardLinkUseCase.Source.Creation) }
        verify { urlOpener.openUrl(testUrl) }
    }

    @Test
    fun `GIVEN HotWallet mode WHEN onBuyClick THEN buy link opened`() = runTest {
        val testUrl = "https://buy.tangem.com"
        coEvery {
            generateBuyTangemCardLinkUseCase.invoke(GenerateBuyTangemCardLinkUseCase.Source.Creation)
        } returns testUrl

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.HotWallet)
        advanceUntilIdle()

        model.uiState.value.otherMethodClick.invoke()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(
                match<Basic.ButtonBuy> { true },
            )
        }
        coVerify { generateBuyTangemCardLinkUseCase.invoke(GenerateBuyTangemCardLinkUseCase.Source.Creation) }
        verify { urlOpener.openUrl(testUrl) }
    }

    @Test
    fun `WHEN scanCard THEN access code policy set AND scanProcessor called`() = runTest {
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns true
        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify { cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = true) }
        coVerify {
            scanCardProcessor.scan(
                analyticsSource = AnalyticsParam.ScreensSources.Intro,
                shouldCheckIsAlreadyActivated = true,
                cardId = null,
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `GIVEN NfcFeatureIsUnavailable WHEN handleScanError THEN nfcFeatureUnavailable dialog sent`() = runTest {
        val error = TangemSdkError.NfcFeatureIsUnavailable()
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } coAnswers {
            val onFailure = arg<suspend (TangemError) -> Unit>(6)
            onFailure.invoke(error)
        }

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify { uiMessageSender.send(match<DialogMessage> { true }) }
    }

    @Test
    fun `GIVEN generic TangemSdkError WHEN handleScanError THEN no dialog sent`() = runTest {
        val error: TangemSdkError = mockk(relaxed = true)
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } coAnswers {
            val onFailure = arg<suspend (TangemError) -> Unit>(6)
            onFailure.invoke(error)
        }

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN non-sdk TangemError WHEN handleScanError THEN no dialog sent`() = runTest {
        val error: TangemError = mockk(relaxed = true)
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } coAnswers {
            val onFailure = arg<suspend (TangemError) -> Unit>(6)
            onFailure.invoke(error)
        }

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN builder returns null WHEN proceedWithScanResponse THEN saveWalletUseCase not called`() = runTest {
        coEvery { coldUserWalletBuilder.build() } returns null
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } coAnswers {
            val onSuccess = arg<suspend (ScanResponse) -> Unit>(7)
            onSuccess.invoke(testScanResponse)
        }

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify { coldUserWalletBuilderFactory.create(scanResponse = testScanResponse) }
        coVerify(exactly = 0) { saveWalletUseCase(userWallet = any(), canOverride = any(), analyticsSource = any()) }
        verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN save returns WalletAlreadySaved WHEN proceedWithScanResponse THEN unlock called AND Wallet replaced`() =
        runTest {
            coEvery {
                saveWalletUseCase(userWallet = any(), canOverride = any(), analyticsSource = any())
            } returns SaveWalletError.WalletAlreadySaved(messageId = 0).left()
            coEvery {
                userWalletsListRepository.unlock(
                    userWalletId = testUserWalletId,
                    unlockMethod = any(),
                )
            } returns Unit.right()
            coEvery {
                scanCardProcessor.scan(
                    analyticsSource = any(),
                    shouldCheckIsAlreadyActivated = any(),
                    cardId = any(),
                    onProgressStateChange = any(),
                    onWalletNotCreated = any(),
                    onCancel = any(),
                    onFailure = any(),
                    onSuccess = any()
                )
            } coAnswers {
                val onSuccess = arg<suspend (ScanResponse) -> Unit>(7)
                onSuccess.invoke(testScanResponse)
            }

            val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
            advanceUntilIdle()

            model.uiState.value.onScanClick.invoke()
            advanceUntilIdle()

            coVerify {
                userWalletsListRepository.unlock(
                    userWalletId = testUserWalletId,
                    unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(
                        scanResponse = testScanResponse,
                        source = AnalyticsParam.ScreensSources.Intro,
                    ),
                )
            }
            verify { appRouter.replaceAll(routes = arrayOf(AppRoute.Wallet), onComplete = any()) }
        }

    @Test
    fun `GIVEN save returns DataError WHEN proceedWithScanResponse THEN no unlock AND no replace`() = runTest {
        coEvery {
            saveWalletUseCase(userWallet = any(), canOverride = any(), analyticsSource = any())
        } returns SaveWalletError.DataError(messageId = 0).left()
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any()
            )
        } coAnswers {
            val onSuccess = arg<suspend (ScanResponse) -> Unit>(7)
            onSuccess.invoke(testScanResponse)
        }

        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        coVerify(exactly = 0) { userWalletsListRepository.unlock(any(), any()) }
        verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN save success AND isAddressSyncEnabled disabled WHEN proceedWithScanResponse THEN Wallet replaced`() =
        runTest {
            every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns false
            coEvery {
                saveWalletUseCase(userWallet = any(), canOverride = any(), analyticsSource = any())
            } returns Unit.right()
            coEvery {
                scanCardProcessor.scan(
                    analyticsSource = any(),
                    shouldCheckIsAlreadyActivated = any(),
                    cardId = any(),
                    onProgressStateChange = any(),
                    onWalletNotCreated = any(),
                    onCancel = any(),
                    onFailure = any(),
                    onSuccess = any()
                )
            } coAnswers {
                val onSuccess = arg<suspend (ScanResponse) -> Unit>(7)
                onSuccess.invoke(testScanResponse)
            }

            val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
            advanceUntilIdle()

            model.uiState.value.onScanClick.invoke()
            advanceUntilIdle()

            verify { appRouter.replaceAll(routes = arrayOf(AppRoute.Wallet), onComplete = any()) }
        }

    @Test
    fun `GIVEN save success AND isAddressSyncEnabled WHEN proceedWithScanResponse THEN AddressSync replaced`() =
        runTest {
            every { onboardingV2FeatureToggles.isAddressSyncEnabled } returns true
            coEvery {
                saveWalletUseCase(userWallet = any(), canOverride = any(), analyticsSource = any())
            } returns Unit.right()
            coEvery {
                scanCardProcessor.scan(
                    analyticsSource = any(),
                    shouldCheckIsAlreadyActivated = any(),
                    cardId = any(),
                    onProgressStateChange = any(),
                    onWalletNotCreated = any(),
                    onCancel = any(),
                    onFailure = any(),
                    onSuccess = any()
                )
            } coAnswers {
                val onSuccess = arg<suspend (ScanResponse) -> Unit>(7)
                onSuccess.invoke(testScanResponse)
            }

            val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
            advanceUntilIdle()

            model.uiState.value.onScanClick.invoke()
            advanceUntilIdle()

            verify {
                appRouter.replaceAll(
                    routes = arrayOf(
                        AppRoute.Onboarding(
                            scanResponse = testScanResponse,
                            mode = AppRoute.Onboarding.Mode.AddressSync(
                                userWalletId = testUserWalletId,
                                isWalletStarted = false,
                            ),
                        )
                    ),
                    onComplete = any()
                )
            }
        }

    @Test
    fun `WHEN model initialized THEN CreateWalletIntroScreenOpened event sent with referral id`() = runTest {
        val refcode = "referralCode"
        coEvery { appsFlyerStore.get() } returns AppsFlyerConversionData(refcode = refcode, campaign = null)

        createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(
                match<IntroductionProcess.CreateWalletIntroScreenOpened> { true },
            )
        }
    }

    @Test
    fun `WHEN ColdWallet WHEN get model THEN correct resources`() = runTest {
        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.ColdWallet)
        val state = model.uiState.value
        assert(state.title == resourceReference(R.string.common_tangem_wallet))
        assert(state.description == resourceReference(R.string.welcome_create_wallet_hardware_description))
        assert(
            state.featureItems == persistentListOf(
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_shield_check_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_class),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_flash_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_delivery),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_sparkles_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_use),
                )
            )
        )
        assert(state.imageResId == R.drawable.img_hardware_wallet)
        assert(state.shouldShowScanSecondaryButton)
        assert(state.primaryButtonText == resourceReference(R.string.details_buy_wallet))
        assert(state.otherMethodTitle == resourceReference(R.string.welcome_create_wallet_mobile_title))
        assert(state.otherMethodDescription == null)
        assert(state.isScanInProgress.not())
    }

    @Test
    fun `WHEN HotWallet WHEN get model THEN correct resources`() = runTest {
        val model = createModel(testScope = this, mode = CreateWalletStartComponent.Mode.HotWallet)
        val state = model.uiState.value
        assert(state.title == resourceReference(R.string.hw_mobile_wallet))
        assert(state.description == resourceReference(R.string.welcome_create_wallet_mobile_description_full))
        assert(
            state.featureItems == persistentListOf(
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_shield_check_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_seamless),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_flash_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_one_tap),
                ),
                CreateWalletStartUM.FeatureItem(
                    iconResId = R.drawable.ic_stack_fill_new_16,
                    text = resourceReference(R.string.welcome_create_wallet_feature_assets),
                ),
            )
        )
        assert(state.imageResId == R.drawable.img_mobile_wallet)
        assert(state.shouldShowScanSecondaryButton.not())
        assert(state.primaryButtonText == resourceReference(R.string.welcome_create_wallet_mobile_title))
        assert(state.otherMethodTitle == resourceReference(R.string.welcome_create_wallet_use_hardware_title))
        assert(state.otherMethodDescription == resourceReference(R.string.welcome_create_wallet_use_hardware_description))
        assert(state.isScanInProgress.not())
    }

    private fun createModel(
        testScope: TestScope,
        mode: CreateWalletStartComponent.Mode,
        paramsContainer: ParamsContainer = MutableParamsContainer(
            value = CreateWalletStartComponent.Params(mode = mode)
        ),
    ): CreateWalletStartModel {
        return CreateWalletStartModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            scanCardProcessor = scanCardProcessor,
            cardSdkConfigRepository = cardSdkConfigRepository,
            settingsRepository = settingsRepository,
            appRouter = appRouter,
            coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
            saveWalletUseCase = saveWalletUseCase,
            isHotWalletCreationSupported = isHotWalletCreationSupported,
            userWalletsListRepository = userWalletsListRepository,
            uiMessageSender = uiMessageSender,
            generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
            urlOpener = urlOpener,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            appsFlyerStore = appsFlyerStore,
            onboardingV2FeatureToggles = onboardingV2FeatureToggles,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}