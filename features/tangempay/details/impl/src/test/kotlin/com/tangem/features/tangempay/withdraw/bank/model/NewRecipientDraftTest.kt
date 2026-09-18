package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import org.junit.jupiter.api.Test

internal class NewRecipientDraftTest {

    private val personalFields = mapOf(
        BankWithdrawFormField.AccountNumber to "4830019271",
        BankWithdrawFormField.RoutingNumber to "021000021",
        BankWithdrawFormField.FirstName to "Basit",
        BankWithdrawFormField.LastName to "Didenko",
        BankWithdrawFormField.Country to "us",
        BankWithdrawFormField.Region to "ny",
        BankWithdrawFormField.City to "New York",
        BankWithdrawFormField.AddressLine1 to "1 Main St",
        BankWithdrawFormField.PostalCode to "10001",
        BankWithdrawFormField.BankName to "Test Bank",
        BankWithdrawFormField.BankRegion to "ca",
        BankWithdrawFormField.BankCity to "San Francisco",
        BankWithdrawFormField.BankAddressLine1 to "2 Bank St",
        BankWithdrawFormField.BankPostalCode to "94105",
    )

    private val businessFields = personalFields
        .minus(BankWithdrawFormField.FirstName)
        .minus(BankWithdrawFormField.LastName)
        .plus(BankWithdrawFormField.BusinessName to "Raxanax")

    @Test
    fun `GIVEN complete personal draft WHEN to bank account details THEN codes uppercased and us region uppercased`() {
        // Arrange
        val draft = NewRecipientDraft(
            isThirdParty = true,
            accountType = BankAccountType.SAVINGS,
            fields = personalFields,
        )

        // Act
        val actual = draft.toBankAccountDetails(rail = BankRail.WIRE, isBusiness = false)

        // Assert
        assertThat(actual).isEqualTo(
            BankAccountDetails(
                currency = "USD",
                rail = BankRail.WIRE,
                isThirdParty = true,
                beneficiary = Beneficiary.Individual(firstName = "Basit", lastName = "Didenko"),
                beneficiaryAddress = PostalAddress(
                    line1 = "1 Main St",
                    line2 = null,
                    city = "New York",
                    region = "NY",
                    postalCode = "10001",
                    countryCode = "US",
                ),
                bankName = "Test Bank",
                bankAddress = PostalAddress(
                    line1 = "2 Bank St",
                    line2 = null,
                    city = "San Francisco",
                    region = "CA",
                    postalCode = "94105",
                    countryCode = "US",
                ),
                accountNumber = "4830019271",
                routingNumber = "021000021",
                accountType = BankAccountType.SAVINGS,
            ),
        )
    }

    @Test
    fun `GIVEN complete business draft WHEN to bank account details THEN business beneficiary`() {
        // Arrange
        val draft = answeredDraft(fields = businessFields, isThirdParty = true)

        // Act
        val actual = draft.toBankAccountDetails(rail = BankRail.ACH, isBusiness = true)

        // Assert
        assertThat(actual.beneficiary).isEqualTo(Beneficiary.Business(name = "Raxanax"))
        assertThat(actual.isThirdParty).isTrue()
        assertThat(actual.accountType).isEqualTo(BankAccountType.CHECKING)
    }

    @Test
    fun `GIVEN business fields WHEN checked as personal THEN incomplete`() {
        // Arrange
        val draft = answeredDraft(fields = businessFields)

        // Assert
        assertThat(draft.isComplete(isBusiness = true)).isTrue()
        assertThat(draft.isComplete(isBusiness = false)).isFalse()
    }

    @Test
    fun `GIVEN blank bank name WHEN is complete THEN false`() {
        // Arrange
        val draft = answeredDraft(fields = personalFields + (BankWithdrawFormField.BankName to "  "))

        // Assert
        assertThat(draft.isComplete(isBusiness = false)).isFalse()
    }

    @Test
    fun `GIVEN no bank region WHEN is complete THEN false`() {
        // Arrange
        val draft = answeredDraft(fields = personalFields - BankWithdrawFormField.BankRegion)

        // Assert
        assertThat(draft.isComplete(isBusiness = false)).isFalse()
    }

    @Test
    fun `GIVEN draft without transfer type WHEN is complete THEN false`() {
        // Arrange
        val draft = NewRecipientDraft(accountType = BankAccountType.CHECKING, fields = personalFields)

        // Assert
        assertThat(draft.isComplete(isBusiness = false)).isFalse()
    }

    @Test
    fun `GIVEN draft without account type WHEN is complete THEN false`() {
        // Arrange
        val draft = NewRecipientDraft(isThirdParty = false, fields = personalFields)

        // Assert
        assertThat(draft.isComplete(isBusiness = false)).isFalse()
    }

    @Test
    fun `GIVEN wizard draft without destination WHEN to new bank account details THEN null`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = answeredDraft(fields = personalFields),
        )

        // Assert
        assertThat(draft.toNewBankAccountDetails()).isNull()
    }

    @Test
    fun `GIVEN complete wizard draft WHEN to new bank account details THEN details with chosen rail`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewBusiness,
            newRecipient = answeredDraft(fields = businessFields, isThirdParty = true),
            destination = offrampDestination(rail = BankRail.WIRE),
        )

        // Act
        val actual = draft.toNewBankAccountDetails()

        // Assert
        assertThat(actual?.rail).isEqualTo(BankRail.WIRE)
        assertThat(actual?.beneficiary).isEqualTo(Beneficiary.Business(name = "Raxanax"))
    }

    @Test
    fun `GIVEN incomplete wizard draft WHEN to new bank account details THEN null`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = answeredDraft(fields = personalFields - BankWithdrawFormField.City),
            destination = offrampDestination(),
        )

        // Assert
        assertThat(draft.toNewBankAccountDetails()).isNull()
    }

    /** A draft with both choice steps answered — the only state in which the form fields decide completeness. */
    private fun answeredDraft(
        fields: Map<BankWithdrawFormField, String>,
        isThirdParty: Boolean = false,
        accountType: BankAccountType = BankAccountType.CHECKING,
    ) = NewRecipientDraft(isThirdParty = isThirdParty, accountType = accountType, fields = fields)
}