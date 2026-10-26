package com.tangem.tap.common.extensions

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.paramsInterceptor.HotWalletContextInterceptor
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class AnalyticsContextTest {

    @AfterEach
    fun eraseContexts() {
        Analytics.removeParamsInterceptor(LinkedCardContextInterceptor.id())
        Analytics.removeParamsInterceptor(HotWalletContextInterceptor.id())
    }

    @Test
    fun `GIVEN hot wallet context WHEN hot wallet context added and removed THEN hot wallet context restored`() {
        // Arrange
        val walletContext = HotWalletContextInterceptor()
        Analytics.addParamsInterceptor(walletContext)

        // Act
        Analytics.addHotWalletContext()
        Analytics.removeContext()

        // Assert
        assertThat(Analytics.removeParamsInterceptor(HotWalletContextInterceptor.id())).isSameInstanceAs(walletContext)
    }

    @Test
    fun `GIVEN card context WHEN hot wallet context added and removed THEN card context restored`() {
        // Arrange
        val cardContext = mockk<LinkedCardContextInterceptor> {
            every { id() } returns LinkedCardContextInterceptor.id()
        }
        Analytics.addParamsInterceptor(cardContext)

        // Act
        Analytics.addHotWalletContext()
        Analytics.removeContext()

        // Assert
        assertThat(Analytics.removeParamsInterceptor(LinkedCardContextInterceptor.id())).isSameInstanceAs(cardContext)
        assertThat(Analytics.removeParamsInterceptor(HotWalletContextInterceptor.id())).isNull()
    }

    @Test
    fun `GIVEN no context WHEN hot wallet context added and removed THEN no context remains`() {
        // Act
        Analytics.addHotWalletContext()
        Analytics.removeContext()

        // Assert
        assertThat(Analytics.removeParamsInterceptor(LinkedCardContextInterceptor.id())).isNull()
        assertThat(Analytics.removeParamsInterceptor(HotWalletContextInterceptor.id())).isNull()
    }
}