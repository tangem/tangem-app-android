package com.tangem.data.quotes.utils

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsCoinIds
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.quotes.utils.QuotesUnsupportedCurrenciesIdAdapter.ReplacementResult
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse.Quote
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QuotesUnsupportedCurrenciesIdAdapterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReplaceUnsupportedCurrencies {

        @ParameterizedTest
        @ProvideTestModels
        fun replaceUnsupportedCurrencies(model: ReplaceUnsupportedCurrenciesModel) {
            // Act
            val actual = QuotesUnsupportedCurrenciesIdAdapter.replaceUnsupportedCurrencies(
                currenciesIds = model.currenciesIds,
            )

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            ReplaceUnsupportedCurrenciesModel(
                currenciesIds = l2BlockchainsCoinIds.toSet(),
                expected = ReplacementResult(
                    idsForRequest = l2BlockchainsCoinIds.mapTo(hashSetOf()) {
                        Blockchain.Ethereum.toCoinId()
                    },
                    idsFiltered = l2BlockchainsCoinIds.toSet(),
                ),
            ),
            ReplaceUnsupportedCurrenciesModel(
                currenciesIds = notL2BlockchainsCoinIds,
                expected = ReplacementResult(idsForRequest = notL2BlockchainsCoinIds, idsFiltered = emptySet()),
            ),
            ReplaceUnsupportedCurrenciesModel(
                currenciesIds = blockchainCoinIds,
                expected = ReplacementResult(
                    idsForRequest = Blockchain.entries
                        .filterNot(Blockchain::isTestnet)
                        .mapTo(hashSetOf()) {
                            if (it.isL2EthereumNetwork()) {
                                Blockchain.Ethereum.toCoinId()
                            } else {
                                it.toCoinId()
                            }
                        },
                    idsFiltered = l2BlockchainsCoinIds.toSet(),
                ),
            ),
        )
    }

    data class ReplaceUnsupportedCurrenciesModel(
        val currenciesIds: Set<String>,
        val expected: ReplacementResult,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetResponseWithUnsupportedCurrencies {

        private val zeroQuote = MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ZERO)
        private val ethQuote = "ethereum" to zeroQuote

        @ParameterizedTest
        @ProvideTestModels
        fun getResponseWithUnsupportedCurrencies(model: GetResponseWithUnsupportedCurrenciesModel) {
            // Act
            val actual = QuotesUnsupportedCurrenciesIdAdapter.getResponseWithUnsupportedCurrencies(
                response = model.response,
                filteredIds = model.filteredIds,
            )

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            GetResponseWithUnsupportedCurrenciesModel(
                response = QuotesResponse(quotes = emptyMap()),
                filteredIds = emptySet(),
                expected = QuotesResponse(quotes = emptyMap()),
            ),
            GetResponseWithUnsupportedCurrenciesModel(
                response = createQuotesResponse(ethQuote),
                filteredIds = emptySet(),
                expected = createQuotesResponse(ethQuote),
            ),
            GetResponseWithUnsupportedCurrenciesModel(
                response = createQuotesResponse(
                    "arbitrum-one" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
                    ethQuote,
                ),
                filteredIds = blockchainCoinIds,
                expected = createQuotesResponse(
                    *l2BlockchainsCoinIds.map { it to zeroQuote }.toTypedArray(),
                    "arbitrum-one" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
                    ethQuote,
                ),
            ),
        )

        private fun createQuotesResponse(vararg quotes: Pair<String, Quote>): QuotesResponse {
            return QuotesResponse(quotes = quotes.toMap())
        }
    }

    data class GetResponseWithUnsupportedCurrenciesModel(
        val response: QuotesResponse,
        val filteredIds: Set<String>,
        val expected: QuotesResponse,
    )

    private companion object {

        val blockchainCoinIds = Blockchain.entries
            .filterNot(Blockchain::isTestnet)
            .mapTo(destination = hashSetOf(), transform = Blockchain::toCoinId)

        val notL2BlockchainsCoinIds = Blockchain.entries
            .filterNot { it.isTestnet() || it.isL2EthereumNetwork() }
            .mapTo(hashSetOf()) { it.toCoinId() }
    }
}