package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import org.junit.jupiter.api.Test

internal class BankAccountDetailsFormValuesTest {

    @Test
    fun `GIVEN individual details WHEN toFormValues THEN every personal and bank field filled`() {
        // Arrange
        val details = bankAccount(
            beneficiary = Beneficiary.Individual(firstName = "Ada", lastName = "Lovelace"),
        ).details

        // Act
        val actual = details.toFormValues()

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                BankWithdrawFormField.AccountNumber to "0000000000",
                BankWithdrawFormField.RoutingNumber to "021000021",
                BankWithdrawFormField.FirstName to "Ada",
                BankWithdrawFormField.LastName to "Lovelace",
                BankWithdrawFormField.Country to "US",
                BankWithdrawFormField.Region to "NY",
                BankWithdrawFormField.City to "New York",
                BankWithdrawFormField.AddressLine1 to "1 Main St",
                BankWithdrawFormField.PostalCode to "10001",
                BankWithdrawFormField.BankName to "Test Bank",
                BankWithdrawFormField.BankRegion to "NY",
                BankWithdrawFormField.BankCity to "New York",
                BankWithdrawFormField.BankAddressLine1 to "1 Main St",
                BankWithdrawFormField.BankPostalCode to "10001",
            ),
        )
    }

    @Test
    fun `GIVEN business details WHEN toFormValues THEN business name instead of first and last name`() {
        // Arrange
        val details = bankAccount(beneficiary = Beneficiary.Business(name = "Arasaka")).details

        // Act
        val actual = details.toFormValues()

        // Assert
        assertThat(actual).containsEntry(BankWithdrawFormField.BusinessName, "Arasaka")
        assertThat(actual).doesNotContainKey(BankWithdrawFormField.FirstName)
        assertThat(actual).doesNotContainKey(BankWithdrawFormField.LastName)
    }

    @Test
    fun `GIVEN complete personal form values WHEN converted to details and back THEN values round-trip`() {
        // Arrange
        val values = mapOf(
            BankWithdrawFormField.AccountNumber to "1234567890",
            BankWithdrawFormField.RoutingNumber to "021000021",
            BankWithdrawFormField.FirstName to "Saburo",
            BankWithdrawFormField.LastName to "Arasaka",
            BankWithdrawFormField.Country to "US",
            BankWithdrawFormField.Region to "CA",
            BankWithdrawFormField.City to "Night City",
            BankWithdrawFormField.AddressLine1 to "Megatower H10, 322F",
            BankWithdrawFormField.PostalCode to "90001",
            BankWithdrawFormField.BankName to "Arasaka Bank",
            BankWithdrawFormField.BankRegion to "CA",
            BankWithdrawFormField.BankCity to "Night City",
            BankWithdrawFormField.BankAddressLine1 to "Ebunike st., 32A",
            BankWithdrawFormField.BankPostalCode to "90002",
        )
        val draft = NewRecipientDraft(
            isThirdParty = false,
            accountType = BankAccountType.CHECKING,
            fields = values,
        )
        val details = draft.toBankAccountDetails(rail = BankRail.ACH, isBusiness = false)

        // Act
        val actual = details.toFormValues()

        // Assert
        assertThat(actual).containsExactlyEntriesIn(values)
    }

    @Test
    fun `GIVEN details without a bank address WHEN toFormValues THEN bank rows absent`() {
        // Arrange
        val details = bankAccount().details.copy(bankName = null, bankAddress = null)

        // Act
        val actual = details.toFormValues()

        // Assert
        assertThat(actual.keys).containsNoneOf(
            BankWithdrawFormField.BankName,
            BankWithdrawFormField.BankRegion,
            BankWithdrawFormField.BankCity,
            BankWithdrawFormField.BankAddressLine1,
            BankWithdrawFormField.BankPostalCode,
        )
        assertThat(actual).containsEntry(BankWithdrawFormField.AccountNumber, "0000000000")
    }
}