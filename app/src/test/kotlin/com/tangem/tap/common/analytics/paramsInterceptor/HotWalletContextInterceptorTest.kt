package com.tangem.tap.common.analytics.paramsInterceptor

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import org.junit.jupiter.api.Test

internal class HotWalletContextInterceptorTest {

    private val interceptor = HotWalletContextInterceptor()

    @Test
    fun `GIVEN card was scanned event WHEN canBeAppliedTo THEN false`() {
        // Arrange
        val event = Basic.CardWasScanned(source = AnalyticsParam.ScreensSources.Settings)

        // Act
        val actual = interceptor.canBeAppliedTo(event)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN balance loaded event WHEN canBeAppliedTo THEN true`() {
        // Arrange
        val event = Basic.BalanceLoaded(balance = AnalyticsParam.CardBalanceState.Empty, tokensCount = null)

        // Act
        val actual = interceptor.canBeAppliedTo(event)

        // Assert
        assertThat(actual).isTrue()
    }
}