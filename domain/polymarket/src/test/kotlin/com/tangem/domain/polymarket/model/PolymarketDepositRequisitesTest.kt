package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketContracts
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PolymarketDepositRequisitesTest {

    @Test
    fun `GIVEN polygon usdc listed WHEN default read THEN it is the polygon usdc target`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(networks = listOf(polygon(), base()))

        // Act
        val actual = requisites.default

        // Assert
        assertThat(actual?.token?.address).isEqualTo(PolymarketContracts.USDC_TOKEN_ADDRESS)
        assertThat(actual?.address).isEqualTo(EVM_ADDRESS)
    }

    @Test
    fun `GIVEN polygon usdc absent WHEN default read THEN it is null`() {
        // Arrange
        val requisites = PolymarketDepositRequisites(networks = listOf(base()))

        // Act
        val actual = requisites.default

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN token address in another case WHEN targetFor called THEN the token is found`() {
        // Arrange
        val network = polygon()

        // Act
        val actual = network.targetFor(tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS.uppercase())

        // Assert
        assertThat(actual?.token?.symbol).isEqualTo("USDC")
    }

    @Test
    fun `GIVEN two tokens with different minimums WHEN read THEN each keeps its own`() {
        // Arrange
        val network = polygon()

        // Act
        val usdc = network.targetFor(tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS)
        val other = network.targetFor(tokenAddress = OTHER_TOKEN_ADDRESS)

        // Assert
        assertThat(usdc?.token?.minCheckoutUsd).isEqualTo(BigDecimal("2"))
        assertThat(other?.token?.minCheckoutUsd).isEqualTo(BigDecimal("9"))
    }

    private fun polygon() = PolymarketDepositNetwork(
        chainId = "137",
        chainName = "Polygon",
        family = PolymarketChainFamily.EVM,
        address = EVM_ADDRESS,
        tokens = listOf(
            PolymarketDepositToken(
                symbol = "USDC",
                address = PolymarketContracts.USDC_TOKEN_ADDRESS,
                decimals = 6,
                minCheckoutUsd = BigDecimal("2"),
            ),
            PolymarketDepositToken(
                symbol = "WBTC",
                address = OTHER_TOKEN_ADDRESS,
                decimals = 8,
                minCheckoutUsd = BigDecimal("9"),
            ),
        ),
    )

    private fun base() = PolymarketDepositNetwork(
        chainId = "8453",
        chainName = "Base",
        family = PolymarketChainFamily.EVM,
        address = EVM_ADDRESS,
        tokens = listOf(
            PolymarketDepositToken(symbol = "USDC", address = OTHER_TOKEN_ADDRESS, decimals = 6, minCheckoutUsd = BigDecimal("2")),
        ),
    )

    private companion object {
        const val EVM_ADDRESS = "0x4038A7a1"
        const val OTHER_TOKEN_ADDRESS = "0x1bfd67037b42cf73acf2047067bd4f2c47d9bfd6"
    }
}