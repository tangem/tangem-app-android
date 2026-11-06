package com.tangem.domain.yield.supply.models.status

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModuleStatusTest {

    private val path = Network.DerivationPath.Card("m/44'/60'/0'/0/0")
    private val networkId = Network.ID(value = "ethereum", derivationPath = path)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN toYieldSupplyStatus THEN every status field is mapped`(model: StatusModel) {
        // Act
        val actual = model.status.toYieldSupplyStatus()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN token with checksummed address WHEN findFor THEN status with lowercase address is found`() {
        // Arrange
        val status = status(isActive = true, effectiveProtocolBalance = BigDecimal.TEN)
        val otherPath = status.copy(networkId = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None))

        // Act
        val actual = listOf(otherPath, status).findFor(token(path = path))

        // Assert
        assertThat(actual).isEqualTo(status)
    }

    @Test
    fun `GIVEN token from another derivation WHEN findFor THEN nothing is found`() {
        // Act
        val actual = listOf(status(isActive = false, effectiveProtocolBalance = null))
            .findFor(token(path = Network.DerivationPath.Custom("m/44'/60'/1'/0/0")))

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        StatusModel(
            status = status(isActive = true, effectiveProtocolBalance = BigDecimal("12.5")),
            expected = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("12.5"),
            ),
        ),
        StatusModel(
            status = status(isActive = false, effectiveProtocolBalance = null),
            expected = YieldSupplyStatus(
                isActive = false,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            ),
        ),
    )

    private fun status(isActive: Boolean, effectiveProtocolBalance: BigDecimal?) = YieldModuleStatus(
        networkId = networkId,
        tokenAddress = TOKEN_ADDRESS.lowercase(),
        isActive = isActive,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = effectiveProtocolBalance,
        source = StatusSource.ACTUAL,
    )

    private fun token(path: Network.DerivationPath): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = path),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = path,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
                suffix = CryptoCurrency.ID.Suffix.RawID(TOKEN_ADDRESS),
            ),
            network = network,
            name = "USD Coin",
            symbol = "USDC",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = TOKEN_ADDRESS,
        )
    }

    data class StatusModel(val status: YieldModuleStatus, val expected: YieldSupplyStatus)

    private companion object {
        const val TOKEN_ADDRESS = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
    }
}