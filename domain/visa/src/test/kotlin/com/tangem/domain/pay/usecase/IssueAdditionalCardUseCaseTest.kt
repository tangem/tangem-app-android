package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.IssueAdditionalCardError
import com.tangem.domain.pay.model.IssueAdditionalCardResult
import com.tangem.domain.pay.model.CustomerOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Currency

internal class IssueAdditionalCardUseCaseTest {

    private val offersRepository: CustomerOffersRepository = mockk()
    private val orderRepository: CustomerOrderRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk(relaxed = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = IssueAdditionalCardUseCase(
        customerOffersRepository = offersRepository,
        customerOrderRepository = orderRepository,
        issueCardRepository = issueCardRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(offersRepository, orderRepository, issueCardRepository, startTangemPayOrderPollingUseCase)
        coEvery {
            orderRepository.findOrders(userWalletId = USER_WALLET_ID, types = any(), statuses = any())
        } returns emptyList<Order>().right()
    }

    @Nested
    inner class RegularPath {

        @Test
        fun `GIVEN no additional-card offer WHEN invoked THEN Other Unspecified AND no order is created`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns customerOffers().right()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(IssueAdditionalCardError.Other(VisaApiError.Unspecified))
            coVerify(exactly = 0) { orderRepository.findOrders(any(), any(), any()) }
            verifyNoOrderCreated()
        }

        @Test
        fun `GIVEN an offer without a spec name WHEN invoked THEN no order is created`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns
                customerOffers(VIRTUAL_OFFER.copy(data = VIRTUAL_OFFER.data.copy(specificationName = null))).right()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(IssueAdditionalCardError.Other(VisaApiError.Unspecified))
            verifyNoOrderCreated()
        }

        @Test
        fun `GIVEN active issue order of the offer type WHEN invoked THEN Resumed without createOrder`() = runTest {
            // Arrange
            val existing = order(id = "existing", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING)
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns customerOffers(VIRTUAL_OFFER).right()
            coEvery {
                orderRepository.findOrders(
                    userWalletId = USER_WALLET_ID,
                    types = OrderType.issueCardTypes,
                    statuses = setOf(OrderStatus.NEW, OrderStatus.PROCESSING),
                )
            } returns listOf(existing).right()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Resumed(existing))
            verifyNoOrderCreated()
            coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(USER_WALLET_ID, existing.id) }
        }

        @Test
        fun `GIVEN active issue order of another virtual type WHEN invoked THEN a new order is created`() = runTest {
            // Arrange
            val existing = order(id = "existing", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2, status = OrderStatus.NEW)
            val created = order(id = "new", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.NEW)
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns customerOffers(VIRTUAL_OFFER).right()
            coEvery {
                orderRepository.findOrders(userWalletId = USER_WALLET_ID, types = any(), statuses = any())
            } returns listOf(existing).right()
            coEvery { orderRepository.createOrder(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                created.right()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Created(created))
        }

        @Test
        fun `GIVEN backend returns insufficient balance WHEN invoked THEN Other with the cause`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns customerOffers(VIRTUAL_OFFER).right()
            coEvery {
                orderRepository.createOrder(
                    userWalletId = USER_WALLET_ID,
                    type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
                    specificationName = SPEC,
                    idempotencyKey = any(),
                    deeplinkValue = null,
                )
            } returns VisaApiError.CardIssueInsufficientBalance.left()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.leftOrNull())
                .isEqualTo(IssueAdditionalCardError.Other(VisaApiError.CardIssueInsufficientBalance))
        }

        @Test
        fun `GIVEN no active order AND createOrder succeeds WHEN invoked THEN Created without deeplink`() = runTest {
            // Arrange
            val created = order(id = "new", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.NEW)
            coEvery { offersRepository.getOffers(USER_WALLET_ID) } returns customerOffers(VIRTUAL_OFFER).right()
            coEvery {
                orderRepository.createOrder(
                    userWalletId = USER_WALLET_ID,
                    type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
                    specificationName = SPEC,
                    idempotencyKey = any(),
                    deeplinkValue = null,
                )
            } returns created.right()

            // Act
            val result = useCase(USER_WALLET_ID)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Created(created))
            coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(USER_WALLET_ID, created.id) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CampaignPath {

        @Test
        fun `GIVEN campaign offer WHEN invoked with deeplink THEN order created with deeplink spec and type`() =
            runTest {
                // Arrange
                val created = order(id = "new", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2, status = OrderStatus.NEW)
                coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns
                    customerOffers(VIRTUAL_OFFER, CAMPAIGN_OFFER).right()
                coEvery {
                    orderRepository.createOrder(
                        userWalletId = USER_WALLET_ID,
                        type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
                        specificationName = CAMPAIGN_SPEC,
                        idempotencyKey = any(),
                        deeplinkValue = DEEPLINK_VALUE,
                    )
                } returns created.right()

                // Act
                val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

                // Assert
                assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Created(created))
                coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(USER_WALLET_ID, created.id) }
            }

        @Test
        fun `GIVEN offers without campaign offer WHEN invoked with deeplink THEN NoCampaignOffer`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns customerOffers(VIRTUAL_OFFER).right()

            // Act
            val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(IssueAdditionalCardError.NoCampaignOffer)
            verifyNoOrderCreated()
        }

        @Test
        fun `GIVEN backend rejects the offer WHEN invoked with deeplink THEN OfferNotAvailable`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns customerOffers(CAMPAIGN_OFFER).right()
            coEvery { orderRepository.createOrder(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                VisaApiError.CardIssueOfferNotAvailable.left()

            // Act
            val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(IssueAdditionalCardError.OfferNotAvailable)
        }

        @Test
        fun `GIVEN offers request fails WHEN invoked with deeplink THEN Other with the cause`() = runTest {
            // Arrange
            coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns
                VisaApiError.ServerUnavailable.left()

            // Act
            val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(IssueAdditionalCardError.Other(VisaApiError.ServerUnavailable))
            verifyNoOrderCreated()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN active virtual order WHEN invoked with deeplink THEN Resumed without createOrder`(
            type: OrderType,
        ) = runTest {
            // Arrange
            val existing = order(id = "existing", type = type, status = OrderStatus.PROCESSING)
            coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns customerOffers(CAMPAIGN_OFFER).right()
            coEvery {
                orderRepository.findOrders(userWalletId = USER_WALLET_ID, types = any(), statuses = any())
            } returns listOf(existing).right()

            // Act
            val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Resumed(existing))
            verifyNoOrderCreated()
        }

        private fun provideTestModels() = OrderType.virtualIssueCardTypes.toList()

        @Test
        fun `GIVEN active plastic order WHEN invoked with deeplink THEN a campaign order is still created`() = runTest {
            // Arrange
            val plastic = order(id = "plastic", type = OrderType.CARD_ISSUE_PLASTIC_RAIN, status = OrderStatus.PROCESSING)
            val created = order(id = "new", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2, status = OrderStatus.NEW)
            coEvery { offersRepository.getOffers(USER_WALLET_ID, DEEPLINK_VALUE) } returns customerOffers(CAMPAIGN_OFFER).right()
            coEvery {
                orderRepository.findOrders(userWalletId = USER_WALLET_ID, types = any(), statuses = any())
            } returns listOf(plastic).right()
            coEvery { orderRepository.createOrder(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                created.right()

            // Act
            val result = useCase(USER_WALLET_ID, deeplinkValue = DEEPLINK_VALUE)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(IssueAdditionalCardResult.Created(created))
        }
    }

    private fun verifyNoOrderCreated() {
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private fun order(id: String, type: OrderType, status: OrderStatus): Order = Order(
        id = id,
        customerId = "customer",
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

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val SPEC = "SP_000004"
        const val CAMPAIGN_SPEC = "SP_000009"
        const val DEEPLINK_VALUE = "tpay_for_oxford"

        val VIRTUAL_OFFER = Offer(
            type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
            fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
            data = Offer.Data(specificationName = SPEC, orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
        )
        val CAMPAIGN_OFFER = Offer(
            type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
            fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
            data = Offer.Data(specificationName = CAMPAIGN_SPEC, orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2),
        )
    }
}

private fun customerOffers(vararg offers: Offer) =
    CustomerOffers(orderable = offers.toList(), artwork = emptyMap())