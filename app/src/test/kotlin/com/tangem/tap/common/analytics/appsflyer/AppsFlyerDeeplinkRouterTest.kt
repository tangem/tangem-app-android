package com.tangem.tap.common.analytics.appsflyer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    private val router = AppsFlyerDeeplinkRouter(
        appsFlyerRepository = appsFlyerRepository,
        userWalletsListRepository = userWalletsListRepository,
        clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
        appRouter = appRouter,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            appsFlyerRepository,
            userWalletsListRepository,
            clearAppsFlyerDeeplinkUseCase,
            appRouter,
            tangemPayFeatureToggles,
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
        BlockedRouteModel(AppRoute.Wallet, expected = false),
        BlockedRouteModel(AppRoute.AppSettings, expected = false),
        BlockedRouteModel(
            AppRoute.CreateWalletStart(mode = AppRoute.CreateWalletStart.Mode.HotWallet),
            expected = false,
        ),
        BlockedRouteModel(currentRoute = null, expected = false),
    )

    // endregion

    // region reactive observe

    @Test
    fun `GIVEN deeplink and authorized wallet on Wallet WHEN observe THEN push onboarding and clear`() = runTest(UnconfinedTestDispatcher()) {
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
    fun `GIVEN deeplink and empty wallets on Home WHEN observe THEN stories kept and deeplink not consumed`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — the intro stories must not be skipped: Home consumes the deep link on "Get started".
            verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
            verify(exactly = 0) { appRouter.push(route = any(), onComplete = any()) }
            coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase() }
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

    private companion object {
        const val CAMPAIGN_VALUE = "tpay_for_oxford"
        val WALLET_ID = UserWalletId("0011")
    }
}