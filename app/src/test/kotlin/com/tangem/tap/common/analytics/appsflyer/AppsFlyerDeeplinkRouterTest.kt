package com.tangem.tap.common.analytics.appsflyer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppsFlyerDeeplinkRouterTest {

    private val appsFlyerStore: AppsFlyerStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)

    private val router = AppsFlyerDeeplinkRouter(
        appsFlyerStore = appsFlyerStore,
        userWalletsListRepository = userWalletsListRepository,
        clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
        appRouter = appRouter,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            appsFlyerStore,
            userWalletsListRepository,
            clearAppsFlyerDeeplinkUseCase,
            appRouter,
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
    fun `GIVEN deeplink and empty wallets on Home WHEN observe THEN replaceAll hot wallet onboarding and clear`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val currentRoute = MutableStateFlow<AppRoute?>(AppRoute.Home())

            // Act
            router.observe(backgroundScope, currentRoute)
            advanceUntilIdle()

            // Assert — unauthorized path opens onboarding as the root (skips Home) and consumes the deep link.
            verify { appRouter.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
            coVerify { clearAppsFlyerDeeplinkUseCase() }
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
}