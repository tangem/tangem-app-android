package com.tangem.features.staking.impl.presentation.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.onramp.model.OnrampSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelMarketingDeeplinkTest : StakingModelTestBase() {

    @Test
    fun `GIVEN swap deeplink WHEN onMarketingBannerDeeplink THEN pushes Swap for current token`() = runTest {
        // Arrange
        every { appRouter.push(any(), any()) } just Runs
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        val handled = model.onMarketingBannerDeeplink("tangem://swap")

        // Assert
        assertThat(handled).isTrue()
        verify {
            appRouter.push(
                match {
                    it is AppRoute.Swap &&
                        it.userWalletId == testUserWalletId &&
                        it.fromCryptoCurrency == testCryptoCurrency &&
                        it.screenSource == AnalyticsParam.ScreensSources.Staking.value
                },
                any(),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN buy deeplink WHEN onMarketingBannerDeeplink THEN pushes Onramp for current token`() = runTest {
        // Arrange
        every { appRouter.push(any(), any()) } just Runs
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        val handled = model.onMarketingBannerDeeplink("tangem://buy")

        // Assert
        assertThat(handled).isTrue()
        verify {
            appRouter.push(
                match {
                    it is AppRoute.Onramp &&
                        it.userWalletId == testUserWalletId &&
                        it.currency == testCryptoCurrency &&
                        it.source == OnrampSource.MARKETING_BANNER
                },
                any(),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN external deeplink WHEN onMarketingBannerDeeplink THEN not handled and no navigation`() = runTest {
        // Arrange
        every { appRouter.push(any(), any()) } just Runs
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        val handled = model.onMarketingBannerDeeplink("https://tangem.com/promo")

        // Assert
        assertThat(handled).isFalse()
        verify(exactly = 0) { appRouter.push(any(), any()) }

        model.onDestroy()
    }
}