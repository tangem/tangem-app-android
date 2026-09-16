package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class BankWithdrawFormFieldsTest {

    @Test
    fun `GIVEN personal recipient WHEN recipient details fields THEN nine fields with first and last name`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = false)

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
    fun `GIVEN business recipient WHEN recipient details fields THEN business name instead of first and last name`() {
        // Act
        val actual = BankWithdrawFormFields.recipientDetails(isBusiness = true)

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
    fun `GIVEN bank details WHEN fields THEN six bank fields in figma order with country`() {
        // Assert
        assertThat(BankWithdrawFormFields.bankDetails).containsExactly(
            BankWithdrawFormField.BankName,
            BankWithdrawFormField.BankCountry,
            BankWithdrawFormField.BankRegion,
            BankWithdrawFormField.BankCity,
            BankWithdrawFormField.BankAddressLine1,
            BankWithdrawFormField.BankPostalCode,
        ).inOrder()
    }
}