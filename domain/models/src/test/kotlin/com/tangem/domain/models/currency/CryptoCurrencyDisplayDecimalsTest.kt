package com.tangem.domain.models.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class CryptoCurrencyDisplayDecimalsTest {

    @Test
    fun `GIVEN coin without display decimals WHEN created THEN display decimals equal decimals`() {
        // Act
        val coin = CryptoCurrency.Coin(
            id = coinId,
            network = network,
            name = "Arc",
            symbol = "USDC",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )

        // Assert
        assertThat(coin.displayDecimals).isEqualTo(18)
    }

    @Test
    fun `GIVEN coin with fewer display decimals WHEN created THEN both values are kept`() {
        // Act
        val coin = coin(decimals = 18, displayDecimals = 6)

        // Assert
        assertThat(coin.decimals).isEqualTo(18)
        assertThat(coin.displayDecimals).isEqualTo(6)
    }

    @Test
    fun `GIVEN display decimals above decimals WHEN coin created THEN it fails`() {
        // Act
        val error = runCatching { coin(decimals = 6, displayDecimals = 18) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN token WHEN display decimals requested THEN they equal decimals`() {
        // Arrange
        val token = CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = "arc"),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0x1"),
            ),
            network = network,
            name = "Token",
            symbol = "TKN",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0x1",
        )

        // Assert
        assertThat(token.displayDecimals).isEqualTo(8)
    }

    @Test
    fun `GIVEN coin with display decimals WHEN serialized polymorphically and restored THEN it is equal`() {
        // Arrange
        val coin: CryptoCurrency = coin(decimals = 18, displayDecimals = 6)

        // Act
        val restored = Json.decodeFromString<CryptoCurrency>(Json.encodeToString(coin))

        // Assert
        assertThat(restored).isEqualTo(coin)
    }

    @Test
    fun `GIVEN persisted coin json without display decimals WHEN decoded THEN display decimals equal decimals`() {
        // Arrange
        val json = Json.encodeToString<CryptoCurrency>(coin(decimals = 18, displayDecimals = 18))
            .replace(""","displayDecimals":18""", "")

        // Act
        val restored = Json.decodeFromString<CryptoCurrency>(json)

        // Assert
        assertThat(restored.displayDecimals).isEqualTo(18)
    }

    @Test
    fun `GIVEN coin json with display decimals above decimals WHEN decoded THEN it fails`() {
        // Arrange
        val json = Json.encodeToString<CryptoCurrency>(coin(decimals = 18, displayDecimals = 6))
            .replace(""""displayDecimals":6""", """"displayDecimals":19""")

        // Act
        val error = runCatching { Json.decodeFromString<CryptoCurrency>(json) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun coin(decimals: Int, displayDecimals: Int) = CryptoCurrency.Coin(
        id = coinId,
        network = network,
        name = "Arc",
        symbol = "USDC",
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
        displayDecimals = displayDecimals,
    )

    private val coinId = CryptoCurrency.ID(
        prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
        body = CryptoCurrency.ID.Body.NetworkId(rawId = "arc"),
        suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin"),
    )

    private val network = Network(
        id = Network.ID(value = "arc", derivationPath = Network.DerivationPath.None),
        name = "Arc",
        currencySymbol = "USDC",
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}