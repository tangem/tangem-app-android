package com.tangem.features.onboarding.v2.entry.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.features.onboarding.v2.common.ui.CantLeaveBackupDialog
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnboardingEntryModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val paramsContainer: ParamsContainer = mockk()

    private val scanResponse: ScanResponse = mockk()
    private val params: OnboardingEntryComponent.Params = mockk {
        every { scanResponse } returns this@OnboardingEntryModelTest.scanResponse
    }

    @BeforeEach
    fun setUp() {
        every { paramsContainer.require<OnboardingEntryComponent.Params>() } returns params
        every { params.mode } returns OnboardingEntryComponent.Mode.Onboarding
        every { scanResponse.productType } returns ProductType.Wallet
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns false
        coEvery { settingsRepository.shouldShowAskBiometry() } returns false
    }

    @ParameterizedTest
    @MethodSource("provideStartRouteByProductType")
    fun `GIVEN product type WHEN model is created THEN startRoute is of expected type`(
        productType: ProductType,
        expectedRouteClass: KClass<out OnboardingRoute>,
    ) = runTest {
        every { scanResponse.productType } returns productType
        every { params.mode } returns OnboardingEntryComponent.Mode.Onboarding

        val model = createModel(this)

        Assertions.assertTrue(
            expectedRouteClass.isInstance(model.startRoute),
            "Expected ${expectedRouteClass.simpleName} but got ${model.startRoute::class.simpleName}",
        )
    }

    @ParameterizedTest
    @MethodSource("provideWallet2ModeMappings")
    fun `GIVEN Wallet2 AND entry mode WHEN model is created THEN multi-wallet mode is mapped`(
        entryMode: OnboardingEntryComponent.Mode,
        expectedMultiWalletMode: OnboardingMultiWalletComponent.Mode,
    ) = runTest {
        every { scanResponse.productType } returns ProductType.Wallet2
        every { params.mode } returns entryMode

        val model = createModel(this)

        val route = model.startRoute as OnboardingRoute.MultiWallet
        Assertions.assertEquals(expectedMultiWalletMode, route.mode)
        Assertions.assertEquals(true, route.withSeedPhraseFlow)
    }

    @ParameterizedTest
    @MethodSource("provideTwinsModeMappings")
    fun `GIVEN Twins AND entry mode WHEN model is created THEN twin mode is mapped`(
        entryMode: OnboardingEntryComponent.Mode,
        expectedTwinMode: OnboardingTwinComponent.Params.Mode,
    ) = runTest {
        every { scanResponse.productType } returns ProductType.Twins
        every { params.mode } returns entryMode

        val model = createModel(this)

        val route = model.startRoute as OnboardingRoute.Twins
        Assertions.assertEquals(expectedTwinMode, route.mode)
    }

    @Test
    fun `GIVEN Wallet WHEN model is created THEN withSeedPhraseFlow is false`() = runTest {
        every { scanResponse.productType } returns ProductType.Wallet
        every { params.mode } returns OnboardingEntryComponent.Mode.Onboarding

        val model = createModel(this)

        val route = model.startRoute as OnboardingRoute.MultiWallet
        Assertions.assertEquals(false, route.withSeedPhraseFlow)
    }

    @Test
    fun `GIVEN biometry available AND should ask WHEN onManageTokensDone THEN AskBiometry replaces stack`() = runTest {
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns true
        coEvery { settingsRepository.shouldShowAskBiometry() } returns true

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onManageTokensDone()
        advanceUntilIdle()

        Assertions.assertEquals(1, stack.size)
        Assertions.assertTrue(stack.first() is OnboardingRoute.AskBiometry)
    }

    @Test
    fun `GIVEN biometry not available WHEN onManageTokensDone THEN Done WalletCreated replaces stack`() = runTest {
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onManageTokensDone()
        advanceUntilIdle()

        Assertions.assertEquals(1, stack.size)
        val route = stack.first()
        Assertions.assertTrue(route is OnboardingRoute.Done)
        Assertions.assertEquals(OnboardingDoneComponent.Mode.WalletCreated, (route as OnboardingRoute.Done).mode)
    }

    @Test
    fun `GIVEN biometry available AND should not ask WHEN onManageTokensDone THEN Done replaces stack`() = runTest {
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns true
        coEvery { settingsRepository.shouldShowAskBiometry() } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onManageTokensDone()
        advanceUntilIdle()

        Assertions.assertEquals(1, stack.size)
        Assertions.assertTrue(stack.first() is OnboardingRoute.Done)
    }

    @Test
    fun `GIVEN Visa AND biometry available WHEN onManageTokensDone THEN BiometricScreenOpened analytics is sent`() =
        runTest {
            every { scanResponse.productType } returns ProductType.Visa
            coEvery { tangemSdkManager.checkCanUseBiometry() } returns true
            coEvery { settingsRepository.shouldShowAskBiometry() } returns true

            val model = createModel(this)

            model.onManageTokensDone()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(match<OnboardingVisaAnalyticsEvent.BiometricScreenOpened> { true })
            }
            verify(exactly = 0) {
                analyticsEventHandler.send(match<OnboardingVisaAnalyticsEvent.SuccessScreenOpened> { true })
            }
        }

    @Test
    fun `GIVEN Visa AND biometry not available WHEN onManageTokensDone THEN SuccessScreenOpened analytics is sent`() =
        runTest {
            every { scanResponse.productType } returns ProductType.Visa
            coEvery { tangemSdkManager.checkCanUseBiometry() } returns false

            val model = createModel(this)

            model.onManageTokensDone()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(match<OnboardingVisaAnalyticsEvent.SuccessScreenOpened> { true })
            }
            verify(exactly = 0) {
                analyticsEventHandler.send(match<OnboardingVisaAnalyticsEvent.BiometricScreenOpened> { true })
            }
        }

    @Test
    fun `GIVEN non-Visa WHEN onManageTokensDone THEN no Visa analytics sent`() = runTest {
        every { scanResponse.productType } returns ProductType.Wallet2
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns true
        coEvery { settingsRepository.shouldShowAskBiometry() } returns true

        val model = createModel(this)

        model.onManageTokensDone()
        advanceUntilIdle()

        verify(exactly = 0) {
            analyticsEventHandler.send(match<OnboardingVisaAnalyticsEvent> { true })
        }
    }

    @Test
    fun `WHEN onBack THEN CantLeaveBackupDialog is sent`() = runTest {
        val model = createModel(this)

        model.onBack()

        verify { uiMessageSender.send(CantLeaveBackupDialog) }
    }

    @Test
    fun `WHEN onboardingTwinModelCallbacks onBack THEN router pop is called`() = runTest {
        val model = createModel(this)

        model.onboardingTwinModelCallbacks.onBack()

        verify { router.pop(onComplete = any()) }
    }

    @Test
    fun `WHEN onboardingTwinModelCallbacks onDone THEN navigateToFinalScreenFlow runs`() = runTest {
        coEvery { tangemSdkManager.checkCanUseBiometry() } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onboardingTwinModelCallbacks.onDone()
        advanceUntilIdle()

        Assertions.assertEquals(1, stack.size)
        val route = stack.first()
        Assertions.assertTrue(route is OnboardingRoute.Done)
        Assertions.assertEquals(OnboardingDoneComponent.Mode.WalletCreated, (route as OnboardingRoute.Done).mode)
    }

    private fun StackNavigation<OnboardingRoute>.trackStack(): List<OnboardingRoute> {
        val tracked = mutableListOf<OnboardingRoute>()
        subscribe { event ->
            val newStack = event.transformer(tracked.toList())
            tracked.clear()
            tracked.addAll(newStack)
        }
        return tracked
    }

    private fun createModel(testScope: TestScope): OnboardingEntryModel {
        return OnboardingEntryModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            tangemSdkManager = tangemSdkManager,
            settingsRepository = settingsRepository,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
            userWalletsListRepository = userWalletsListRepository,
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

    companion object {

        @JvmStatic
        fun provideStartRouteByProductType(): List<Arguments> = listOf(
            Arguments.of(ProductType.Wallet, OnboardingRoute.MultiWallet::class),
            Arguments.of(ProductType.Wallet2, OnboardingRoute.MultiWallet::class),
            Arguments.of(ProductType.Ring, OnboardingRoute.MultiWallet::class),
            Arguments.of(ProductType.Note, OnboardingRoute.Note::class),
            Arguments.of(ProductType.Twins, OnboardingRoute.Twins::class),
            Arguments.of(ProductType.Visa, OnboardingRoute.Visa::class),
        )

        @JvmStatic
        fun provideWallet2ModeMappings(): List<Arguments> {
            val userWalletId = UserWalletId("011")
            return listOf(
                Arguments.of(
                    OnboardingEntryComponent.Mode.Onboarding,
                    OnboardingMultiWalletComponent.Mode.Onboarding,
                ),
                Arguments.of(
                    OnboardingEntryComponent.Mode.AddBackupWallet1,
                    OnboardingMultiWalletComponent.Mode.AddBackup,
                ),
                Arguments.of(
                    OnboardingEntryComponent.Mode.ContinueFinalize,
                    OnboardingMultiWalletComponent.Mode.ContinueFinalize,
                ),
                Arguments.of(
                    OnboardingEntryComponent.Mode.UpgradeHotWallet(userWalletId),
                    OnboardingMultiWalletComponent.Mode.UpgradeHotWallet(userWalletId),
                ),
                Arguments.of(
                    OnboardingEntryComponent.Mode.AddressSync(userWalletId, isWalletStarted = true),
                    OnboardingMultiWalletComponent.Mode.AddressSync(userWalletId, isWalletStarted = true),
                ),
            )
        }

        @JvmStatic
        fun provideTwinsModeMappings(): List<Arguments> = listOf(
            Arguments.of(
                OnboardingEntryComponent.Mode.Onboarding,
                OnboardingTwinComponent.Params.Mode.CreateWallet,
            ),
            Arguments.of(
                OnboardingEntryComponent.Mode.WelcomeOnlyTwin,
                OnboardingTwinComponent.Params.Mode.WelcomeOnly,
            ),
            Arguments.of(
                OnboardingEntryComponent.Mode.RecreateWalletTwin,
                OnboardingTwinComponent.Params.Mode.RecreateWallet,
            ),
        )
    }
}