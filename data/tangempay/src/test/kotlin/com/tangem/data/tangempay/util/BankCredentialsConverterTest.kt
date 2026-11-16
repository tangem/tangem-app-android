package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.spend.datasource.tangempay.models.response.AmountLimitsResponse
import com.tangem.spend.datasource.tangempay.models.response.BankCredentialsResponse
import com.tangem.spend.datasource.tangempay.models.response.FeesResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankCredentialsConverterTest {

    private val converter = BankCredentialsConverter(isDepositFeeRead = true)

    @Test
    fun `GIVEN full response WHEN convert THEN all fields mapped`() {
        // Arrange
        val response = result()

        // Act
        val actual = converter.convert(response)

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
                calculationType = FeeCalculationType.FIXED,
                amount = BigDecimal("1.00"),
            ),
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
            exchangeRate = BankCredentials.ExchangeRate(rate = BigDecimal("17.25"), base = "USD", quote = "MXN"),
            amountLimits = AmountLimits(min = BigDecimal("2"), max = null, currency = "USD"),
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
            depositFee = null,
            depositLimit = null,
            amountLimits = null,
            exchangeRate = null,
        )

        // Act
        val actual = converter.convert(response)

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
        val response = result(
            transferMessage = null,
            fee = null,
            depositLimit = null,
            exchangeRate = null,
            amountLimits = null,
        )

        // Act
        val actual = converter.convert(response)

        // Assert
        assertThat(actual.transferMessage).isNull()
        assertThat(actual.depositFee).isNull()
        assertThat(actual.depositLimit).isNull()
        assertThat(actual.exchangeRate).isNull()
        assertThat(actual.amountLimits).isNull()
        assertThat(actual.accountNumber).isEqualTo("707613210122")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN deposit fee or legacy fee WHEN convert THEN the deposit fee is read only with MXN`(model: FeeModel) {
        // Act
        val actual = BankCredentialsConverter(isDepositFeeRead = model.isMxnEnabled)
            .convert(result(fee = model.fee, depositFee = model.depositFee))

        // Assert
        assertThat(actual.depositFee).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideExchangeRateModels")
    fun `GIVEN exchange rate WHEN convert THEN mapped only when complete`(model: ExchangeRateModel) {
        // Act
        val actual = converter.convert(result(exchangeRate = model.exchangeRate))

        // Assert
        assertThat(actual.exchangeRate).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN blank rail WHEN convert THEN it is dropped`() {
        // Arrange
        val response = result(rail = " ")

        // Act
        val actual = converter.convert(response)

        // Assert
        assertThat(actual.rail).isNull()
    }

    @Test
    fun `GIVEN blank transfer message WHEN convert THEN it is dropped`() {
        // Arrange
        val response = result(transferMessage = "   ")

        // Act
        val actual = converter.convert(response)

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
        val actual = converter.convert(response)

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
        val actual = converter.convert(response)

        // Assert
        assertThat(actual.depositLimit).isNull()
    }

    private fun provideTestModels() = listOf(
        FeeModel(fee = legacyFee(type = "flat", amount = "1.00"), expected = fixedFee("1.00")),
        FeeModel(fee = legacyFee(type = "PERCENTAGE", amount = "1"), expected = percentageFee("1")),
        FeeModel(fee = legacyFee(type = "variable", amount = "1.00"), expected = percentageFee("1.00")),
        FeeModel(fee = legacyFee(type = "tiered", amount = "1.00"), expected = null),
        FeeModel(fee = legacyFee(type = null, amount = "1.00"), expected = null),
        FeeModel(fee = legacyFee(type = "flat", amount = null), expected = null),
        FeeModel(fee = legacyFee(type = "flat", amount = "n/a"), expected = null),
        FeeModel(fee = legacyFee(type = "ACH_ONRAMP", amount = "0.00"), expected = null),
        FeeModel(
            fee = legacyFee(type = "variable", amount = "1.00"),
            depositFee = sharedFee(
                type = "SPEI_ONRAMP",
                calculationType = "PERCENTAGE",
                amount = "1.00",
                fixedAmount = "10",
                currency = "MXN",
            ),
            expected = percentageFee("1.00", fixedAmount = "10", currency = "MXN"),
        ),
        FeeModel(
            fee = legacyFee(type = "flat", amount = "1.00"),
            depositFee = sharedFee(type = "ACH_ONRAMP", calculationType = "TIERED", amount = "1.00", currency = "USD"),
            expected = fixedFee("1.00"),
        ),
        FeeModel(
            fee = null,
            depositFee = sharedFee(type = "ACH_ONRAMP", calculationType = "FIXED", amount = "0", currency = "USD"),
            expected = fixedFee("0", currency = "USD"),
        ),
        FeeModel(
            fee = legacyFee(type = "flat", amount = "n/a"),
            depositFee = sharedFee(type = "ACH_ONRAMP", calculationType = "FIXED", amount = "0", currency = "USD"),
            expected = fixedFee("0", currency = "USD"),
        ),
        FeeModel(
            fee = legacyFee(type = "variable", amount = "1.00"),
            depositFee = sharedFee(
                type = "ACH_ONRAMP",
                calculationType = "PERCENTAGE",
                amount = "1.00",
                fixedAmount = "0.50",
                currency = "USD",
            ),
            expected = percentageFee("1.00"),
            isMxnEnabled = false,
        ),
        FeeModel(
            fee = null,
            depositFee = sharedFee(type = "ACH_ONRAMP", calculationType = "FIXED", amount = "0", currency = "USD"),
            expected = null,
            isMxnEnabled = false,
        ),
    )

    private fun legacyFee(type: String?, amount: String?) =
        BankCredentialsResponse.LegacyFee(type = type, amount = amount)

    private fun sharedFee(
        type: String?,
        calculationType: String?,
        amount: String?,
        currency: String?,
        fixedAmount: String? = null,
    ) = FeesResponse.Fee(
        type = type,
        group = null,
        name = null,
        paymentMethodId = null,
        calculationType = calculationType,
        amount = amount?.let(::BigDecimal),
        fixedAmount = fixedAmount?.let(::BigDecimal),
        currency = currency,
        description = null,
    )

    private fun fixedFee(amount: String, currency: String? = null) = BankCredentials.DepositFee(
        calculationType = FeeCalculationType.FIXED,
        amount = BigDecimal(amount),
        currency = currency,
    )

    private fun percentageFee(amount: String, fixedAmount: String? = null, currency: String? = null) =
        BankCredentials.DepositFee(
            calculationType = FeeCalculationType.PERCENTAGE,
            amount = BigDecimal(amount),
            fixedAmount = fixedAmount?.let(::BigDecimal),
            currency = currency,
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
        fee: BankCredentialsResponse.LegacyFee? = legacyFee(type = "flat", amount = "1.00"),
        depositFee: FeesResponse.Fee? = null,
        depositLimit: BankCredentialsResponse.DepositLimit? = BankCredentialsResponse.DepositLimit(
            amount = "10000",
            period = "day",
        ),
        exchangeRate: BankCredentialsResponse.ExchangeRate? = BankCredentialsResponse.ExchangeRate(
            rate = "17.25",
            base = "USD",
            quote = "MXN",
        ),
        amountLimits: AmountLimitsResponse? = AmountLimitsResponse(min = BigDecimal("2"), max = null, currency = "USD"),
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
        depositFee = depositFee,
        depositLimit = depositLimit,
        amountLimits = amountLimits,
        exchangeRate = exchangeRate,
    )

    data class FeeModel(
        val fee: BankCredentialsResponse.LegacyFee?,
        val depositFee: FeesResponse.Fee? = null,
        val expected: BankCredentials.DepositFee?,
        val isMxnEnabled: Boolean = true,
    )

    data class ExchangeRateModel(
        val exchangeRate: BankCredentialsResponse.ExchangeRate?,
        val expected: BankCredentials.ExchangeRate?,
    )
}