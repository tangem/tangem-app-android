package com.tangem.domain.models.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Guards the `@Transient` contract on [CryptoCurrencyStatus.Value.contributions]: [BalanceContribution]
 * is an open interface with no polymorphic registration, so without the annotation encoding a status with
 * a non-empty list compiles fine but throws [kotlinx.serialization.SerializationException] at runtime
 * (e.g. nav-stack persistence).
 */
internal class CryptoCurrencyStatusSerializationTest {

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
    fun `GIVEN loaded status with contributions WHEN json round-trip THEN contributions are excluded`() {
        // Arrange
        val contribution = StakingBalance.Empty(
            stakingId = StakingID(integrationId = "polygon-pol-native-staking", address = "0xADDRESS"),
            source = StatusSource.CACHE,
        )
        val loaded = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.TEN,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "0xADDRESS", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(contributionSources = listOf(StatusSource.CACHE)),
            contributions = listOf(contribution),
        )
        val status = CryptoCurrencyStatus(currency = token(), value = loaded)

        // Act
        val restored = json.decodeFromString<CryptoCurrencyStatus>(json.encodeToString(status))

        // Assert
        assertThat(restored).isEqualTo(status.copy(value = loaded.copy(contributions = emptyList())))
    }
}