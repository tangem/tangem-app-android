package com.tangem.data.pay.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MockBankOfframpSpeiCatalogTest {

    @Test
    fun `GIVEN the mock SPEI recipient as a new recipient WHEN createQuote THEN it passes the real request checks`() =
        runTest {
            // Arrange
            val repository = MockBankOfframpOrderRepository(currentTimeMillis = { 1_000_000L })

            // Act
            val actual = repository.createQuote(
                userWalletId = UserWalletId("0123456789abcdef"),
                productInstanceId = "123e4567-e89b-12d3-a456-426614174000",
                amount = BigDecimal("100"),
                recipient = BankOfframpRecipient.New(MOCK_SPEI_RECIPIENT.details),
                idempotencyKey = "idempotency-key",
            )

            // Assert
            assertThat(actual.getOrNull()?.bankAccountId).isEqualTo("mock-bank-account-new-1")
        }

    @ParameterizedTest
    @MethodSource("provideMockSpeiRecipientModels")
    fun `GIVEN backend recipients WHEN withMockSpeiRecipient THEN the SPEI recipient added once within the rail`(
        model: MockSpeiRecipientModel,
    ) {
        // Act
        val actual = withMockSpeiRecipient(accounts = model.accounts, rail = model.rail)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    @ParameterizedTest
    @MethodSource("provideMockSpeiDestinationModels")
    fun `GIVEN backend options WHEN withMockSpeiDestination THEN SPEI added once and only for the USD account`(
        model: MockSpeiDestinationModel,
    ) {
        // Act
        val actual = withMockSpeiDestination(options = model.options, product = model.product)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideMockSpeiRecipientModels(): List<MockSpeiRecipientModel> {
        val usdRecipient = BankAccount(id = "mock-bank-account-1", displayName = "John Doe", details = usdDetails())
        return listOf(
            MockSpeiRecipientModel(
                accounts = listOf(usdRecipient),
                rail = null,
                expected = listOf(usdRecipient, MOCK_SPEI_RECIPIENT),
            ),
            MockSpeiRecipientModel(
                accounts = listOf(usdRecipient),
                rail = BankRail.SPEI,
                expected = listOf(MOCK_SPEI_RECIPIENT),
            ),
            MockSpeiRecipientModel(
                accounts = listOf(usdRecipient),
                rail = BankRail.ACH,
                expected = listOf(usdRecipient),
            ),
            MockSpeiRecipientModel(
                accounts = listOf(MOCK_SPEI_RECIPIENT),
                rail = null,
                expected = listOf(MOCK_SPEI_RECIPIENT),
            ),
        )
    }

    private fun provideMockSpeiDestinationModels(): List<MockSpeiDestinationModel> {
        val ach = OfframpDestination(
            rail = BankRail.ACH,
            currency = "USD",
            fee = OfframpFee(
                name = "ACH",
                calculationType = AccountFee.CalculationType.FIXED,
                amount = BigDecimal("1.50"),
                currency = "USD",
                description = null,
            ),
            eta = OfframpEta(minDays = 1, maxDays = 3),
        )
        val backendSpei = ach.copy(rail = BankRail.SPEI, currency = "MXN")
        return listOf(
            MockSpeiDestinationModel(
                options = options(ach),
                product = TangemPayProduct.BANKING_ACCOUNT,
                expected = options(ach, MOCK_SPEI_DESTINATION),
            ),
            MockSpeiDestinationModel(
                options = options(ach),
                product = TangemPayProduct.PAYMENT_ACCOUNT,
                expected = options(ach),
            ),
            MockSpeiDestinationModel(
                options = options(ach, backendSpei),
                product = TangemPayProduct.BANKING_ACCOUNT,
                expected = options(ach, backendSpei),
            ),
        )
    }

    private fun options(vararg destinations: OfframpDestination) =
        BankOfframpOptions(sources = emptyList(), destinations = destinations.toList())

    internal data class MockSpeiRecipientModel(
        val accounts: List<BankAccount>,
        val rail: BankRail?,
        val expected: List<BankAccount>,
    )

    internal data class MockSpeiDestinationModel(
        val options: BankOfframpOptions,
        val product: TangemPayProduct,
        val expected: BankOfframpOptions,
    )

    private fun usdDetails() = BankAccountDetails(
        currency = "USD",
        rail = BankRail.ACH,
        isThirdParty = false,
        beneficiary = Beneficiary.Individual(firstName = "John", lastName = "Doe"),
        beneficiaryAddress = null,
        bankName = "Test Bank",
        bankAddress = null,
        accountNumber = "1234567890",
        routingNumber = "021000021",
        accountType = BankAccountType.CHECKING,
    )
}