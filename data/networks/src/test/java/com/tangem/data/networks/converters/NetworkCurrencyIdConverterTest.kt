package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkCurrencyIdConverterTest {

    // Legacy SDK format stored in cache (see NetworkStatusDataModelConverter:
    // `value.network.toBlockchain().id`). Runtime CryptoCurrency.ID expects the canonical
    // network rawId ("ethereum"), so the converter normalizes via Blockchain.fromId(...).toNetworkId().
    private val blockchainId = "ETH"
    private val canonicalNetworkRawId = "ethereum"
    private val derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0")
    private val derivationPathHashCode = "-1843072795"
    private val converter = NetworkCurrencyIdConverter(blockchainId = blockchainId, derivationPath = derivationPath)

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
                    CryptoCurrency.ID.fromValue(value = "coin⟨ethereum→$derivationPathHashCode⟩ethereum"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createCoinId(""),
                expected = Result.failure(
                    IllegalStateException("Coin id is null for $blockchainId with $derivationPath"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createCoinId(" "),
                expected = Result.failure(
                    IllegalStateException("Coin id is null for $blockchainId with $derivationPath"),
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
                        value = "token⟨ethereum→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
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
                        value = "token⟨ethereum→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
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
                        value = "token⟨ethereum→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
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
                        value = "token⟨ethereum→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    ),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = "",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(value = "coin⟨ethereum→$derivationPathHashCode⟩usdt"),
                ),
            ),
            ConvertModel(
                value = CurrencyId.createTokenId(
                    rawTokenId = "usdt",
                    contractAddress = " ",
                ),
                expected = Result.success(
                    CryptoCurrency.ID.fromValue(value = "coin⟨ethereum→$derivationPathHashCode⟩usdt"),
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
                value = CryptoCurrency.ID.fromValue("coin⟨ethereum→$derivationPathHashCode⟩ethereum"),
                expected = Result.success(
                    CurrencyId.createCoinId("ethereum"),
                ),
            ),
            ConvertBackModel(
                value = CryptoCurrency.ID.fromValue(
                    value = "token⟨ethereum→$derivationPathHashCode⟩usdt⚓0xdAC17F958D2ee523a2206206994597C13D831ec7",
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
                    value = "token⟨ethereum→$derivationPathHashCode⟩0xdAC17F958D2ee523a2206206994597C13D831ec7",
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

    /**
     * Regression coverage for [REDACTED_TASK_KEY]. Cache stores `blockchainId` in the legacy SDK format
     * (`Blockchain.id`, e.g. "ETH"), but runtime [CryptoCurrency.ID] is built using the canonical
     * network rawId (`Blockchain.toNetworkId()`, e.g. "ethereum"). The converter must bridge the
     * two formats so that IDs reconstructed from cache equal those built at runtime — otherwise
     * `NetworkStatus.Verified.amounts[currency.id]` returns null and the wallet shimmer never clears.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LegacyBlockchainIdNormalization {

        @Test
        fun `convert with legacy ETH blockchainId produces id with canonical ethereum rawId`() {
            val cached = CurrencyId.createCoinId("ethereum")

            val result = converter.convert(cached)

            Truth.assertThat(result)
                .isEqualTo(CryptoCurrency.ID.fromValue("coin⟨$canonicalNetworkRawId→$derivationPathHashCode⟩ethereum"))
        }

        @Test
        fun `convert with legacy BTC blockchainId produces id with canonical bitcoin rawId`() {
            val btcDerivationPath = Network.DerivationPath.Card(value = "m/44'/0'/0'/0/0")
            val btcDerivationHash = btcDerivationPath.value.hashCode()
            val btcConverter = NetworkCurrencyIdConverter(
                blockchainId = "BTC",
                derivationPath = btcDerivationPath,
            )
            val cached = CurrencyId.createCoinId("bitcoin")

            val result = btcConverter.convert(cached)

            Truth.assertThat(result)
                .isEqualTo(CryptoCurrency.ID.fromValue("coin⟨bitcoin→$btcDerivationHash⟩bitcoin"))
        }

        @Test
        fun `convert and convertBack roundtrip preserves CurrencyId`() {
            val cached = CurrencyId.createCoinId("ethereum")

            val runtimeId = converter.convert(cached)
            val roundTrip = converter.convertBack(runtimeId)

            Truth.assertThat(roundTrip).isEqualTo(cached)
        }
    }

    data class ConvertModel(val value: CurrencyId, val expected: Result<CryptoCurrency.ID>)

    data class ConvertBackModel(val value: CryptoCurrency.ID, val expected: Result<CurrencyId>)
}