package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.availableNetwork
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.currencyStatus
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.offrampOptions
import com.tangem.features.tangempay.withdraw.bank.offrampSource
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class BankWithdrawOptionsFactoryTest {

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val usdtPolygon = paymentToken(contractAddress = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", symbol = "USDT")
    private val usdtBase = paymentToken(
        networkId = "base",
        networkName = "Base",
        contractAddress = "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2",
        symbol = "USDT",
    )
    private val daiPolygon = paymentToken(
        contractAddress = "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063",
        symbol = "DAI",
    )

    private val bothSources = offrampOptions(
        sources = listOf(offrampSource(), offrampSource("Base", BASE_CHAIN_ID, USDC_BASE)),
    )

    @Test
    fun `GIVEN accepted tokens with balance WHEN create THEN kept in the account's network order`() {
        // Arrange
        val networks = listOf(
            availableNetwork(token = usdcPolygon, fiatBalance = BigDecimal("2325.55")),
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("12325.55")),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(bothSources, networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources?.map { it.token }).containsExactly(usdcPolygon, usdcBase).inOrder()
        assertThat(actual?.currency).isEqualTo(Currency.getInstance("USD"))
        assertThat(actual?.minAmount).isEqualTo(BigDecimal("1.00"))
        assertThat(actual?.maxAmount).isEqualTo(BigDecimal("10000.00"))
    }

    @Test
    fun `GIVEN tokens without balance WHEN create THEN dropped whether accepted or not`() {
        // Arrange
        val networks = listOf(
            availableNetwork(
                token = usdtPolygon,
                fiatBalance = BigDecimal.ZERO,
                extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
            ),
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal.ZERO),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(bothSources, networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources).containsExactly(
            bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("100")),
        )
    }

    @Test
    fun `GIVEN tokens not accepted with balance WHEN create THEN listed unavailable inside their symbol group`() {
        // Arrange
        val networks = listOf(
            availableNetwork(
                token = usdtPolygon,
                fiatBalance = BigDecimal("5"),
                extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
            ),
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("40")),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources).containsExactly(
            bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("100")),
            bankWithdrawSource(
                token = usdcBase,
                chainId = BASE_CHAIN_ID,
                fiatBalance = BigDecimal("40"),
                isAvailable = false,
            ),
            bankWithdrawSource(token = usdtPolygon, fiatBalance = BigDecimal("5"), isAvailable = false),
        ).inOrder()
    }

    @Test
    fun `GIVEN only tokens the off-ramp does not accept WHEN create THEN null`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdtPolygon, fiatBalance = BigDecimal("5")))

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN contract address in different case WHEN create THEN still accepted`() {
        // Arrange
        val token = paymentToken(contractAddress = "0x3C499C542CEF5E3811E1192CE70D8CC03D5C3359")
        val networks = listOf(availableNetwork(token = token))

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources?.single()?.token).isEqualTo(token)
    }

    @Test
    fun `GIVEN every token without balance WHEN create THEN null`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdcPolygon, fiatBalance = BigDecimal.ZERO))

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no destination WHEN create THEN null`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdcPolygon))

        // Act
        val actual = BankWithdrawOptionsFactory.create(
            options = offrampOptions(destinations = emptyList()),
            networks = networks,
            balanceCurrencyCode = "USD",
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN disabled and not issued networks WHEN create THEN null`() {
        // Arrange
        val networks: List<PaymentNetworkStatus> = listOf(
            PaymentNetworkStatus.Disabled(
                network = usdcBase.network,
                chainId = BASE_CHAIN_ID,
                cryptoCurrencies = listOf(usdcBase),
            ),
            PaymentNetworkStatus.NotIssued(network = mockk(), chainId = BASE_CHAIN_ID),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN account without currency WHEN create THEN the rail fee currency is used`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdcPolygon))

        // Act
        val actual = BankWithdrawOptionsFactory.create(
            options = offrampOptions(destinations = listOf(offrampDestination())),
            networks = networks,
            balanceCurrencyCode = null,
        )

        // Assert
        assertThat(actual?.currency).isEqualTo(Currency.getInstance("USD"))
    }

    @Test
    fun `GIVEN sources where USDC is not first WHEN resolved THEN USDC comes first`() {
        // Arrange
        val networks = listOf(
            availableNetwork(
                token = usdtPolygon,
                extraStatuses = listOf(currencyStatus(usdcPolygon, fiatBalance = BigDecimal("100"))),
            ),
        )
        val options = offrampOptions(
            sources = listOf(offrampSource(contractAddress = usdtPolygon.contractAddress), offrampSource()),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(options, networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources?.map { it.token }).containsExactly(usdcPolygon, usdtPolygon).inOrder()
    }

    @Test
    fun `GIVEN several symbols WHEN resolved THEN symbols keep first-seen order`() {
        // Arrange
        val networks = listOf(
            availableNetwork(
                token = usdtPolygon,
                extraStatuses = listOf(currencyStatus(daiPolygon, fiatBalance = BigDecimal("100"))),
            ),
            availableNetwork(token = usdtBase, chainId = BASE_CHAIN_ID),
        )
        val options = offrampOptions(
            sources = listOf(
                offrampSource(contractAddress = usdtPolygon.contractAddress),
                offrampSource(contractAddress = daiPolygon.contractAddress),
                offrampSource("Base", BASE_CHAIN_ID, usdtBase.contractAddress),
            ),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(options, networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources?.map { it.token }).containsExactly(usdtPolygon, usdtBase, daiPolygon).inOrder()
    }

    @Test
    fun `GIVEN one symbol on several networks WHEN resolved THEN backend order kept`() {
        // Arrange
        val networks = listOf(
            availableNetwork(token = usdcBase, chainId = BASE_CHAIN_ID),
            availableNetwork(token = usdcPolygon),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(bothSources, networks, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual?.sources?.map { it.token }).containsExactly(usdcBase, usdcPolygon).inOrder()
    }

    @Test
    fun `GIVEN unknown account currency WHEN create THEN the rail fee currency is used`() {
        // Arrange
        val networks = listOf(availableNetwork(token = usdcPolygon))

        // Act
        val actual = BankWithdrawOptionsFactory.create(offrampOptions(), networks, balanceCurrencyCode = "XXXX")

        // Assert
        assertThat(actual?.currency).isEqualTo(Currency.getInstance("USD"))
    }
}