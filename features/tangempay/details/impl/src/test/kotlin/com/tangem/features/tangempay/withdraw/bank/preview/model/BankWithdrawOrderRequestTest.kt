package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.POLYGON_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_POLYGON
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawOrderRequestTest {

    private val businessFormValues = personalFormValues
        .minus(BankWithdrawFormField.FirstName)
        .minus(BankWithdrawFormField.LastName)
        .plus(BankWithdrawFormField.BusinessName to "Arasaka")

    @Test
    fun `GIVEN saved recipient WHEN toOrderRequest THEN saved recipient id and amount in cents`() {
        // Arrange
        val draft = savedDraft().copy(amount = BigDecimal("500.5"))

        // Act
        val actual = draft.toOrderRequest()

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpOrderRequest(
                amountInCents = 50_050L,
                chainId = POLYGON_CHAIN_ID,
                tokenContractAddress = USDC_POLYGON,
                rail = BankRail.ACH,
                recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-1"),
            ),
        )
    }

    @Test
    fun `GIVEN complete new recipient WHEN toOrderRequest THEN new recipient details carry the selected rail`() {
        // Arrange
        val draft = newDraft().copy(destination = offrampDestination(rail = BankRail.WIRE))

        // Act
        val actual = draft.toOrderRequest()

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpOrderRequest(
                amountInCents = 50_000L,
                chainId = POLYGON_CHAIN_ID,
                tokenContractAddress = USDC_POLYGON,
                rail = BankRail.WIRE,
                recipient = BankOfframpRecipient.New(
                    BankAccountDetails(
                        currency = "USD",
                        rail = BankRail.WIRE,
                        isThirdParty = false,
                        beneficiary = Beneficiary.Individual(firstName = "Saburo", lastName = "Arasaka"),
                        beneficiaryAddress = PostalAddress(
                            line1 = "Megatower H10, 322F",
                            line2 = null,
                            city = "Night City",
                            region = "CA",
                            postalCode = "90001",
                            countryCode = "US",
                        ),
                        bankName = "Arasaka Bank",
                        bankAddress = PostalAddress(
                            line1 = "Ebunike st., 32A",
                            line2 = null,
                            city = "Night City",
                            region = "CA",
                            postalCode = "90002",
                            countryCode = "US",
                        ),
                        accountNumber = "1234567890",
                        routingNumber = "021000021",
                        accountType = BankAccountType.CHECKING,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN new business recipient WHEN toOrderRequest THEN third party is true`() {
        // Arrange
        val business = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewBusiness,
            amount = BigDecimal("500"),
            source = bankWithdrawSource(),
            destination = offrampDestination(),
        )
        val draft = business.copy(
            newRecipient = business.newRecipient.copy(
                accountType = BankAccountType.CHECKING,
                fields = businessFormValues,
            ),
        )

        // Act
        val actual = draft.toOrderRequest()

        // Assert
        val details = (actual?.recipient as? BankOfframpRecipient.New)?.details
        assertThat(details?.isThirdParty).isTrue()
        assertThat(details?.beneficiary).isEqualTo(Beneficiary.Business(name = "Arasaka"))
    }

    @Test
    fun `GIVEN amount with more than two decimals WHEN toOrderRequest THEN cents rounded down`() {
        // Arrange
        val draft = savedDraft().copy(amount = BigDecimal("500.555"))

        // Act
        val actual = requireNotNull(draft.toOrderRequest())

        // Assert
        assertThat(actual.amountInCents).isEqualTo(50_055L)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN incomplete draft WHEN toOrderRequest THEN null`(draft: BankWithdrawDraft) {
        // Act
        val actual = draft.toOrderRequest()

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        savedDraft().copy(amount = null),
        savedDraft().copy(source = null),
        savedDraft().copy(destination = null),
        savedDraft().copy(recipient = null),
        newDraft().copy(newRecipient = answeredRecipient(personalFormValues - BankWithdrawFormField.BankName)),
        newDraft().copy(newRecipient = NewRecipientDraft(fields = personalFormValues)),
    )

    private fun savedDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.Saved("acc-1"),
        recipient = bankAccount(id = "acc-1"),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun newDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.NewPersonal,
        newRecipient = answeredRecipient(personalFormValues),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun answeredRecipient(fields: Map<BankWithdrawFormField, String>) = NewRecipientDraft(
        isThirdParty = false,
        accountType = BankAccountType.CHECKING,
        fields = fields,
    )
}