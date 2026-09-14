package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.spend.datasource.pay.models.response.BankAccountsResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankAccountConverterTest {

    @Test
    fun `GIVEN full individual recipient WHEN convert THEN every field mapped`() {
        // Act
        val actual = BankAccountConverter.convert(response())

        // Assert
        val details = requireNotNull(actual).details
        assertThat(actual.id).isEqualTo("account-1")
        assertThat(actual.displayName).isEqualTo("Primary checking account")
        assertThat(details.currency).isEqualTo("USD")
        assertThat(details.rail).isEqualTo(BankRail.ACH)
        assertThat(details.isThirdParty).isFalse()
        assertThat(details.beneficiary).isEqualTo(Beneficiary.Individual("John", "Doe"))
        assertThat(details.beneficiaryAddress.line2).isEqualTo("Apt 4B")
        assertThat(details.bankAddress.line2).isNull()
        assertThat(details.accountNumber).isEqualTo("1234567890")
        assertThat(details.routingNumber).isEqualTo("021000021")
        assertThat(details.accountType).isEqualTo(BankAccountType.CHECKING)
    }

    @Test
    fun `GIVEN business beneficiary WHEN convert THEN company name kept`() {
        // Arrange
        val beneficiary = beneficiary(type = "BUSINESS", firstName = null, lastName = null, name = "Raxanax")

        // Act
        val actual = BankAccountConverter.convert(response(beneficiary = beneficiary))

        // Assert
        assertThat(actual?.details?.beneficiary).isEqualTo(Beneficiary.Business("Raxanax"))
    }

    @Test
    fun `GIVEN blank display name WHEN convert THEN name derived from beneficiary`() {
        // Act
        val actual = BankAccountConverter.convert(response(displayName = " "))

        // Assert
        assertThat(actual?.displayName).isEqualTo("John Doe")
    }

    @Test
    fun `GIVEN savings account type WHEN convert THEN savings kept`() {
        // Act
        val actual = BankAccountConverter.convert(response(accountType = "savings"))

        // Assert
        assertThat(actual?.details?.accountType).isEqualTo(BankAccountType.SAVINGS)
    }

    @Test
    fun `GIVEN wire recipient without account type WHEN convert THEN falls back to checking`() {
        // Act
        val actual = BankAccountConverter.convert(response(rail = "WIRE", accountType = null))

        // Assert
        assertThat(actual?.details?.rail).isEqualTo(BankRail.WIRE)
        assertThat(actual?.details?.accountType).isEqualTo(BankAccountType.CHECKING)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN incomplete recipient WHEN convert THEN dropped`(model: IncompleteModel) {
        // Act
        val actual = BankAccountConverter.convert(model.response)

        // Assert
        assertThat(actual).isNull()
    }

    internal data class IncompleteModel(val name: String, val response: BankAccountsResponse.BankAccount)

    private fun provideTestModels() = listOf(
        IncompleteModel("no details", response().copy(details = null)),
        IncompleteModel("unknown rail", response(rail = "SEPA")),
        IncompleteModel("no rail", response(rail = null)),
        IncompleteModel("no currency", response(currency = null)),
        IncompleteModel("no routing number", response(routingNumber = null)),
        IncompleteModel("blank account number", response(accountNumber = "")),
        IncompleteModel("no beneficiary", response(beneficiary = null)),
        IncompleteModel("nameless individual", response(beneficiary = beneficiary(lastName = null))),
        IncompleteModel("nameless business", response(beneficiary = beneficiary(type = "BUSINESS", name = null))),
        IncompleteModel("address without city", response(beneficiaryAddress = address(city = null))),
        IncompleteModel("no bank address", response(bankAddress = null)),
    )

    @Suppress("LongParameterList")
    private fun response(
        displayName: String? = "Primary checking account",
        currency: String? = "USD",
        rail: String? = "ACH",
        beneficiary: BankAccountsResponse.Beneficiary? = beneficiary(),
        beneficiaryAddress: BankAccountsResponse.Address? = address(line2 = "Apt 4B"),
        bankAddress: BankAccountsResponse.Address? = address(),
        accountNumber: String? = "1234567890",
        routingNumber: String? = "021000021",
        accountType: String? = "CHECKING",
    ) = BankAccountsResponse.BankAccount(
        id = "account-1",
        displayName = displayName,
        details = BankAccountsResponse.Details(
            currency = currency,
            rail = rail,
            isThirdParty = false,
            beneficiary = beneficiary,
            beneficiaryAddress = beneficiaryAddress,
            bankName = "Chase Bank",
            bankAddress = bankAddress,
            accountNumber = accountNumber,
            routingNumber = routingNumber,
            bankAccountType = accountType,
        ),
        createdAt = "2026-08-12T10:15:30Z",
    )

    private fun beneficiary(
        type: String? = "INDIVIDUAL",
        firstName: String? = "John",
        lastName: String? = "Doe",
        name: String? = null,
    ) = BankAccountsResponse.Beneficiary(type = type, firstName = firstName, lastName = lastName, name = name)

    private fun address(city: String? = "New York", line2: String? = null) = BankAccountsResponse.Address(
        line1 = "123 Main St",
        line2 = line2,
        city = city,
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )
}