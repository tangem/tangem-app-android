package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.currency.CryptoCurrency.ID.Prefix
import com.tangem.domain.models.network.NetworkStatus.Amount
import com.tangem.domain.models.network.NetworkStatus.Amount.Loaded
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
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
            ) to Loaded(value = BigDecimal.ZERO),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to Loaded(value = BigDecimal.ONE),
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
            ) to Loaded(value = BigDecimal.ZERO),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to Loaded(value = BigDecimal.ONE),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "BTC"),
                suffix = ID.Suffix.RawID(rawId = "bitcoin"),
            ) to Amount.NotFound,
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