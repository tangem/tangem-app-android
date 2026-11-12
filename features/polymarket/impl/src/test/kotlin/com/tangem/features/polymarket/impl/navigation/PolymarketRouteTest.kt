package com.tangem.features.polymarket.impl.navigation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class PolymarketRouteTest {

    @Test
    fun `GIVEN entry at the root WHEN settledUserWalletId THEN null is returned`() {
        // Act
        val actual = listOf(PolymarketRoute.Entry).settledUserWalletId

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN onboarding at the root WHEN settledUserWalletId THEN its wallet is returned`() {
        // Act
        val actual = listOf(PolymarketRoute.Onboarding(USER_WALLET_ID)).settledUserWalletId

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET_ID)
    }

    @Test
    fun `GIVEN a screen pushed above main WHEN settledUserWalletId THEN the wallet of main is returned`() {
        // Arrange
        val stack = listOf(
            PolymarketRoute.Main(USER_WALLET_ID),
            PolymarketRoute.FastTradingSettings(OTHER_WALLET_ID),
        )

        // Act
        val actual = stack.settledUserWalletId

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET_ID)
    }

    @Test
    fun `GIVEN an empty stack WHEN settledUserWalletId THEN null is returned`() {
        // Act
        val actual = emptyList<PolymarketRoute>().settledUserWalletId

        // Assert
        assertThat(actual).isNull()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
    }
}