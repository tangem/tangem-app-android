package com.tangem.domain.models.account

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Round-trips every [PaymentNetworkStatus] subtype through kotlinx-serialization JSON. Guards the
 * polymorphic registry: a subtype missing its own `@Serializable` annotation compiles fine but blows
 * up at runtime the moment a status carrying networks is serialized (e.g. nav-stack persistence).
 */
internal class PaymentNetworkStatusSerializationTest {

    private val json = Json

    private fun network() = Network(
        id = Network.ID(value = "polygon-pos", derivationPath = Network.DerivationPath.None),
        name = "Polygon",
        currencySymbol = "POL",
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    private fun token() = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "polygon-pos"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0xCONTRACT"),
        ),
        network = network(),
        name = "USD Coin",
        symbol = "USDC",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xCONTRACT",
    )

    @Test
    fun `GIVEN each subtype WHEN json round-trip THEN restores an equal object`() {
        // Arrange
        val statuses: List<PaymentNetworkStatus> = listOf(
            PaymentNetworkStatus.Available(
                network = network(),
                depositAddress = "0xDEPOSIT",
                cryptoCurrencyStatuses = listOf(
                    CryptoCurrencyStatus(currency = token(), value = CryptoCurrencyStatus.Loading),
                ),
            ),
            PaymentNetworkStatus.NotIssued(network = network(), cryptoCurrencies = listOf(token())),
            PaymentNetworkStatus.Disabled(network = network(), cryptoCurrencies = listOf(token())),
        )

        // Act
        val restored = json.decodeFromString<List<PaymentNetworkStatus>>(json.encodeToString(statuses))

        // Assert
        assertThat(restored).isEqualTo(statuses)
    }
}