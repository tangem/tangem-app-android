package com.tangem.features.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelTangemPayTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN eligible wallets WHEN init THEN tangem pay item added and analytics sent`() = runTest {
        // Arrange
        coEvery {
            tangemPayEligibilityManager.getEligibleWallets(
                shouldExcludePaeraCustomers = true,
                entryPoint = TangemPayEntryPoint.DETAILS,
            )
        } returns listOf(hotWallet(wallet1))

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        verify { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.PermanentButtonShowed>()) }
        verify { itemsBuilder.addTangemPayItem(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no eligible wallets WHEN init THEN tangem pay item not added`() = runTest {
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns emptyList()

        val model = createModel(this)
        advanceUntilIdle()

        verify(exactly = 0) { itemsBuilder.addTangemPayItem(any(), any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.PermanentButtonShowed>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN tangem pay available WHEN item clicked THEN navigates to onboarding`() = runTest {
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns listOf(hotWallet(wallet1))
        coEvery { tangemPayEligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DETAILS) } returns true

        val model = createModel(this)
        advanceUntilIdle()
        onTangemPaySlot.captured.invoke()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.DetailsVisaPermanentButtonClicked>()) }
        verify {
            router.push(
                route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.FromBannerInSettings),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN tangem pay unavailable WHEN item clicked THEN item removed and no navigation`() = runTest {
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns listOf(hotWallet(wallet1))
        coEvery { tangemPayEligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DETAILS) } returns false

        val model = createModel(this)
        advanceUntilIdle()
        onTangemPaySlot.captured.invoke()
        advanceUntilIdle()

        verify { itemsBuilder.removeTangemPayItem(any()) }
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }
}