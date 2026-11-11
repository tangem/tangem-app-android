package com.tangem.tap.common.analytics.paramsInterceptor

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN import wallet request WHEN canBeAppliedTo THEN applied only when the selected wallet is related`(
        isSelectedWalletUnrelated: Boolean,
    ) {
        // Arrange
        val event = OnboardingAnalyticsEvent.Backup.ImportWalletRequest(
            cloudBackup = AnalyticsParam.CloudBackupAvailability.Available,
            isSelectedWalletUnrelated = isSelectedWalletUnrelated,
        )

        // Act
        val actual = interceptor.canBeAppliedTo(event)

        // Assert
        assertThat(actual).isEqualTo(!isSelectedWalletUnrelated)
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