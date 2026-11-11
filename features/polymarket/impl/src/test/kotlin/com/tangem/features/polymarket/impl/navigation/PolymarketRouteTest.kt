package com.tangem.features.polymarket.impl.navigation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class PolymarketRouteTest {

    @Test
    fun `GIVEN entry route WHEN userWalletIdOrNull THEN null is returned`() {
        // Act
        val actual = PolymarketRoute.Entry.userWalletIdOrNull

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN wallet scoped route WHEN userWalletIdOrNull THEN its wallet is returned`() {
        // Act
        val actual = PolymarketRoute.FastTradingSettings(USER_WALLET_ID).userWalletIdOrNull

        // Assert
        assertThat(actual).isEqualTo(USER_WALLET_ID)
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}