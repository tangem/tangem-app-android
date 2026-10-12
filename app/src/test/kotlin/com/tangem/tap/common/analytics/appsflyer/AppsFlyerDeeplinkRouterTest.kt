package com.tangem.tap.common.analytics.appsflyer

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.deeplink.OnboardVisaDeepLinkHandler
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppsFlyerDeeplinkRouterTest {

    private val appsFlyerRepository: AppsFlyerRepository = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()
    private val onboardVisaDeepLink: OnboardVisaDeepLinkHandler.Factory = mockk(relaxed = true)
    private val urisByString = listOf(DIRECT_URI, UNSUPPORTED_URI).associateWith { mockk<Uri>() }
    private val directUri: Uri = urisByString.getValue(DIRECT_URI)

    private val router = AppsFlyerDeeplinkRouter(
        appsFlyerRepository = appsFlyerRepository,
        userWalletsListRepository = userWalletsListRepository,
        clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
        appRouter = appRouter,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
        onboardVisaDeepLink = onboardVisaDeepLink,
    )

    @BeforeAll
    fun mockUriParsing() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { urisByString.getValue(firstArg()) }
    }

    @AfterAll
    fun unmockUriParsing() {
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun setup() {
        clearMocks(
            appsFlyerRepository,
            userWalletsListRepository,
            clearAppsFlyerDeeplinkUseCase,
            appRouter,
            tangemPayFeatureToggles,
            onboardVisaDeepLink,
        )
        // Happy baseline: deep link pending, authorized.
        every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf("tpay_mobileonboard")
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
        every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns true
    }

    // region navigation blocklist

    @ParameterizedTest
    @ProvideTestModels
    fun isNavigationBlocked(model: BlockedRouteModel) {
        assertThat(isNavigationBlocked(model.currentRoute)).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<BlockedRouteModel> = listOf(
        // Any regular screen — a deep link may route from here.
        BlockedRouteModel(AppRoute.Home(), expected = false),
        BlockedRouteModel(AppRoute.Stories(storyId = "id", screenSource = "src"), expected = false),
        BlockedRouteModel(AppRoute.Wallet, expected = false),
        BlockedRouteModel(AppRoute.Details(userWalletId = WALLET_ID), expected = false),
        BlockedRouteModel(AppRoute.AppSettings, expected = false),
        BlockedRouteModel(AppRoute.AddressBook(), expected = false),
        BlockedRouteModel(AppRoute.Markets(), expected = false),
        // Startup gates and the Tangem Pay flows the links open — must not be interrupted.
        BlockedRouteModel(currentRoute = null, expected = true),
        BlockedRouteModel(AppRoute.Initial, expected = true),
        BlockedRouteModel(AppRoute.Welcome(), expected = true),
        BlockedRouteModel(AppRoute.Disclaimer(isTosAccepted = true), expected = true),
        BlockedRouteModel(AppRoute.PushNotification(source = AppRoute.PushNotification.Source.Main), expected = true),
        BlockedRouteModel(AppRoute.Onboarding(scanResponse = mockk()), expected = true),
        BlockedRouteModel(AppRoute.Kyc(userWalletId = WALLET_ID), expected = true),
        BlockedRouteModel(AppRoute.TangemPayHotWalletOnboarding, expected = true),
        BlockedRouteModel(
            AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink),
            expected = true,
        ),
        BlockedRouteModel(AppRoute.TangemPayCampaign(CAMPAIGN_VALUE), expected = true),
    )

    data class BlockedRouteModel(val currentRoute: AppRoute?, val expected: Boolean)

    @ParameterizedTest
    @MethodSource("provideFreshInstallEntryModels")
    fun isFreshInstallEntry(model: BlockedRouteModel) {
        assertThat(isFreshInstallEntry(model.currentRoute)).isEqualTo(model.expected)
    }

    private fun provideFreshInstallEntryModels(): List<BlockedRouteModel> = listOf(
        BlockedRouteModel(AppRoute.Home(), expected = true),
        BlockedRouteModel(AppRoute.Stories(storyId = "id", screenSource = "src"), expected = true),
        BlockedRouteModel(
            AppRoute.Home(entryReason = AppRoute.Home.EntryReason.LastWalletForgotten),
            expected = false,
        ),
        BlockedRouteModel(AppRoute.Wallet, expected = false),
        BlockedRouteModel(AppRoute.AppSettings, expected = false),
        BlockedRouteModel(
            AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            expected = false,
        ),
        BlockedRouteModel(currentRoute = null, expected = false),
    )

    @ParameterizedTest
    @MethodSource("provideIdleEntryPointModels")
    fun isIdleEntryPoint(model: BlockedRouteModel) {
        assertThat(isIdleEntryPoint(model.currentRoute)).isEqualTo(model.expected)
    }

    private fun provideIdleEntryPointModels(): List<BlockedRouteModel> = listOf(
        BlockedRouteModel(AppRoute.Home(), expected = true),
        BlockedRouteModel(AppRoute.Stories(storyId = "id", screenSource = "src"), expected = true),
        BlockedRouteModel(AppRoute.Wallet, expected = true),
        BlockedRouteModel(AppRoute.Initial, expected = false),
        BlockedRouteModel(AppRoute.Disclaimer(isTosAccepted = true), expected = false),
        BlockedRouteModel(
            AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            expected = false,
        ),
        BlockedRouteModel(AppRoute.TangemPayHotWalletOnboarding, expected = false),
        BlockedRouteModel(
            AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink),
            expected = false,
        ),
        BlockedRouteModel(currentRoute = null, expected = false),
    )

    // endregion

    // region reactive observe

    @Test
    fun `GIVEN deeplink and authorized wallet on Wallet WHEN observe THEN push onboarding and clear`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify {
                appRouter.push(
                    route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink),
                    onComplete = any(),
                )
            }
            coVerify { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN deeplink and empty wallets on Home WHEN observe THEN replaceAll onboarding and clear`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — root, not push: the onboarding falls back to Home with `replaceCurrent`.
            verify(exactly = 1) { appRouter.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
            verify(exactly = 0) { appRouter.push(route = any(), onComplete = any()) }
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN deeplink and empty wallets on Stories WHEN observe THEN replaceAll onboarding and clear`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Stories(storyId = "id", screenSource = "src"))

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { appRouter.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN deeplink and empty wallets on forgotten wallet Home WHEN observe THEN does not route`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(
                AppRoute.Home(entryReason = AppRoute.Home.EntryReason.LastWalletForgotten),
            )

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — returning after forgetting the last wallet is not an install entry to hijack.
            verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN deeplink and empty wallets on card scan launch WHEN observe THEN does not route`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(
                AppRoute.Home(launchMode = InitScreenLaunchMode.WithCardScan),
            )

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — the NFC scan started by the launch owns the screen.
            verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN deeplink and empty wallets on CreateWalletStart WHEN observe THEN does not route`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(
                AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            )

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — wallet creation is already under way; the link waits for a start screen.
            verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN routed from Home WHEN route becomes onboarding THEN does not route again`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Act — the store is mocked, so the link still reads as pending on the re-emission.
            currentRoute.value = AppRoute.TangemPayHotWalletOnboarding
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { appRouter.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN referral deeplink and empty wallets on Home WHEN observe THEN replaceAll create wallet, not cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf("referral")
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — referral install skips stories → hot wallet creation; deep link stays as attribution.
            verify {
                appRouter.replaceAll(
                    match<AppRoute> {
                        it is AppRoute.CreateWalletStart && it.mode == AppRoute.CreateWalletStart.Mode.HotWallet
                    },
                    onComplete = any(),
                )
            }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN campaign deeplink and authorized wallet WHEN observe THEN push container, not cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify { appRouter.push(route = AppRoute.TangemPayCampaign(CAMPAIGN_VALUE), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN campaign deeplink and empty wallets WHEN observe THEN replaceAll container, not cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify { appRouter.replaceAll(AppRoute.TangemPayCampaign(CAMPAIGN_VALUE), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN campaign deeplink and toggle disabled WHEN observe THEN does not route, not cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns false
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { appRouter.replaceAll(any(), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN toggle disabled WHEN observe THEN mobile onboarding still routes`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { tangemPayFeatureToggles.isCustomCardsCobrandEnabled } returns false
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify {
                appRouter.push(
                    route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink),
                    onComplete = any(),
                )
            }
            coVerify { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN campaign deeplink on blocked route WHEN observe THEN does not route`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.TangemPayCampaign(CAMPAIGN_VALUE))

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { appRouter.replaceAll(any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN campaign deeplink and authorized wallet on AppSettings WHEN observe THEN push container`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.AppSettings)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                appRouter.push(route = AppRoute.TangemPayCampaign(CAMPAIGN_VALUE), onComplete = any())
            }
        }

    @Test
    fun `GIVEN campaign deeplink and empty wallets on CreateWalletStart WHEN observe THEN does not route`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(CAMPAIGN_VALUE)
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(
                AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            )

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { appRouter.replaceAll(any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN referral deeplink and empty wallets on CreateWalletStart WHEN observe THEN does not route again`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf("referral")
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(
                AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            )

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { appRouter.replaceAll(any(), onComplete = any()) }
        }

    @Test
    fun `GIVEN no stored deeplink WHEN observe THEN does not evaluate`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(null)
        val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

        // Act
        router.observe(backgroundScope, currentRoute)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { userWalletsListRepository.userWalletsSync() }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    // endregion

    // region deferred direct deeplink

    @Test
    fun `GIVEN direct uri deeplink and wallet on Wallet WHEN observe THEN onboard visa handler created`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onboardVisaDeepLink.create(directUri) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
        }

    @Test
    fun `GIVEN direct uri deeplink and empty wallets WHEN observe THEN handled without consulting wallets`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onboardVisaDeepLink.create(directUri) }
            coVerify(exactly = 0) { userWalletsListRepository.userWalletsSync() }
            verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
        }

    @Test
    fun `GIVEN unsupported direct uri in store WHEN observe THEN not handled and not cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(UNSUPPORTED_URI)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { onboardVisaDeepLink.create(any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN direct uri deeplink on non-idle route WHEN observe THEN not handled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Initial)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { onboardVisaDeepLink.create(any()) }
        }

    @Test
    fun `GIVEN direct uri deeplink WHEN route changes twice THEN handled once`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            currentRoute.value = AppRoute.Home()
            currentRoute.value = AppRoute.Wallet
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onboardVisaDeepLink.create(directUri) }
        }

    @Test
    fun `GIVEN direct uri deeplink WHEN handled THEN stored deeplink cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            every { appsFlyerRepository.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
        }

    @Test
    fun `GIVEN direct uri stored again after clear WHEN observe THEN dispatched again`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val stored = MutableStateFlow<String?>(DIRECT_URI)
            every { appsFlyerRepository.observeNavigationDeeplink() } returns stored
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Wallet)

            // Act
            router.observe(backgroundScope, currentRoute)
            stored.value = null
            stored.value = DIRECT_URI
            advanceUntilIdle()

            // Assert
            verify(exactly = 2) { onboardVisaDeepLink.create(directUri) }
            coVerify(exactly = 2) { clearAppsFlyerDeeplinkUseCase() }
        }

    // endregion

    private companion object {
        const val CAMPAIGN_VALUE = "tpay_for_oxford"
        const val DIRECT_URI = "tangem://onboard-visa"
        const val UNSUPPORTED_URI = "https://tangem.com/pay-app"
        val WALLET_ID = UserWalletId("0011")
    }
}