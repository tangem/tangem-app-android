package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CurrencyIdConverterTest {

    private val rawNetworkId = "ETH"
    private val derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0")
    private val derivationPathHashCode = "-1843072795"
    private val converter = CurrencyIdConverter(rawNetworkId = rawNetworkId, derivationPath = derivationPath)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = runCatching { converter.convert(value = model.value) }

            // Assert
            actual
                .onSuccess {
                    Truth.assertThat(it).isEqualTo(model.expected.getOrNull())
                }
                .onFailure {
                    val expected = model.expected.exceptionOrNull()!!

                    Truth.assertThat(it).isInstanceOf(expected::class.java)
                    Truth.assertThat(it).hasMessageThat().isEqualTo(expected.message)
                }
        }

        private fun provideTestModels(): Collection<ConvertModel> = listOf(
            // create coin id
            ConvertModel(
                value = CurrencyId.createCoinId("ethereum"),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(value = "coin⟨ETH→$derivationPathHashCode⟩ethereum"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createCoinId(""),
                expected = Result.failure(
                    IllegalStateException("Coin id is null for $rawNetworkId with $derivationPath"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createCoinId(" "),
                expected = Result.failure(
                    IllegalStateException("Coin id is null for $rawNetworkId with $derivationPath"),
                ),
            ),
            // create token id
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(
                        value = "token⟨ETH→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = null,
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(
                        value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(
                        value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = " ",
                    contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(
                        value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = "",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(value = "coin⟨ETH→$derivationPathHashCode⟩usdt"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = " ",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(value = "coin⟨ETH→$derivationPathHashCode⟩usdt"),
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = runCatching { converter.convertBack(value = model.value) }

            // Assert
            actual
                .onSuccess {
                    Truth.assertThat(it).isEqualTo(model.expected.getOrNull())
                }
                .onFailure {
                    val expected = model.expected.exceptionOrNull()!!

                    Truth.assertThat(it).isInstanceOf(expected::class.java)
                    Truth.assertThat(it).hasMessageThat().isEqualTo(expected.message)
                }
        }

        private fun provideTestModels(): Collection<ConvertBackModel> = listOf(
            ConvertBackModel(
                value = CryptoCurrency.ID.fromValue("coin⟨ETH→$derivationPathHashCode⟩ethereum"),
                expected = Result.success(
                    CurrencyId.createCoinId("ethereum"),
                ),
            ),
            ConvertBackModel(
                value = CryptoCurrency.ID.fromValue(
                    value = "token⟨ETH→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CurrencyId.createTokenId(
                        rawTokenId = "usdt",
                        contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertBackModel(
                value = CryptoCurrency.ID.fromValue(
                    value = "token⟨ETH→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
                ),
                expected = Result.success(
                    CurrencyId.createTokenId(
                        rawTokenId = null,
                        contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
        )
    }

    data class ConvertModel(val value: CurrencyId, val expected: Result<CryptoCurrency.ID>)

    data class ConvertBackModel(val value: CryptoCurrency.ID, val expected: Result<CurrencyId>)
}