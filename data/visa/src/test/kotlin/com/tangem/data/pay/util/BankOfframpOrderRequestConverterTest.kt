package com.tangem.data.pay.util

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.spend.datasource.pay.models.request.BankOfframpPlaceOrderRequest
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankOfframpOrderRequestConverterTest {

    @Test
    fun `GIVEN saved recipient WHEN convert THEN only the bank account id sent`() {
        // Act
        val actual = BankOfframpOrderRequestConverter.convert(
            orderRequest(recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-1")),
        )

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpPlaceOrderRequest(
                data = BankOfframpPlaceOrderRequest.Data(
                    chainId = 137L,
                    amountInCents = 10_000L,
                    tokenContractAddress = TOKEN_ADDRESS,
                    bankAccountId = "acc-1",
                ),
            ).right(),
        )
    }

    @Test
    fun `GIVEN new individual recipient WHEN convert THEN the full details sent with the selected rail`() {
        // Act
        val actual = BankOfframpOrderRequestConverter.convert(
            orderRequest(recipient = BankOfframpRecipient.New(details = details())),
        )

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpPlaceOrderRequest(
                data = BankOfframpPlaceOrderRequest.Data(
                    chainId = 137L,
                    amountInCents = 10_000L,
                    tokenContractAddress = TOKEN_ADDRESS,
                    bankAccount = BankOfframpPlaceOrderRequest.BankAccount(
                        details = BankOfframpPlaceOrderRequest.Details(
                            currency = "USD",
                            rail = "WIRE",
                            isThirdParty = false,
                            beneficiary = BankOfframpPlaceOrderRequest.Beneficiary(
                                type = "INDIVIDUAL",
                                firstName = "John",
                                lastName = "Doe",
                            ),
                            beneficiaryAddress = BankOfframpPlaceOrderRequest.Address(
                                line1 = "123 Main St",
                                line2 = "Apt 4",
                                city = "New York",
                                region = "NY",
                                postalCode = "10001",
                                countryCode = "US",
                            ),
                            bankName = "Chase Bank",
                            bankAddress = BankOfframpPlaceOrderRequest.Address(
                                line1 = "123 Main St",
                                line2 = null,
                                city = "New York",
                                region = "NY",
                                postalCode = "10001",
                                countryCode = "US",
                            ),
                            accountNumber = "1234567890",
                            routingNumber = "021000021",
                            bankAccountType = "SAVINGS",
                        ),
                    ),
                ),
            ).right(),
        )
    }

    @Test
    fun `GIVEN business recipient WHEN convert THEN business beneficiary sent without personal names`() {
        // Act
        val actual = BankOfframpOrderRequestConverter.convert(
            orderRequest(
                recipient = BankOfframpRecipient.New(
                    details = details(beneficiary = Beneficiary.Business(name = "Tangem AG")),
                ),
            ),
        )

        // Assert
        val beneficiary = actual.getOrNull()?.data?.bankAccount?.details?.beneficiary
        assertThat(beneficiary).isEqualTo(
            BankOfframpPlaceOrderRequest.Beneficiary(type = "BUSINESS", businessName = "Tangem AG"),
        )
    }

    @Test
    fun `GIVEN the recipient rail differs from the route WHEN convert THEN the route rail sent`() {
        // Act
        val actual = BankOfframpOrderRequestConverter.convert(
            orderRequest(
                rail = BankRail.ACH,
                recipient = BankOfframpRecipient.New(details = details(rail = BankRail.WIRE)),
            ),
        )

        // Assert
        assertThat(actual.getOrNull()?.data?.bankAccount?.details?.rail).isEqualTo("ACH")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a new recipient missing a required field WHEN convert THEN validation error`(model: IncompleteModel) {
        // Act
        val actual = BankOfframpOrderRequestConverter.convert(
            orderRequest(recipient = BankOfframpRecipient.New(details = model.details)),
        )

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Validation.left())
    }

    internal data class IncompleteModel(val name: String, val details: BankAccountDetails)

    private fun provideTestModels() = listOf(
        IncompleteModel("no currency", details().copy(currency = null)),
        IncompleteModel("no beneficiary address", details().copy(beneficiaryAddress = null)),
        IncompleteModel("no bank name", details().copy(bankName = null)),
        IncompleteModel("no bank address", details().copy(bankAddress = null)),
        IncompleteModel("no routing number", details().copy(routingNumber = null)),
        IncompleteModel("no account type", details().copy(accountType = null)),
    )

    private fun orderRequest(
        recipient: BankOfframpRecipient,
        rail: BankRail = BankRail.WIRE,
    ) = BankOfframpOrderRequest(
        amountInCents = 10_000L,
        chainId = 137L,
        tokenContractAddress = TOKEN_ADDRESS,
        tokenDecimals = 6,
        rail = rail,
        recipient = recipient,
    )

    private fun details(
        beneficiary: Beneficiary = Beneficiary.Individual(firstName = "John", lastName = "Doe"),
        rail: BankRail = BankRail.WIRE,
    ) = BankAccountDetails(
        currency = "USD",
        rail = rail,
        isThirdParty = false,
        beneficiary = beneficiary,
        beneficiaryAddress = address(line2 = "Apt 4"),
        bankName = "Chase Bank",
        bankAddress = address(),
        accountNumber = "1234567890",
        routingNumber = "021000021",
        accountType = BankAccountType.SAVINGS,
    )

    private fun address(line2: String? = null) = PostalAddress(
        line1 = "123 Main St",
        line2 = line2,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private companion object {
        const val TOKEN_ADDRESS = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
    }
}