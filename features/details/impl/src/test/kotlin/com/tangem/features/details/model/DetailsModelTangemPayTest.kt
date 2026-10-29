package com.tangem.features.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DetailsModelTangemPayTest : DetailsModelTestBase() {

    @Test
    fun `GIVEN compatible wallets WHEN init THEN tangem pay item added and analytics sent`() = runTest {
        // Arrange
        coEvery {
            tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
        } returns listOf(wallet1)

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        verify { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.PermanentButtonShowed>()) }
        verify { itemsBuilder.addTangemPayItem(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no compatible wallets WHEN init THEN tangem pay item not added`() = runTest {
        // Arrange
        coEvery { tangemPayEligibilityManager.getCompatibleWalletsIds(any()) } returns emptyList()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { itemsBuilder.addTangemPayItem(any(), any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.PermanentButtonShowed>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN item shown AND account created WHEN resumed THEN tangem pay item removed`() = runTest {
        // Arrange
        coEvery {
            tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
        } returnsMany listOf(listOf(wallet1), emptyList())
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { itemsBuilder.addTangemPayItem(any(), any()) }
        verify(exactly = 1) { itemsBuilder.removeTangemPayItem(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN item shown WHEN resumed again THEN analytics not resent`() = runTest {
        // Arrange
        coEvery {
            tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
        } returns listOf(wallet1)
        every { itemsBuilder.hasTangemPayItem(any()) } returnsMany listOf(false, true)
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.PermanentButtonShowed>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN item shown WHEN item clicked THEN analytics sent and navigates to onboarding`() = runTest {
        // Arrange
        coEvery {
            tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
        } returns listOf(wallet1)
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        onTangemPaySlot.captured.invoke()
        advanceUntilIdle()

        // Assert
        verify { analyticsEventHandler.send(any<TangemPayAnalyticsEvents.DetailsVisaPermanentButtonClicked>()) }
        verify {
            router.push(
                route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.FromBannerInSettings),
                onComplete = any(),
            )
        }
        verify(exactly = 0) { itemsBuilder.removeTangemPayItem(any()) }
        model.onDestroy()
    }
}