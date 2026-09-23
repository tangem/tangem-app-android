package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
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
        assertThat(actual).isEqualTo(
            BankAccount(id = "account-1", displayName = "Primary checking account", details = expectedDetails()),
        )
    }

    @Test
    fun `GIVEN business beneficiary WHEN convert THEN business name read from business_name`() {
        // Arrange
        val beneficiary = beneficiary(type = "BUSINESS", firstName = null, lastName = null, businessName = "Raxanax")

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
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(accountType = BankAccountType.SAVINGS))
    }

    @Test
    fun `GIVEN wire recipient without an account type WHEN convert THEN account type is null`() {
        // Act
        val actual = BankAccountConverter.convert(response(rail = "WIRE", accountType = null))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(rail = BankRail.WIRE, accountType = null))
    }

    @Test
    fun `GIVEN recipient without a routing number WHEN convert THEN account kept with a null routing number`() {
        // Act
        val actual = BankAccountConverter.convert(response(routingNumber = null))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(routingNumber = null))
    }

    @Test
    fun `GIVEN recipient without a rail WHEN convert THEN account kept with a null rail`() {
        // Act
        val actual = BankAccountConverter.convert(response(rail = null))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(rail = null))
    }

    @Test
    fun `GIVEN recipient with an unknown rail WHEN convert THEN account kept with a null rail`() {
        // Act
        val actual = BankAccountConverter.convert(response(rail = "SEPA"))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(rail = null))
    }

    @Test
    fun `GIVEN recipient without a currency WHEN convert THEN account kept with a null currency`() {
        // Act
        val actual = BankAccountConverter.convert(response(currency = null))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(currency = null))
    }

    @Test
    fun `GIVEN recipient with a partial bank address WHEN convert THEN address dropped and account kept`() {
        // Act
        val actual = BankAccountConverter.convert(response(bankAddress = address(city = null)))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(bankAddress = null))
    }

    @Test
    fun `GIVEN recipient with a partial beneficiary address WHEN convert THEN address dropped and account kept`() {
        // Act
        val actual = BankAccountConverter.convert(response(beneficiaryAddress = address(city = null)))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(beneficiaryAddress = null))
    }

    @Test
    fun `GIVEN recipient without a bank address WHEN convert THEN account kept with a null bank address`() {
        // Act
        val actual = BankAccountConverter.convert(response(bankAddress = null))

        // Assert
        assertThat(actual?.details).isEqualTo(expectedDetails().copy(bankAddress = null))
    }

    @Test
    fun `GIVEN recipient without an account number WHEN convert THEN dropped`() {
        // Act
        val actual = BankAccountConverter.convert(response(accountNumber = null))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN recipient without a beneficiary name WHEN convert THEN dropped`() {
        // Act
        val actual = BankAccountConverter.convert(response(beneficiary = beneficiary(lastName = null)))

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN unidentifiable recipient WHEN convert THEN dropped`(model: IncompleteModel) {
        // Act
        val actual = BankAccountConverter.convert(model.response)

        // Assert
        assertThat(actual).isNull()
    }

    internal data class IncompleteModel(val name: String, val response: BankAccountsResponse.BankAccount)

    private fun provideTestModels() = listOf(
        IncompleteModel("no details", response().copy(details = null)),
        IncompleteModel("no beneficiary", response(beneficiary = null)),
        IncompleteModel(
            "nameless business",
            response(beneficiary = beneficiary(type = "BUSINESS", firstName = null, lastName = null)),
        ),
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
        businessName: String? = null,
    ) = BankAccountsResponse.Beneficiary(
        type = type,
        firstName = firstName,
        lastName = lastName,
        businessName = businessName,
    )

    private fun address(city: String? = "New York", line2: String? = null) = BankAccountsResponse.Address(
        line1 = "123 Main St",
        line2 = line2,
        city = city,
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private fun expectedDetails() = BankAccountDetails(
        currency = "USD",
        rail = BankRail.ACH,
        isThirdParty = false,
        beneficiary = Beneficiary.Individual("John", "Doe"),
        beneficiaryAddress = postalAddress(line2 = "Apt 4B"),
        bankName = "Chase Bank",
        bankAddress = postalAddress(),
        accountNumber = "1234567890",
        routingNumber = "021000021",
        accountType = BankAccountType.CHECKING,
    )

    private fun postalAddress(line2: String? = null) = PostalAddress(
        line1 = "123 Main St",
        line2 = line2,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )
}