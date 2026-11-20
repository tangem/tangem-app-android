package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawFormFieldsTest {

    @Test
    fun `GIVEN personal recipient WHEN recipient details fields THEN nine fields with first and last name`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = false, rail = BankRail.ACH)

        // Assert
        assertThat(actual).containsExactly(
            BankWithdrawFormField.AccountNumber,
            BankWithdrawFormField.RoutingNumber,
            BankWithdrawFormField.FirstName,
            BankWithdrawFormField.LastName,
            BankWithdrawFormField.Country,
            BankWithdrawFormField.Region,
            BankWithdrawFormField.City,
            BankWithdrawFormField.AddressLine1,
            BankWithdrawFormField.PostalCode,
        ).inOrder()
    }

    @Test
    fun `GIVEN personal SPEI recipient WHEN recipient details fields THEN CLABE and the name only`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = false, rail = BankRail.SPEI)

        // Assert
        assertThat(actual).containsExactly(
            BankWithdrawFormField.Clabe,
            BankWithdrawFormField.FirstName,
            BankWithdrawFormField.LastName,
        ).inOrder()
    }

    @Test
    fun `GIVEN business SPEI recipient WHEN recipient details fields THEN CLABE and the business name only`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = true, rail = BankRail.SPEI)

        // Assert
        assertThat(actual).containsExactly(BankWithdrawFormField.Clabe, BankWithdrawFormField.BusinessName).inOrder()
    }

    @Test
    fun `GIVEN business recipient WHEN recipient details fields THEN business name instead of first and last name`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = true, rail = BankRail.ACH)

        // Assert
        assertThat(actual).containsExactly(
            BankWithdrawFormField.AccountNumber,
            BankWithdrawFormField.RoutingNumber,
            BankWithdrawFormField.BusinessName,
            BankWithdrawFormField.Country,
            BankWithdrawFormField.Region,
            BankWithdrawFormField.City,
            BankWithdrawFormField.AddressLine1,
            BankWithdrawFormField.PostalCode,
        ).inOrder()
    }

    @Test
    fun `GIVEN bank details WHEN fields THEN five bank fields in figma order without country`() {
        // Assert
        assertThat(BankWithdrawFormFields.bankDetails).containsExactly(
            BankWithdrawFormField.BankName,
            BankWithdrawFormField.BankRegion,
            BankWithdrawFormField.BankCity,
            BankWithdrawFormField.BankAddressLine1,
            BankWithdrawFormField.BankPostalCode,
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN recipient country value WHEN checked as us country THEN expected verdict`(model: UsCountryModel) {
        // Act
        val actual = BankWithdrawFormFields.isUsRecipientCountry(model.values)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class UsCountryModel(val values: Map<BankWithdrawFormField, String>, val expected: Boolean)

    private fun provideTestModels() = listOf(
        UsCountryModel(values = mapOf(BankWithdrawFormField.Country to "US"), expected = true),
        UsCountryModel(values = mapOf(BankWithdrawFormField.Country to " us "), expected = true),
        UsCountryModel(values = mapOf(BankWithdrawFormField.Country to "MC"), expected = false),
        UsCountryModel(values = mapOf(BankWithdrawFormField.Country to ""), expected = false),
        UsCountryModel(values = emptyMap(), expected = false),
    )
}