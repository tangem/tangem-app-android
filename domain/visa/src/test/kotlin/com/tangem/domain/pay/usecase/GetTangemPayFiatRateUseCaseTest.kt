package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTangemPayFiatRateUseCaseTest {

    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()

    private val useCase = GetTangemPayFiatRateUseCase(singleQuoteStatusSupplier = singleQuoteStatusSupplier)

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleQuoteStatusSupplier)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quote status WHEN invoke THEN expected fiat rate`(model: FiatRateModel) = runTest {
        // Arrange
        coEvery {
            singleQuoteStatusSupplier.getSyncOrNull(
                SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
            )
        } returns model.quoteStatus

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FiatRateModel(
            name = "quote data",
            quoteStatus = QuoteStatus(
                rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID,
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = FIAT_RATE,
                    fiatRateUSD = FIAT_RATE,
                    priceChange = BigDecimal.ZERO,
                ),
            ),
            expected = FIAT_RATE,
        ),
        FiatRateModel(
            name = "empty quote",
            quoteStatus = QuoteStatus(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
            expected = null,
        ),
        FiatRateModel(
            name = "no quote",
            quoteStatus = null,
            expected = null,
        ),
    )

    data class FiatRateModel(
        val name: String,
        val quoteStatus: QuoteStatus?,
        val expected: BigDecimal?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val FIAT_RATE = BigDecimal("0.99")
    }
}