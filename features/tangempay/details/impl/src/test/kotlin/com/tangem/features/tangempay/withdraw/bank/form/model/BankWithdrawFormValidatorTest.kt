package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawFormValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN field values WHEN validate THEN expected error`(model: ValidationModel) {
        // Act
        val actual = BankWithdrawFormValidator.validate(field = model.field, values = model.values)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN several fields WHEN validate all THEN only failing fields reported`() {
        // Arrange
        val fields = listOf(
            BankWithdrawFormField.AccountNumber,
            BankWithdrawFormField.RoutingNumber,
            BankWithdrawFormField.FirstName,
        )
        val values = mapOf(
            BankWithdrawFormField.AccountNumber to "4830019271",
            BankWithdrawFormField.RoutingNumber to "1",
        )

        // Act
        val actual = BankWithdrawFormValidator.validateAll(fields = fields, values = values)

        // Assert
        assertThat(actual).containsExactly(
            BankWithdrawFormField.RoutingNumber, BankWithdrawFieldError.InvalidRoutingNumber,
            BankWithdrawFormField.FirstName, BankWithdrawFieldError.Required,
        )
    }

    @Test
    fun `GIVEN recipient country WHEN dependent fields THEN region and postal code of the same section`() {
        // Assert
        assertThat(BankWithdrawFormValidator.dependentFieldsOf(BankWithdrawFormField.Country))
            .containsExactly(BankWithdrawFormField.Region, BankWithdrawFormField.PostalCode)
            .inOrder()
        assertThat(BankWithdrawFormValidator.dependentFieldsOf(BankWithdrawFormField.City)).isEmpty()
    }

    data class ValidationModel(
        val field: BankWithdrawFormField,
        val values: Map<BankWithdrawFormField, String>,
        val expected: BankWithdrawFieldError?,
    )

    private fun provideTestModels() = listOf(
        single(BankWithdrawFormField.AccountNumber, "4830", expected = null),
        single(BankWithdrawFormField.AccountNumber, "12345678901234567", expected = null),
        single(BankWithdrawFormField.AccountNumber, "483", expected = BankWithdrawFieldError.InvalidFormat),
        single(
            BankWithdrawFormField.AccountNumber,
            "123456789012345678",
            expected = BankWithdrawFieldError.InvalidFormat,
        ),
        single(BankWithdrawFormField.AccountNumber, "4830ab", expected = BankWithdrawFieldError.InvalidFormat),
        single(BankWithdrawFormField.AccountNumber, "", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.AccountNumber, "   ", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.RoutingNumber, "021000021", expected = null),
        single(
            BankWithdrawFormField.RoutingNumber,
            "02100002",
            expected = BankWithdrawFieldError.InvalidRoutingNumber,
        ),
        single(
            BankWithdrawFormField.RoutingNumber,
            "02100002a",
            expected = BankWithdrawFieldError.InvalidRoutingNumber,
        ),
        single(BankWithdrawFormField.RoutingNumber, "", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.FirstName, " John ", expected = null),
        single(BankWithdrawFormField.FirstName, "Zoë", expected = null),
        single(BankWithdrawFormField.FirstName, "Иван", expected = BankWithdrawFieldError.NonLatin),
        single(BankWithdrawFormField.LastName, "a".repeat(255), expected = null),
        single(BankWithdrawFormField.LastName, "O’Brien", expected = null),
        single(BankWithdrawFormField.BusinessName, "a".repeat(256), expected = BankWithdrawFieldError.InvalidFormat),
        single(BankWithdrawFormField.BankName, "", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.Country, "us", expected = null),
        single(BankWithdrawFormField.Country, "USA", expected = BankWithdrawFieldError.InvalidFormat),
        withCountry(BankWithdrawFormField.Region, "CA", country = "US", expected = null),
        withCountry(BankWithdrawFormField.Region, "ca", country = "us", expected = null),
        withCountry(
            BankWithdrawFormField.Region,
            "California",
            country = "US",
            expected = BankWithdrawFieldError.InvalidFormat,
        ),
        withCountry(BankWithdrawFormField.Region, "Ontario", country = "CA", expected = null),
        single(BankWithdrawFormField.Region, "California", expected = null),
        single(BankWithdrawFormField.Region, "", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.Region, "x".repeat(101), expected = BankWithdrawFieldError.InvalidFormat),
        single(BankWithdrawFormField.Region, "Бавария", expected = BankWithdrawFieldError.NonLatin),
        single(BankWithdrawFormField.BankRegion, "NY", expected = null),
        single(BankWithdrawFormField.BankRegion, "New York", expected = BankWithdrawFieldError.InvalidFormat),
        withCountry(
            BankWithdrawFormField.BankRegion,
            "New York",
            country = "GB",
            expected = BankWithdrawFieldError.InvalidFormat,
        ),
        withCountry(BankWithdrawFormField.PostalCode, "12345", country = "US", expected = null),
        withCountry(BankWithdrawFormField.PostalCode, "12345-6789", country = "US", expected = null),
        withCountry(BankWithdrawFormField.PostalCode, "123456789", country = "US", expected = null),
        withCountry(
            BankWithdrawFormField.PostalCode,
            "1234",
            country = "US",
            expected = BankWithdrawFieldError.InvalidFormat,
        ),
        withCountry(BankWithdrawFormField.PostalCode, "SW1A 1AA", country = "GB", expected = null),
        single(BankWithdrawFormField.BankPostalCode, "10001", expected = null),
        single(BankWithdrawFormField.BankPostalCode, "1234", expected = BankWithdrawFieldError.InvalidFormat),
        withCountry(
            BankWithdrawFormField.BankPostalCode,
            "SW1A 1AA",
            country = "GB",
            expected = BankWithdrawFieldError.InvalidFormat,
        ),
        single(BankWithdrawFormField.City, "x".repeat(100), expected = null),
        single(BankWithdrawFormField.City, "Nürnberg", expected = null),
        single(BankWithdrawFormField.City, "я".repeat(101), expected = BankWithdrawFieldError.InvalidFormat),
        single(BankWithdrawFormField.AddressLine1, "x".repeat(101), expected = BankWithdrawFieldError.InvalidFormat),
        single(BankWithdrawFormField.BankCity, "", expected = BankWithdrawFieldError.Required),
        single(BankWithdrawFormField.BankCity, "东京", expected = BankWithdrawFieldError.NonLatin),
        single(BankWithdrawFormField.BankAddressLine1, "1 Main St", expected = null),
    )

    private fun single(field: BankWithdrawFormField, value: String, expected: BankWithdrawFieldError?) =
        ValidationModel(field = field, values = mapOf(field to value), expected = expected)

    private fun withCountry(
        field: BankWithdrawFormField,
        value: String,
        country: String,
        expected: BankWithdrawFieldError?,
    ) = ValidationModel(
        field = field,
        values = mapOf(field to value, BankWithdrawFormField.Country to country),
        expected = expected,
    )
}