package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.domain.tokens.model.CryptoCurrency.ID
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Body
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Prefix
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
internal class NetworkAmountsConverterTest {

    @Test
    fun convert() {
        // Arrange
        val value = mapOf(
            "coin⟨BCH⟩bitcoin-cash" to BigDecimal.ZERO,
            "coin⟨ETH→12367123⟩ethereum" to BigDecimal.ONE,
        )

        // Act
        val actual = NetworkAmountsConverter.convert(value)

        // Assert
        val expected = mapOf(
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "BCH"),
                suffix = ID.Suffix.RawID(rawId = "bitcoin-cash"),
            ) to CryptoCurrencyAmountStatus.Loaded(value = BigDecimal.ZERO),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to CryptoCurrencyAmountStatus.Loaded(value = BigDecimal.ONE),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun convertBack() {
        // Arrange
        val value = mapOf(
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "BCH"),
                suffix = ID.Suffix.RawID(rawId = "bitcoin-cash"),
            ) to CryptoCurrencyAmountStatus.Loaded(value = BigDecimal.ZERO),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to CryptoCurrencyAmountStatus.Loaded(value = BigDecimal.ONE),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "BTC"),
                suffix = ID.Suffix.RawID(rawId = "bitcoin"),
            ) to CryptoCurrencyAmountStatus.NotFound,
        )

        // Act
        val actual = NetworkAmountsConverter.convertBack(value)

        // Assert
        val expected = mapOf(
            "coin⟨BCH⟩bitcoin-cash" to BigDecimal.ZERO,
            "coin⟨ETH→12367123⟩ethereum" to BigDecimal.ONE,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }
}
