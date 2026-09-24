package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionTotalBalanceTest {

    @Test
    fun `GIVEN the positions value was never read WHEN of THEN there is no total`() {
        // Act
        val actual = PredictionTotalBalance.of(status = active(StatusSource.ACTUAL), positions = PredictionPositionsValue.Unknown)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun of(model: OfModel) {
        // Act
        val actual = PredictionTotalBalance.of(
            status = active(model.statusSource),
            positions = PredictionPositionsValue.Known(amount = BigDecimal("2.25"), source = model.positionsSource),
        )

        // Assert
        assertThat(actual).isEqualTo(PredictionTotalBalance(amount = BigDecimal("12.75"), source = model.expected))
    }

    internal data class OfModel(
        val statusSource: StatusSource,
        val positionsSource: StatusSource,
        val expected: StatusSource,
    )

    private fun provideTestModels() = listOf(
        OfModel(statusSource = StatusSource.ACTUAL, positionsSource = StatusSource.ACTUAL, expected = StatusSource.ACTUAL),
        OfModel(statusSource = StatusSource.CACHE, positionsSource = StatusSource.ACTUAL, expected = StatusSource.CACHE),
        OfModel(
            statusSource = StatusSource.ACTUAL,
            positionsSource = StatusSource.ONLY_CACHE,
            expected = StatusSource.ONLY_CACHE,
        ),
        OfModel(
            statusSource = StatusSource.CACHE,
            positionsSource = StatusSource.ONLY_CACHE,
            expected = StatusSource.ONLY_CACHE,
        ),
    )

    private fun active(source: StatusSource) = PredictionAccountStatusValue.Active(
        source = source,
        balance = BigDecimal("10.5"),
        positionsValue = null,
        fiatRate = null,
        depositCurrency = COLLATERAL,
        depositAddress = null,
    )

    private companion object {
        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)
    }
}