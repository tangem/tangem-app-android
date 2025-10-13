package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus.Amount.Loaded
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NetworkAmountsConverterTest {

    private val rawNetworkId = "ETH"
    private val derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0")
    private val derivationPathHashCode = "-1843072795"
    private val converter = NetworkAmountsConverter(rawNetworkId = rawNetworkId, derivationPath = derivationPath)

    @Test
    fun convert() {
        // Arrange
        val value = listOf(
            NetworkStatusDM.CurrencyAmount(CurrencyId.createCoinId(coinId = "ethereum"), BigDecimal.ONE),
            NetworkStatusDM.CurrencyAmount(
                id = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                amount = BigDecimal.ZERO,
            ),
            NetworkStatusDM.CurrencyAmount(
                id = CurrencyId.createTokenId(
                    rawTokenId = null,
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                amount = BigDecimal.TEN,
            ),
        )

        // Act
        val actual = converter.convert(value)

        // Assert
        val expected = mapOf(
            ID.fromValue("coin⟨ETH→$derivationPathHashCode⟩ethereum") to Loaded(value = BigDecimal.ONE),
            ID.fromValue(
                value = "token⟨ETH→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
            ) to Loaded(value = BigDecimal.ZERO),
            ID.fromValue(
                value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
            ) to Loaded(value = BigDecimal.TEN),
        )

        Truth.assertThat(actual).containsExactlyEntriesIn(expected)
    }

    @Test
    fun convertBack() {
        // Arrange
        val value = mapOf(
            ID.fromValue("coin⟨ETH→$derivationPathHashCode⟩ethereum") to Loaded(value = BigDecimal.ONE),
            ID.fromValue(
                value = "token⟨ETH→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
            ) to Loaded(value = BigDecimal.ZERO),
            ID.fromValue(
                value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
            ) to Loaded(value = BigDecimal.TEN),
        )

        // Act
        val actual = converter.convertBack(value)

        // Assert
        val expected = listOf(
            NetworkStatusDM.CurrencyAmount(CurrencyId.createCoinId(coinId = "ethereum"), BigDecimal.ONE),
            NetworkStatusDM.CurrencyAmount(
                id = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                amount = BigDecimal.ZERO,
            ),
            NetworkStatusDM.CurrencyAmount(
                id = CurrencyId.createTokenId(
                    rawTokenId = null,
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                amount = BigDecimal.TEN,
            ),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)
    }
}