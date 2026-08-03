package com.tangem.features.polymarket.impl.onboarding.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.ResolvePolymarketEntryUseCase
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PolymarketOnboardingModelTest {

    private val resolvePolymarketEntryUseCase: ResolvePolymarketEntryUseCase = mockk()
    private val router: Router = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")
    private val params = PolymarketComponent.Params(userWalletId = userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(resolvePolymarketEntryUseCase, router)
    }

    @Test
    fun `GIVEN resolution fails WHEN model created THEN state is Failed AND nothing is navigated`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketOnboardingError.Network.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(PolymarketOnboardingUM.Failed::class.java)
        coVerify(exactly = 1) { resolvePolymarketEntryUseCase(userWalletId) }
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Trade WHEN model created THEN the feed is opened in trading mode`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.Trade.right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is ReadOnly WHEN model created THEN the feed is opened in read-only mode`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.ReadOnly.right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.READ_ONLY)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is Onboard WHEN model created THEN state is Welcome AND nothing is navigated`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns
            PolymarketEntry.Onboard(status = PolymarketWalletStatus.NOT_CREATED).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(PolymarketOnboardingUM.Welcome)
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry is RegionBlocked WHEN model created THEN state is RegionBlocked AND nothing is navigated`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.RegionBlocked.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(PolymarketOnboardingUM.RegionBlocked)
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN state is RegionBlocked WHEN the sheet is dismissed THEN the feed is opened in read-only mode`() =
        runTest {
            // Arrange
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } returns PolymarketEntry.RegionBlocked.right()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onRegionRestrictionsDismiss()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.READ_ONLY)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN state is Failed WHEN retry is tapped THEN the entry is resolved again`() = runTest {
        // Arrange
        coEvery { resolvePolymarketEntryUseCase(userWalletId) } returnsMany listOf(
            PolymarketOnboardingError.Network.left(),
            PolymarketEntry.Trade.right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketOnboardingUM.Failed).onRetryClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { resolvePolymarketEntryUseCase(userWalletId) }
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN retry is tapped twice WHEN the superseded attempt fails THEN the fresh resolution survives`() =
        runTest {
            // Arrange
            var attempt = 0
            coEvery { resolvePolymarketEntryUseCase(userWalletId) } coAnswers {
                when (++attempt) {
                    1 -> PolymarketOnboardingError.Network.left()
                    2 -> {
                        runCatching { delay(SUPERSEDED_ATTEMPT_DELAY_MILLIS) }
                        PolymarketOnboardingError.Network.left()
                    }
                    else -> PolymarketEntry.Trade.right()
                }
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val failed = model.uiState.value as PolymarketOnboardingUM.Failed

            // Act
            failed.onRetryClick()
            runCurrent()
            failed.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(PolymarketOnboardingUM.Loading)
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Main(accessMode = PolymarketAccessMode.TRADING)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    private fun createModel(testScope: TestScope): PolymarketOnboardingModel = PolymarketOnboardingModel(
        paramsContainer = MutableParamsContainer(params),
        router = router,
        resolvePolymarketEntryUseCase = resolvePolymarketEntryUseCase,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
    )

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

    private companion object {
        const val SUPERSEDED_ATTEMPT_DELAY_MILLIS = 1_000L
    }
}