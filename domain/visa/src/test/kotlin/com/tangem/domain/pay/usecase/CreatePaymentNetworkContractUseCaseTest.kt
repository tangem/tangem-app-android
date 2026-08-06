package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CreatePaymentNetworkContractUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val pollingUseCase: StartTangemPayOrderPollingUseCase = mockk()

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val network = network(Blockchain.Polygon)
    private val chainId = 137 // Polygon's EVM chain id — what the use case must derive from [network]

    // Polling is fire-and-forget in the app scope; the Unconfined test scope runs the launched poll
    // eagerly (until its first suspension), so calls are observable right after invoke() returns.
    private val useCase = CreatePaymentNetworkContractUseCase(
        customerOrderRepository = customerOrderRepository,
        pollingUseCase = pollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @Test
    fun `GIVEN active contract order WHEN invoke THEN does not create a new order and polls the existing one`() =
        runTest {
            val activeOrder = order(id = "active-order", status = OrderStatus.PROCESSING)
            coEvery {
                customerOrderRepository.findOrders(
                    userWalletId = userWalletId,
                    types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
                    statuses = OrderStatus.activeStatuses,
                )
            } returns listOf(activeOrder).right()
            coEvery { pollingUseCase.invoke(any(), any(), any(), any()) } returns true

            val result = useCase(userWalletId, network)

            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 0) {
                customerOrderRepository.createOrder(any(), any(), any(), any(), any(), any(), any())
            }
            coVerify(exactly = 1) {
                pollingUseCase.invoke(
                    TangemPayOrderInfo(orderId = "active-order", orderStatus = OrderStatus.PROCESSING),
                    userWalletId,
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `GIVEN no active order and create succeeds WHEN invoke THEN creates the order once and polls it`() = runTest {
        coEvery {
            customerOrderRepository.findOrders(
                userWalletId = userWalletId,
                types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
                statuses = OrderStatus.activeStatuses,
            )
        } returns emptyList<Order>().right()
        val createdOrder = order(id = "new-order", status = OrderStatus.NEW)
        coEvery {
            customerOrderRepository.createOrder(
                userWalletId = userWalletId,
                type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                specificationName = null,
                idempotencyKey = any(),
                chainId = chainId,
            )
        } returns createdOrder.right()
        coEvery { pollingUseCase.invoke(any(), any(), any(), any()) } returns true

        val result = useCase(userWalletId, network)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) {
            customerOrderRepository.createOrder(
                userWalletId = userWalletId,
                type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                specificationName = null,
                idempotencyKey = any(),
                chainId = chainId,
            )
        }
        coVerify(exactly = 1) {
            pollingUseCase.invoke(
                TangemPayOrderInfo(orderId = "new-order", orderStatus = OrderStatus.NEW),
                userWalletId,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `GIVEN active order WHEN invoke THEN returns before polling completes`() = runTest {
        val activeOrder = order(id = "active-order", status = OrderStatus.PROCESSING)
        coEvery {
            customerOrderRepository.findOrders(
                userWalletId = userWalletId,
                types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
                statuses = OrderStatus.activeStatuses,
            )
        } returns listOf(activeOrder).right()
        coEvery { pollingUseCase.invoke(any(), any(), any(), any()) } coAnswers {
            awaitCancellation() // a poll that never terminates must not block invoke
        }

        val result = useCase(userWalletId, network)

        assertThat(result.isRight()).isTrue()
    }

    @Test
    fun `GIVEN network without derivable chain id WHEN invoke THEN returns Left without backend calls`() = runTest {
        val result = useCase(userWalletId, network(Blockchain.Tron))

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { customerOrderRepository.findOrders(any(), any(), any()) }
        coVerify(exactly = 0) {
            customerOrderRepository.createOrder(any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { pollingUseCase.invoke(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN no active order and create fails with a 4xx error WHEN invoke THEN returns Left without retry`() =
        runTest {
            coEvery {
                customerOrderRepository.findOrders(
                    userWalletId = userWalletId,
                    types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
                    statuses = OrderStatus.activeStatuses,
                )
            } returns emptyList<Order>().right()
            coEvery {
                customerOrderRepository.createOrder(
                    userWalletId = userWalletId,
                    type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                    specificationName = null,
                    idempotencyKey = any(),
                    chainId = chainId,
                )
            } returns VisaApiError.CustomerIsBlocked.left()

            val result = useCase(userWalletId, network)

            assertThat(result.leftOrNull()).isEqualTo(VisaApiError.CustomerIsBlocked)
            coVerify(exactly = 1) {
                customerOrderRepository.createOrder(
                    userWalletId = userWalletId,
                    type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                    specificationName = null,
                    idempotencyKey = any(),
                    chainId = chainId,
                )
            }
            coVerify(exactly = 0) { pollingUseCase.invoke(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN create fails with 5xx twice then succeeds WHEN invoke THEN retries with the same key and polls`() =
        runTest {
            coEvery {
                customerOrderRepository.findOrders(
                    userWalletId = userWalletId,
                    types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
                    statuses = OrderStatus.activeStatuses,
                )
            } returns emptyList<Order>().right()
            val createdOrder = order(id = "resilient-order", status = OrderStatus.NEW)
            val idempotencyKeys = mutableListOf<String>()
            coEvery {
                customerOrderRepository.createOrder(
                    userWalletId = userWalletId,
                    type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                    specificationName = null,
                    idempotencyKey = capture(idempotencyKeys),
                    chainId = chainId,
                )
            } returnsMany listOf(
                VisaApiError.ServerUnavailable.left(),
                VisaApiError.UnknownWithoutCode.left(),
                createdOrder.right(),
            )
            coEvery { pollingUseCase.invoke(any(), any(), any(), any()) } returns true

            val result = useCase(userWalletId, network)

            assertThat(result.isRight()).isTrue()
            assertThat(idempotencyKeys).hasSize(3)
            assertThat(idempotencyKeys.toSet()).hasSize(1) // same key reused across retries
            coVerify(exactly = 3) {
                customerOrderRepository.createOrder(
                    userWalletId = userWalletId,
                    type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                    specificationName = null,
                    idempotencyKey = any(),
                    chainId = chainId,
                )
            }
            coVerify(exactly = 1) {
                pollingUseCase.invoke(
                    TangemPayOrderInfo(orderId = "resilient-order", orderStatus = OrderStatus.NEW),
                    userWalletId,
                    any(),
                    any(),
                )
            }
        }

    private fun network(blockchain: Blockchain): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = blockchain.toNetworkId(), derivationPath = derivationPath),
            name = blockchain.fullName,
            currencySymbol = blockchain.currency,
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun order(
        id: String,
        status: OrderStatus,
        type: OrderType = OrderType.SMART_CONTRACT_ISSUE_RAIN,
    ): Order = Order(
        id = id,
        customerId = null,
        type = type,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )
}