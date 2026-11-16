package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.BankCredentialsResponse
import com.tangem.domain.models.account.BankCredentials
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankCredentialsConverterTest {

    @Test
    fun `GIVEN full response WHEN convert THEN all fields mapped`() {
        // Arrange
        val response = result()

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        val expected = BankCredentials(
            type = "fiat",
            beneficiaryName = "Ivan Ivanov",
            beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
            beneficiaryBankName = "SSB BANK",
            beneficiaryBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
            accountNumber = "707613210122",
            routingNumber = "043087080",
            rail = "ACH",
            transferMessage = "REF-A1B2C3D4",
            depositFee = BankCredentials.DepositFee(
                amount = BigDecimal("1.00"),
                type = BankCredentials.DepositFee.Type.FLAT,
            ),
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
            exchangeRate = BankCredentials.ExchangeRate(rate = BigDecimal("17.25"), base = "USD", quote = "MXN"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN null fields WHEN convert THEN mapped to empty strings`() {
        // Arrange
        val response = BankCredentialsResponse.Result(
            type = null,
            beneficiaryName = null,
            beneficiaryAddress = null,
            beneficiaryBankName = null,
            beneficiaryBankAddress = null,
            accountNumber = null,
            routingNumber = null,
            rail = null,
            transferMessage = null,
            fee = null,
            depositLimit = null,
            exchangeRate = null,
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        val expected = BankCredentials(
            type = "",
            beneficiaryName = "",
            beneficiaryAddress = "",
            beneficiaryBankName = "",
            beneficiaryBankAddress = "",
            accountNumber = "",
            routingNumber = "",
            rail = null,
            transferMessage = null,
            depositFee = null,
            depositLimit = null,
            exchangeRate = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN response without the extension WHEN convert THEN new fields are null`() {
        // Arrange
        val response = result(transferMessage = null, fee = null, depositLimit = null, exchangeRate = null)

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.transferMessage).isNull()
        assertThat(actual.depositFee).isNull()
        assertThat(actual.depositLimit).isNull()
        assertThat(actual.exchangeRate).isNull()
        assertThat(actual.accountNumber).isEqualTo("707613210122")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN route fee WHEN convert THEN amount and type mapped`(model: FeeModel) {
        // Act
        val actual = BankCredentialsConverter.convert(result(fee = model.fee))

        // Assert
        assertThat(actual.depositFee).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideExchangeRateModels")
    fun `GIVEN exchange rate WHEN convert THEN mapped only when complete`(model: ExchangeRateModel) {
        // Act
        val actual = BankCredentialsConverter.convert(result(exchangeRate = model.exchangeRate))

        // Assert
        assertThat(actual.exchangeRate).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN blank rail WHEN convert THEN it is dropped`() {
        // Arrange
        val response = result(rail = " ")

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.rail).isNull()
    }

    @Test
    fun `GIVEN blank transfer message WHEN convert THEN it is dropped`() {
        // Arrange
        val response = result(transferMessage = "   ")

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.transferMessage).isNull()
    }

    @Test
    fun `GIVEN unrecognised period WHEN convert THEN period is unknown`() {
        // Arrange
        val response = result(
            depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "month"),
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.depositLimit?.period).isEqualTo(BankCredentials.DepositLimit.Period.UNKNOWN)
    }

    @Test
    fun `GIVEN limit without amount WHEN convert THEN limit is dropped`() {
        // Arrange
        val response = result(
            depositLimit = BankCredentialsResponse.DepositLimit(amount = null, period = "day"),
        )

        // Act
        val actual = BankCredentialsConverter.convert(response)

        // Assert
        assertThat(actual.depositLimit).isNull()
    }

    private fun provideTestModels() = listOf(
        FeeModel(
            fee = BankCredentialsResponse.Fee(type = "flat", amount = "1.00"),
            expected = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.FLAT),
        ),
        FeeModel(
            fee = BankCredentialsResponse.Fee(type = "PERCENTAGE", amount = "1"),
            expected = BankCredentials.DepositFee(BigDecimal("1"), BankCredentials.DepositFee.Type.PERCENTAGE),
        ),
        FeeModel(
            fee = BankCredentialsResponse.Fee(type = "variable", amount = "1.00"),
            expected = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.PERCENTAGE),
        ),
        FeeModel(
            fee = BankCredentialsResponse.Fee(type = "tiered", amount = "1.00"),
            expected = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.UNKNOWN),
        ),
        FeeModel(
            fee = BankCredentialsResponse.Fee(type = null, amount = "1.00"),
            expected = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.UNKNOWN),
        ),
        FeeModel(fee = BankCredentialsResponse.Fee(type = "flat", amount = null), expected = null),
        FeeModel(fee = BankCredentialsResponse.Fee(type = "flat", amount = "n/a"), expected = null),
    )

    private fun provideExchangeRateModels() = listOf(
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = "USD", quote = "MXN"),
            expected = BankCredentials.ExchangeRate(rate = BigDecimal("17.25"), base = "USD", quote = "MXN"),
        ),
        ExchangeRateModel(exchangeRate = null, expected = null),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = null, base = "USD", quote = "MXN"),
            expected = null,
        ),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "n/a", base = "USD", quote = "MXN"),
            expected = null,
        ),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = " ", quote = "MXN"),
            expected = null,
        ),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = null, quote = "MXN"),
            expected = null,
        ),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = "USD", quote = null),
            expected = null,
        ),
        ExchangeRateModel(
            exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = "USD", quote = " "),
            expected = null,
        ),
    )

    private fun result(
        rail: String? = "ACH",
        transferMessage: String? = "REF-A1B2C3D4",
        fee: BankCredentialsResponse.Fee? = BankCredentialsResponse.Fee(type = "flat", amount = "1.00"),
        depositLimit: BankCredentialsResponse.DepositLimit? = BankCredentialsResponse.DepositLimit(
            amount = "10000",
            period = "day",
        ),
        exchangeRate: BankCredentialsResponse.ExchangeRate? = BankCredentialsResponse.ExchangeRate(
            rate = "17.25",
            base = "USD",
            quote = "MXN",
        ),
    ) = BankCredentialsResponse.Result(
        type = "fiat",
        beneficiaryName = "Ivan Ivanov",
        beneficiaryAddress = "18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
        beneficiaryBankName = "SSB BANK",
        beneficiaryBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237, US",
        accountNumber = "707613210122",
        routingNumber = "043087080",
        rail = rail,
        transferMessage = transferMessage,
        fee = fee,
        depositLimit = depositLimit,
        exchangeRate = exchangeRate,
    )

    data class FeeModel(val fee: BankCredentialsResponse.Fee, val expected: BankCredentials.DepositFee?)

    data class ExchangeRateModel(
        val exchangeRate: BankCredentialsResponse.ExchangeRate?,
        val expected: BankCredentials.ExchangeRate?,
    )
}