package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.BankOfframpPlaceOrderRequest
import com.tangem.spend.datasource.pay.models.request.BankOfframpSignatureRequest
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.StructuredDataEncoder

@Suppress("UNCHECKED_CAST")
internal class DefaultBankOfframpOrderRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestPerformer: TangemPayRequestPerformer = mockk()

    private val repository = DefaultBankOfframpOrderRepository(
        tangemPayApi = tangemPayApi,
        requestPerformer = requestPerformer,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestPerformer)

        coEvery {
            requestPerformer.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }

    @Test
    fun `GIVEN saved recipient WHEN createOrder THEN the bank account id and the idempotency header sent`() = runTest {
        // Arrange
        val body = slot<BankOfframpPlaceOrderRequest>()
        coEvery {
            tangemPayApi.createBankOfframpOrder(AUTH_HEADER, KEY, capture(body))
        } returns ApiResponse.Success(orderResponse(bankAccountId = "acc-1"))

        // Act
        val actual = repository.createOrder(WALLET_ID, orderRequest(BankOfframpRecipient.Saved("acc-1")), KEY)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpOrderResult(orderId = ORDER_ID, bankAccountId = "acc-1").right())
        assertThat(body.captured.idempotencyKey).isEqualTo(KEY)
        assertThat(body.captured.data.bankAccountId).isEqualTo("acc-1")
        assertThat(body.captured.data.bankAccount).isNull()
        coVerify(exactly = 1) { tangemPayApi.createBankOfframpOrder(AUTH_HEADER, KEY, any()) }
    }

    @Test
    fun `GIVEN new recipient WHEN createOrder THEN the full details sent`() = runTest {
        // Arrange
        val body = slot<BankOfframpPlaceOrderRequest>()
        coEvery {
            tangemPayApi.createBankOfframpOrder(AUTH_HEADER, KEY, capture(body))
        } returns ApiResponse.Success(orderResponse(bankAccountId = "acc-new"))

        val request = orderRequest(BankOfframpRecipient.New(details()))

        // Act
        val actual = repository.createOrder(WALLET_ID, request, KEY)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpOrderResult(orderId = ORDER_ID, bankAccountId = "acc-new").right())
        assertThat(body.captured).isEqualTo(expectedPlaceOrderRequest(request))
    }

    @Test
    fun `GIVEN incomplete new recipient WHEN createOrder THEN validation error and nothing sent`() = runTest {
        // Act
        val actual = repository.createOrder(
            WALLET_ID,
            orderRequest(BankOfframpRecipient.New(details().copy(routingNumber = null))),
            KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Validation.left())
        coVerify(exactly = 0) { tangemPayApi.createBankOfframpOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN api failure WHEN createOrder THEN unknown error returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.createBankOfframpOrder(AUTH_HEADER, KEY, any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<OrderResponse>

        // Act
        val actual = repository.createOrder(WALLET_ID, orderRequest(BankOfframpRecipient.Saved("acc-1")), KEY)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    @Test
    fun `GIVEN unsupported configuration WHEN createOrder THEN the unsupported configuration error returned`() =
        runTest {
            // Arrange
            coEvery { requestPerformer.performRequest<Any>(userWalletId = any(), requestBlock = any()) } returns
                VisaApiError.BankOfframpUnsupportedConfiguration.left()

            // Act
            val actual = repository.createOrder(WALLET_ID, orderRequest(BankOfframpRecipient.Saved("acc-1")), KEY)

            // Assert
            assertThat(actual).isEqualTo(BankOfframpError.UnsupportedConfiguration.left())
        }

    @Test
    fun `GIVEN order awaiting signature with a consistent hash WHEN getOrderState THEN awaiting signature returned`() =
        runTest {
            // Arrange
            coEvery { tangemPayApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(
                orderResponse(step = "AWAITING_SIGNATURE", withdrawData = withdrawData(hash = consistentHash())),
            )

            // Act
            val actual = repository.getOrderState(WALLET_ID, ORDER_ID)

            // Assert
            assertThat(actual).isEqualTo(BankOfframpOrderState.AwaitingSignature(consistentHash()).right())
        }

    @Test
    fun `GIVEN order awaiting signature with a mismatched hash WHEN getOrderState THEN failed returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(
            orderResponse(step = "AWAITING_SIGNATURE", withdrawData = withdrawData(hash = "0xdeadbeef")),
        )

        // Act
        val actual = repository.getOrderState(WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual.getOrNull()).isInstanceOf(BankOfframpOrderState.Failed::class.java)
    }

    @Test
    fun `GIVEN order awaiting signature without withdraw data WHEN getOrderState THEN failed returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(
            orderResponse(step = "AWAITING_SIGNATURE", withdrawData = null),
        )

        // Act
        val actual = repository.getOrderState(WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual.getOrNull()).isInstanceOf(BankOfframpOrderState.Failed::class.java)
    }

    @Test
    fun `GIVEN processing order WHEN getOrderState THEN processing with the parsed step`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(
            orderResponse(step = "AWAITING_DEPOSIT"),
        )

        // Act
        val actual = repository.getOrderState(WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpOrderState.Processing(OrderStep.AWAITING_DEPOSIT).right())
    }

    @Test
    fun `GIVEN completed order WHEN getOrderState THEN completed returned`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(
            orderResponse(status = OrderResponse.Result.Status.COMPLETED),
        )

        // Act
        val actual = repository.getOrderState(WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpOrderState.Completed.right())
    }

    @Test
    fun `GIVEN signature WHEN submitSignature THEN prefixed hex sent with the idempotency key`() = runTest {
        // Arrange
        val body = slot<BankOfframpSignatureRequest>()
        coEvery {
            tangemPayApi.submitBankOfframpSignature(AUTH_HEADER, KEY, ORDER_ID, capture(body))
        } returns ApiResponse.Success(Unit)

        // Act
        val actual = repository.submitSignature(WALLET_ID, ORDER_ID, "abc123", KEY)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(body.captured).isEqualTo(BankOfframpSignatureRequest(signature = "0xabc123"))
    }

    @Test
    fun `GIVEN order id WHEN cancelOrder THEN the cancel endpoint called`() = runTest {
        // Arrange
        coEvery { tangemPayApi.cancelOrder(AUTH_HEADER, ORDER_ID) } returns ApiResponse.Success(Any())

        // Act
        val actual = repository.cancelOrder(WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { tangemPayApi.cancelOrder(AUTH_HEADER, ORDER_ID) }
    }

    private fun consistentHash(): String = "0x" + StructuredDataEncoder(STRUCTURED_DATA)
        .hashStructuredData()
        .toHexString()

    private fun withdrawData(hash: String) = OrderResponse.Result.Data.WithdrawData(
        hash = hash,
        salt = "0x01",
        senderAddress = "0xSENDER",
        structuredData = STRUCTURED_DATA,
    )

    private fun orderResponse(
        status: OrderResponse.Result.Status = OrderResponse.Result.Status.PROCESSING,
        step: String? = null,
        bankAccountId: String? = null,
        withdrawData: OrderResponse.Result.Data.WithdrawData? = null,
    ) = OrderResponse(
        result = OrderResponse.Result(
            id = ORDER_ID,
            customerId = "customer-1",
            type = "BANK_OFFRAMP_RAIN",
            status = status,
            step = step,
            data = OrderResponse.Result.Data(
                type = "BANK_OFFRAMP_RAIN",
                specificationName = null,
                customerWalletAddress = null,
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
                targetTariffPlanId = null,
                transactionHash = null,
                bankAccountId = bankAccountId,
                withdrawData = withdrawData,
            ),
            stepChangeCode = null,
            createdAt = null,
            updatedAt = null,
        ),
    )

    private fun orderRequest(recipient: BankOfframpRecipient) = BankOfframpOrderRequest(
        amountInCents = 10_000L,
        chainId = 137L,
        tokenContractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
        rail = BankRail.WIRE,
        recipient = recipient,
    )

    private fun details() = BankAccountDetails(
        currency = "USD",
        rail = BankRail.WIRE,
        isThirdParty = false,
        beneficiary = Beneficiary.Individual(firstName = "John", lastName = "Doe"),
        beneficiaryAddress = address(),
        bankName = "Chase Bank",
        bankAddress = address(),
        accountNumber = "1234567890",
        routingNumber = "021000021",
        accountType = BankAccountType.SAVINGS,
    )

    private fun expectedPlaceOrderRequest(request: BankOfframpOrderRequest) = BankOfframpPlaceOrderRequest(
        data = BankOfframpPlaceOrderRequest.Data(
            chainId = request.chainId,
            amountInCents = request.amountInCents,
            tokenContractAddress = request.tokenContractAddress,
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
                    beneficiaryAddress = expectedAddress(),
                    bankName = "Chase Bank",
                    bankAddress = expectedAddress(),
                    accountNumber = "1234567890",
                    routingNumber = "021000021",
                    bankAccountType = "SAVINGS",
                ),
            ),
        ),
        idempotencyKey = KEY,
    )

    private fun expectedAddress() = BankOfframpPlaceOrderRequest.Address(
        line1 = "123 Main St",
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private fun address() = PostalAddress(
        line1 = "123 Main St",
        line2 = null,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )

    private companion object {
        val WALLET_ID = UserWalletId("0123456789abcdef")
        const val AUTH_HEADER = "Bearer token"
        const val KEY = "idempotency-key"
        const val ORDER_ID = "order-1"

        val STRUCTURED_DATA = """
            {
              "types": {
                "EIP712Domain": [
                  {"name": "name", "type": "string"},
                  {"name": "chainId", "type": "uint256"}
                ],
                "Withdraw": [
                  {"name": "sender", "type": "address"},
                  {"name": "amount", "type": "uint256"}
                ]
              },
              "primaryType": "Withdraw",
              "domain": {"name": "Rain", "chainId": 137},
              "message": {
                "sender": "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123",
                "amount": "10000000"
              }
            }
        """.trimIndent()
    }
}