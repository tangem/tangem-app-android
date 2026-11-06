package com.tangem.features.staking.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import org.junit.jupiter.api.Test

internal class StakingParamsInterceptorTest {

    private val interceptor = StakingParamsInterceptor(tokenSymbol = "SOL")

    @Test
    fun `GIVEN staking event WHEN canBeAppliedTo THEN true`() {
        // Act & Assert
        assertThat(interceptor.canBeAppliedTo(StakingAnalyticsEvent.ButtonMax())).isTrue()
    }

    @Test
    fun `GIVEN non staking event WHEN canBeAppliedTo THEN false`() {
        // Act & Assert
        assertThat(interceptor.canBeAppliedTo(AnalyticsEvent(category = "Other", event = "e"))).isFalse()
    }

    @Test
    fun `GIVEN params WHEN intercept THEN token param set and other keys kept`() {
        // Arrange
        val params = mutableMapOf("k" to "v", AnalyticsParam.TOKEN_PARAM to "OLD")

        // Act
        interceptor.intercept(params)

        // Assert
        assertThat(params).isEqualTo(mapOf("k" to "v", "Token" to "SOL"))
    }
}