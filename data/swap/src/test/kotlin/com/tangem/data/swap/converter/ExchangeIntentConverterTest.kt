package com.tangem.data.swap.converter

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.grow.datasource.express.models.response.ExchangeIntentResponse
import com.tangem.grow.datasource.express.models.response.ExchangeIntentTransaction
import com.tangem.grow.datasource.express.models.response.RateType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.grow.datasource.express.models.response.ExchangeIntent as ExchangeIntentDto

internal class ExchangeIntentConverterTest {

    private val converter = ExchangeIntentConverter(rateTypeConverter = ExpressRateTypeConverter())

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN intent response WHEN convert THEN it is read as the expected exchange`(model: ConvertModel) {
            // Act
            val actual = converter.convert(ExchangeIntentResponse(model.intent, model.transactions))

            // Assert
            assertThat(actual).isEqualTo(Either.Right(model.expected))
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            ConvertModel(
                intent = createIntent(),
                transactions = emptyList(),
                expected = createExchange(),
            ),
            ConvertModel(
                intent = createIntent(
                    fromAmount = null,
                    toAmount = "2500000",
                    rateType = RateType.FIXED,
                    toExtraId = "memo",
                ),
                transactions = emptyList(),
                expected = createExchange(
                    amountType = SwapAmountType.To,
                    amount = "2500000",
                    rateType = ExpressRateType.Fixed,
                    toExtraId = "memo",
                ),
            ),
            ConvertModel(
                intent = createIntent(rateType = RateType.FIXED),
                transactions = emptyList(),
                expected = createExchange(rateType = ExpressRateType.Fixed),
            ),
            ConvertModel(
                intent = createIntent(),
                transactions = listOf(ExchangeIntentTransaction(txId = "tx-1")),
                expected = createExchange(isHandled = true),
            ),
            ConvertModel(
                intent = createIntent(clientId = null, sessionId = null, fromAddress = null, payoutAddress = null),
                transactions = emptyList(),
                expected = createExchange(
                    clientId = null,
                    sessionId = null,
                    from = createAsset(network = "ethereum", decimals = 18, address = null),
                    to = createAsset(network = "tron", decimals = 6, address = null),
                ),
            ),
        )

        @ParameterizedTest
        @MethodSource("provideAmbiguousModels")
        fun `GIVEN amounts the service must not send WHEN convert THEN they are reported as ambiguous`(
            model: AmbiguousModel,
        ) {
            // Act
            val actual = converter.convert(ExchangeIntentResponse(model.intent, transactions = emptyList()))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(ExchangeIntentError.AmbiguousAmountSide(INTENT_ID)))
        }

        @Suppress("unused")
        private fun provideAmbiguousModels() = listOf(
            AmbiguousModel(intent = createIntent(fromAmount = "1", toAmount = "2")),
            AmbiguousModel(intent = createIntent(fromAmount = null, toAmount = null)),
        )
    }

    data class ConvertModel(
        val intent: ExchangeIntentDto,
        val transactions: List<ExchangeIntentTransaction>,
        val expected: ExchangeIntent,
    )

    data class AmbiguousModel(val intent: ExchangeIntentDto)

    private fun createIntent(
        fromAmount: String? = "1000000000000000000",
        toAmount: String? = null,
        rateType: RateType = RateType.FLOAT,
        toExtraId: String? = null,
        clientId: String? = "claude-code",
        sessionId: String? = "session-1",
        fromAddress: String? = "0xFrom",
        payoutAddress: String? = "TPayout",
    ) = ExchangeIntentDto(
        intentId = INTENT_ID,
        clientId = clientId,
        sessionId = sessionId,
        walletId = "wallet-1",
        provider = "changelly",
        rateType = rateType,
        fromNetwork = "ethereum",
        fromContractAddress = "0",
        fromDecimals = 18,
        fromAmount = fromAmount,
        fromAddress = fromAddress,
        toNetwork = "tron",
        toContractAddress = "0",
        toDecimals = 6,
        toAmount = toAmount,
        payoutAddress = payoutAddress,
        toExtraId = toExtraId,
    )

    private fun createAsset(network: String, decimals: Int, address: String?) = ExchangeIntent.Asset(
        network = network,
        contractAddress = "0",
        decimals = decimals,
        address = address,
    )

    @Suppress("LongParameterList")
    private fun createExchange(
        clientId: String? = "claude-code",
        sessionId: String? = "session-1",
        amountType: SwapAmountType = SwapAmountType.From,
        amount: String = "1000000000000000000",
        from: ExchangeIntent.Asset = createAsset(network = "ethereum", decimals = 18, address = "0xFrom"),
        to: ExchangeIntent.Asset = createAsset(network = "tron", decimals = 6, address = "TPayout"),
        rateType: ExpressRateType = ExpressRateType.Float,
        toExtraId: String? = null,
        isHandled: Boolean = false,
    ) = ExchangeIntent(
        id = INTENT_ID,
        walletId = "wallet-1",
        clientId = clientId,
        sessionId = sessionId,
        isHandled = isHandled,
        amountType = amountType,
        amount = amount,
        from = from,
        to = to,
        providerId = "changelly",
        rateType = rateType,
        toExtraId = toExtraId,
    )

    private companion object {
        const val INTENT_ID = "fb9488fc-e44c-435c-b074-144b5a288c42"
    }
}