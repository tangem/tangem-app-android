package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.AccruingBalanceContribution
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccruingTickInputsTest {

    private val usd = AppCurrency.Default
    private val eur = AppCurrency(code = "EUR", name = "Euro", symbol = "€")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN two reads WHEN inputs compared THEN only new numbers or currency restart the ticker`(model: InputsModel) {
        // Arrange
        val before = AccruingTickInputs.of(status(position(rate = "0.0255", source = StatusSource.ACTUAL)), usd)

        // Act
        val after = AccruingTickInputs.of(status(model.position), model.appCurrency)

        // Assert
        assertThat(after == before).isEqualTo(model.isSame)
    }

    private fun provideTestModels() = listOf(
        InputsModel(position = position(rate = "0.0255", source = StatusSource.CACHE), appCurrency = usd, isSame = true),
        InputsModel(
            position = position(rate = "0.0255", source = StatusSource.ONLY_CACHE),
            appCurrency = usd,
            isSame = true,
        ),
        InputsModel(position = position(rate = "0.0340", source = StatusSource.ACTUAL), appCurrency = usd, isSame = false),
        InputsModel(position = position(rate = "0.0255", source = StatusSource.ACTUAL), appCurrency = eur, isSame = false),
    )

    private fun status(position: AccruingBalanceContribution): CryptoCurrencyStatus {
        val status = MockYieldSupply.loadedStatus(currency = MockYieldSupply.coin())
        return status.copy(value = (status.value as CryptoCurrencyStatus.Loaded).copy(contributions = listOf(position)))
    }

    private fun position(rate: String, source: StatusSource): AccruingBalanceContribution = TestPosition(
        amount = BigDecimal("2"),
        source = source,
        accrualRate = BigDecimal(rate),
    )

    private data class TestPosition(
        val amount: BigDecimal,
        override val source: StatusSource,
        override val accrualRate: BigDecimal,
    ) : AccruingBalanceContribution {

        override fun totalDeltaCryptoAmount(): BigDecimal = amount
    }

    data class InputsModel(
        val position: AccruingBalanceContribution,
        val appCurrency: AppCurrency,
        val isSame: Boolean,
    )
}