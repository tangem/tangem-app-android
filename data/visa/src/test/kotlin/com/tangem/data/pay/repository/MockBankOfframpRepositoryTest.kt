package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MockBankOfframpRepositoryTest {

    private var nowMillis = 1_000_000L
    private lateinit var repository: MockBankOfframpRepository

    @BeforeEach
    fun createRepository() {
        nowMillis = 1_000_000L
        repository = MockBankOfframpRepository(currentTimeMillis = { nowMillis })
    }

    @Test
    fun `GIVEN fresh repository WHEN getBankAccounts THEN five seeded recipients`() = runTest {
        // Act
        val actual = repository.getBankAccounts(WALLET_ID)

        // Assert
        val accounts = actual.getOrNull()
        assertThat(accounts).hasSize(5)
        assertThat(accounts?.map { it.displayName }).contains("Saburo Arasaka")
    }

    @Test
    fun `GIVEN fresh repository WHEN getOptions THEN three USDC sources and two rails`() = runTest {
        // Act
        val actual = repository.getOptions(WALLET_ID)

        // Assert
        val options = actual.getOrNull()
        assertThat(options?.sources?.map { it.chainId }).containsExactly(137L, 8453L, 42161L).inOrder()
        assertThat(options?.destinations?.map { it.rail }).containsExactly(BankRail.ACH, BankRail.WIRE).inOrder()
    }

    @Test
    fun `GIVEN saved recipient WHEN createOrder THEN order id and same bank account id`() = runTest {
        // Act
        val actual = repository.createOrder(WALLET_ID, request(recipient = BankOfframpRecipient.Saved("mock-bank-account-1")), KEY)

        // Assert
        val result = actual.getOrNull()
        assertThat(result?.orderId).isNotEmpty()
        assertThat(result?.bankAccountId).isEqualTo("mock-bank-account-1")
    }

    @Test
    fun `GIVEN new recipient WHEN createOrder THEN recipient saved with derived display name`() = runTest {
        // Arrange
        val details = details(beneficiary = Beneficiary.Individual(firstName = "Ada", lastName = "Lovelace"))

        // Act
        val created = repository.createOrder(WALLET_ID, request(recipient = BankOfframpRecipient.New(details)), KEY)
        val accounts = repository.getBankAccounts(WALLET_ID)

        // Assert
        val result = requireNotNull(created.getOrNull())
        val saved = accounts.getOrNull()?.single { it.id == result.bankAccountId }
        assertThat(saved?.displayName).isEqualTo("Ada Lovelace")
        assertThat(saved?.details).isEqualTo(details)
    }

    @Test
    fun `GIVEN unknown saved recipient WHEN createOrder THEN validation error`() = runTest {
        // Act
        val actual = repository.createOrder(WALLET_ID, request(recipient = BankOfframpRecipient.Saved("nope")), KEY)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Validation.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN error trigger WHEN createOrder THEN mapped error`(model: TriggerModel) = runTest {
        // Act
        val actual = repository.createOrder(WALLET_ID, model.request, KEY)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    @Test
    fun `GIVEN created order WHEN time passes THEN processing then awaiting signature then completed after signing`() = runTest {
        // Arrange
        val orderId = createOrderId()

        // Act & Assert
        val processing = BankOfframpOrderState.Processing(OrderStep.UNKNOWN).right()
        assertThat(repository.getOrderState(WALLET_ID, orderId)).isEqualTo(processing)
        nowMillis += 5_999L
        assertThat(repository.getOrderState(WALLET_ID, orderId)).isEqualTo(processing)
        nowMillis += 1L
        assertThat(repository.getOrderState(WALLET_ID, orderId).getOrNull())
            .isInstanceOf(BankOfframpOrderState.AwaitingSignature::class.java)
        assertThat(repository.submitSignature(WALLET_ID, orderId, "0xsig")).isEqualTo(Unit.right())
        assertThat(repository.getOrderState(WALLET_ID, orderId)).isEqualTo(BankOfframpOrderState.Completed.right())
    }

    @Test
    fun `GIVEN order still processing WHEN submitSignature THEN unknown error`() = runTest {
        // Arrange
        val orderId = createOrderId()

        // Act
        val actual = repository.submitSignature(WALLET_ID, orderId, "0xsig")

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    @Test
    fun `GIVEN created order WHEN cancelOrder THEN state is canceled`() = runTest {
        // Arrange
        val orderId = createOrderId()

        // Act
        val canceled = repository.cancelOrder(WALLET_ID, orderId)

        // Assert
        assertThat(canceled).isEqualTo(Unit.right())
        assertThat(repository.getOrderState(WALLET_ID, orderId)).isEqualTo(BankOfframpOrderState.Canceled.right())
    }

    @Test
    fun `GIVEN unknown order WHEN getOrderState THEN unknown error`() = runTest {
        // Act
        val actual = repository.getOrderState(WALLET_ID, "missing")

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    @Test
    fun `GIVEN failed trigger amount WHEN processing time passes THEN state is Failed`() = runTest {
        // Arrange
        val created = repository.createOrder(WALLET_ID, request(amountInCents = 88_800L), KEY)
        val orderId = requireNotNull(created.getOrNull()).orderId

        // Act
        nowMillis += 6_000L
        val actual = repository.getOrderState(WALLET_ID, orderId)

        // Assert
        assertThat(actual.getOrNull()).isInstanceOf(BankOfframpOrderState.Failed::class.java)
    }

    @Test
    fun `GIVEN stuck trigger amount WHEN a long time passes THEN order stays processing`() = runTest {
        // Arrange
        val created = repository.createOrder(WALLET_ID, request(amountInCents = 99_900L), KEY)
        val orderId = requireNotNull(created.getOrNull()).orderId

        // Act
        nowMillis += 600_000L
        val actual = repository.getOrderState(WALLET_ID, orderId)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpOrderState.Processing(OrderStep.UNKNOWN).right())
    }

    @Test
    fun `GIVEN signed order WHEN cancelOrder THEN unknown error and order stays completed`() = runTest {
        // Arrange
        val orderId = createOrderId()
        nowMillis += 6_000L
        repository.submitSignature(WALLET_ID, orderId, "0xsig")

        // Act
        val actual = repository.cancelOrder(WALLET_ID, orderId)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
        assertThat(repository.getOrderState(WALLET_ID, orderId)).isEqualTo(BankOfframpOrderState.Completed.right())
    }

    private suspend fun createOrderId(): String {
        return requireNotNull(repository.createOrder(WALLET_ID, request(), KEY).getOrNull()).orderId
    }

    private fun provideTestModels() = listOf(
        TriggerModel(
            request = request(recipient = BankOfframpRecipient.New(details(routingNumber = "000000001"))),
            expected = BankOfframpError.Validation,
        ),
        TriggerModel(
            request = request(recipient = BankOfframpRecipient.New(details(countryCode = "DE"))),
            expected = BankOfframpError.RecipientRegionUnsupported,
        ),
        TriggerModel(
            request = request(amountInCents = 66_600L),
            expected = BankOfframpError.SenderRegionUnsupported,
        ),
        TriggerModel(
            request = request(amountInCents = 77_700L),
            expected = BankOfframpError.WithdrawalInProgress,
        ),
        TriggerModel(
            request = request(recipient = BankOfframpRecipient.New(details(beneficiary = Beneficiary.Business(name = "blocked")))),
            expected = BankOfframpError.BusinessUnsupported,
        ),
    )

    internal data class TriggerModel(val request: BankOfframpOrderRequest, val expected: BankOfframpError)

    private fun request(
        amountInCents: Long = 50_000L,
        recipient: BankOfframpRecipient = BankOfframpRecipient.Saved("mock-bank-account-1"),
    ) = BankOfframpOrderRequest(
        amountInCents = amountInCents,
        chainId = 137L,
        tokenContractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
        rail = BankRail.ACH,
        recipient = recipient,
    )

    private fun details(
        beneficiary: Beneficiary = Beneficiary.Individual(firstName = "John", lastName = "Doe"),
        routingNumber: String = "021000021",
        countryCode: String = "US",
    ) = BankAccountDetails(
        currency = "USD",
        rail = BankRail.ACH,
        isThirdParty = false,
        beneficiary = beneficiary,
        beneficiaryAddress = PostalAddress(
            line1 = "1 Main St",
            line2 = null,
            city = "New York",
            region = "NY",
            postalCode = "10001",
            countryCode = countryCode,
        ),
        bankName = "Test Bank",
        bankAddress = PostalAddress(
            line1 = "2 Bank St",
            line2 = null,
            city = "New York",
            region = "NY",
            postalCode = "10001",
            countryCode = "US",
        ),
        accountNumber = "1234567890",
        routingNumber = routingNumber,
        accountType = BankAccountType.CHECKING,
    )

    private companion object {
        val WALLET_ID = UserWalletId("0123456789abcdef")
        const val KEY = "idempotency-key"
    }
}