package com.tangem.tap.common.analytics.appsflyer

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppsFlyerDeeplinkRouterTest {

    private val appsFlyerStore: AppsFlyerStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val onboardVisaDeepLink: OnboardVisaDeepLinkHandler.Factory = mockk(relaxed = true)
    private val urisByString = listOf(DIRECT_URI, UNSUPPORTED_URI).associateWith { mockk<Uri>() }
    private val directUri: Uri = urisByString.getValue(DIRECT_URI)

    private val router = AppsFlyerDeeplinkRouter(
        appsFlyerStore = appsFlyerStore,
        userWalletsListRepository = userWalletsListRepository,
        clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
        appRouter = appRouter,
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
            appsFlyerStore,
            userWalletsListRepository,
            clearAppsFlyerDeeplinkUseCase,
            appRouter,
            onboardVisaDeepLink,
        )
        // Happy baseline: deep link pending, authorized.
        every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf("tpay_mobileonboard")
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
    }

    // region idle entry point

    @ParameterizedTest
    @ProvideTestModels
    fun isIdle(model: IdleModel) {
        assertThat(isIdleEntryPoint(model.currentRoute)).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<IdleModel> = listOf(
        // Idle entry screens — a deep link may route from here.
        IdleModel(AppRoute.Home(), expected = true),
        IdleModel(AppRoute.Stories(storyId = "id", screenSource = "src"), expected = true),
        IdleModel(AppRoute.Wallet, expected = true),
        // In-progress / already-on-onboarding / startup routes — must not be interrupted.
        IdleModel(AppRoute.Initial, expected = false),
        IdleModel(AppRoute.Disclaimer(isTosAccepted = true), expected = false),
        IdleModel(AppRoute.TangemPayHotWalletOnboarding, expected = false),
        IdleModel(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.MobileOnboardingDeeplink), expected = false),
        IdleModel(currentRoute = null, expected = false),
    )

    data class IdleModel(val currentRoute: AppRoute?, val expected: Boolean)

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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf("referral")
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
    fun `GIVEN no stored deeplink WHEN observe THEN does not evaluate`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(null)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(UNSUPPORTED_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns flowOf(DIRECT_URI)
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
            every { appsFlyerStore.observeNavigationDeeplink() } returns stored
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
        const val DIRECT_URI = "tangem://onboard-visa"
        const val UNSUPPORTED_URI = "https://tangem.com/pay-app"
    }
}