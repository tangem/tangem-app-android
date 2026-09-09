package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.availableNetwork
import com.tangem.features.tangempay.withdraw.bank.currencyStatus
import com.tangem.features.tangempay.withdraw.bank.offrampSource
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankWithdrawSourceResolverTest {

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val usdtPolygon = paymentToken(contractAddress = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", symbol = "USDT")

    @Test
    fun `GIVEN accepted token with balance WHEN resolve THEN available with backend order`() {
        // Arrange
        val networks = listOf(
            availableNetwork(token = usdcPolygon, fiatBalance = BigDecimal("2325.55")),
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("12325.55")),
        )
        val sources = listOf(offrampSource(), offrampSource("Base", BASE_CHAIN_ID, USDC_BASE))

        // Act
        val actual = BankWithdrawSourceResolver.resolve(networks, sources)

        // Assert
        assertThat(actual.map { it.token to it.isAvailable }).containsExactly(
            usdcPolygon to true,
            usdcBase to true,
        ).inOrder()
        assertThat(BankWithdrawSourceResolver.default(actual)?.token).isEqualTo(usdcPolygon)
    }

    @Test
    fun `GIVEN token not in sources or without balance WHEN resolve THEN unavailable and listed last`() {
        // Arrange
        val networks = listOf(
            availableNetwork(
                token = usdtPolygon,
                fiatBalance = BigDecimal("5"),
                extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
            ),
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal.ZERO),
        )
        val sources = listOf(offrampSource(), offrampSource("Base", BASE_CHAIN_ID, USDC_BASE))

        // Act
        val actual = BankWithdrawSourceResolver.resolve(networks, sources)

        // Assert
        assertThat(actual.map { it.token to it.isAvailable }).containsExactly(
            usdcPolygon to true,
            usdtPolygon to false,
            usdcBase to false,
        ).inOrder()
    }

    @Test
    fun `GIVEN contract address in different case WHEN resolve THEN still accepted`() {
        // Arrange
        val networks = listOf(availableNetwork(token = paymentToken(contractAddress = "0x3C499C542CEF5E3811E1192CE70D8CC03D5C3359")))

        // Act
        val actual = BankWithdrawSourceResolver.resolve(networks, listOf(offrampSource()))

        // Assert
        assertThat(actual.single().isAvailable).isTrue()
    }

    @Test
    fun `GIVEN no available token WHEN default THEN first source`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdcPolygon, fiatBalance = BigDecimal.ZERO))

        // Act
        val actual = BankWithdrawSourceResolver.resolve(networks, listOf(offrampSource()))

        // Assert
        assertThat(BankWithdrawSourceResolver.default(actual)).isEqualTo(actual.first())
        assertThat(actual.first().isAvailable).isFalse()
    }

    @Test
    fun `GIVEN disabled and not issued networks WHEN resolve THEN skipped`() {
        // Arrange
        val networks: List<PaymentNetworkStatus> = listOf(
            PaymentNetworkStatus.Disabled(network = usdcBase.network, cryptoCurrencies = listOf(usdcBase)),
            PaymentNetworkStatus.NotIssued(network = mockk()),
        )

        // Act
        val actual = BankWithdrawSourceResolver.resolve(networks, listOf(offrampSource()))

        // Assert
        assertThat(actual).isEmpty()
        assertThat(BankWithdrawSourceResolver.default(actual)).isNull()
    }
}