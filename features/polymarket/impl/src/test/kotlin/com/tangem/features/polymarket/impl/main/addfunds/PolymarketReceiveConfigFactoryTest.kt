package com.tangem.features.polymarket.impl.main.addfunds

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketReceiveConfigFactoryTest {

    @Test
    fun `GIVEN a token with a minimum WHEN created THEN the limits notification is shown`() {
        // Arrange
        val target = target(minCheckoutUsd = BigDecimal("2"))

        // Act
        val actual = create(target)

        // Assert
        assertThat(actual.tokenReceiveNotification.map { it.title })
            .containsExactly(R.string.prediction_add_funds_notification_limits_title)
    }

    @Test
    fun `GIVEN a token without a minimum WHEN created THEN no limits notification is shown`() {
        // Arrange
        val target = target(minCheckoutUsd = null)

        // Act
        val actual = create(target)

        // Assert
        assertThat(actual.tokenReceiveNotification).isEmpty()
    }

    @Test
    fun `GIVEN a token without a minimum WHEN created THEN the deposit address is still offered`() {
        // Arrange
        val target = target(minCheckoutUsd = null)

        // Act
        val actual = create(target)

        // Assert
        assertThat(actual.receiveAddress.map { it.value }).containsExactly(NETWORK_ADDRESS)
    }

    private fun create(target: PolymarketDepositTarget) = PolymarketReceiveConfigFactory.create(
        cryptoCurrency = mockk(relaxed = true),
        userWalletId = USER_WALLET_ID,
        target = target,
    )

    private fun target(minCheckoutUsd: BigDecimal?) = PolymarketDepositTarget(
        network = PolymarketDepositNetwork(
            chainId = PolymarketContracts.CHAIN_ID.toString(),
            chainName = "Polygon",
            family = PolymarketChainFamily.EVM,
            address = NETWORK_ADDRESS,
            tokens = emptyList(),
        ),
        token = PolymarketDepositToken(
            symbol = "USDC",
            address = PolymarketContracts.USDC_TOKEN_ADDRESS,
            decimals = 6,
            minCheckoutUsd = minCheckoutUsd,
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        const val NETWORK_ADDRESS = "0xrequisites"
    }
}