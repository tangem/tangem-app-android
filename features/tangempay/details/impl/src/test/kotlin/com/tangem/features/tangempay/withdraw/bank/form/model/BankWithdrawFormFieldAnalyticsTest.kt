package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawFormFieldAnalyticsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN form field WHEN converted to analytics name THEN expected name is returned`(model: NameModel) {
        // Act
        val actual = model.field.toAnalyticsName()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN test models WHEN listed THEN every form field is covered`() {
        // Act
        val covered = provideTestModels().map(NameModel::field)

        // Assert
        assertThat(covered).containsExactlyElementsIn(BankWithdrawFormField.entries)
    }

    internal data class NameModel(val field: BankWithdrawFormField, val expected: String)

    private fun provideTestModels() = listOf(
        NameModel(field = BankWithdrawFormField.AccountNumber, expected = "Account Number"),
        NameModel(field = BankWithdrawFormField.RoutingNumber, expected = "Routing Number"),
        NameModel(field = BankWithdrawFormField.FirstName, expected = "First Name"),
        NameModel(field = BankWithdrawFormField.LastName, expected = "Last Name"),
        NameModel(field = BankWithdrawFormField.BusinessName, expected = "Business Name"),
        NameModel(field = BankWithdrawFormField.Country, expected = "Country"),
        NameModel(field = BankWithdrawFormField.Region, expected = "Region"),
        NameModel(field = BankWithdrawFormField.City, expected = "City"),
        NameModel(field = BankWithdrawFormField.AddressLine1, expected = "Address Line 1"),
        NameModel(field = BankWithdrawFormField.PostalCode, expected = "Postal Code"),
        NameModel(field = BankWithdrawFormField.BankName, expected = "Bank Name"),
        NameModel(field = BankWithdrawFormField.BankRegion, expected = "Bank Region"),
        NameModel(field = BankWithdrawFormField.BankCity, expected = "Bank City"),
        NameModel(field = BankWithdrawFormField.BankAddressLine1, expected = "Bank Address Line 1"),
        NameModel(field = BankWithdrawFormField.BankPostalCode, expected = "Bank Postal Code"),
    )
}