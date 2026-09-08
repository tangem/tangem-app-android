package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayOrderCardModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getCustomerOffers: GetCustomerOffersUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val onAddFundsRequested: () -> Unit = mockk(relaxed = true)

    private val status: AccountStatus.Payment = mockk {
        every { value } returns PaymentAccountStatusValue.Loading
    }

    private var model: TangemPayOrderCardModel? = null

    @BeforeEach
    fun setUp() {
        every { paymentAccountStatusSupplier(WALLET_ID) } returns flowOf(status)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN virtual offer WHEN onSelectVirtual THEN additional-card offer requested`() = runTest {
        // Arrange
        coEvery { getCustomerOffers.additionalCardOffer(WALLET_ID) } returns virtualOffer().right()

        // Act
        val model = createModel(testScope = this)
        model.onSelectVirtual()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getCustomerOffers.additionalCardOffer(WALLET_ID) }
    }

    @Test
    fun `GIVEN issue succeeded WHEN callback THEN status refreshed and flow closed`() = runTest {
        // Act
        val model = createModel(testScope = this)
        model.onIssueAdditionalCardSucceeded()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(WALLET_ID) }
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN issue popup shown WHEN onAddFundsForCardIssue THEN popup dismissed and add funds requested`() =
        runTest {
            // Arrange
            coEvery { getCustomerOffers.additionalCardOffer(WALLET_ID) } returns virtualOffer().right()
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            model.onSelectVirtual()
            advanceUntilIdle()

            // Act
            model.onAddFundsForCardIssue()

            // Assert
            assertThat(openedSheets.filterIsInstance<TangemPayOrderCardNavigation.IssueVirtual>()).hasSize(1)
            assertThat(openedSheets.last()).isNull()
            verifyOrder {
                onAddFundsRequested()
                router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails)
            }
            verify(exactly = 1) { onAddFundsRequested() }
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN add funds already requested WHEN onAddFundsForCardIssue again THEN request is not repeated`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.onAddFundsForCardIssue()
        model.onAddFundsForCardIssue()

        // Assert
        verify(exactly = 1) { onAddFundsRequested() }
        verify(exactly = 1) { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails) }
    }

    @Test
    fun `GIVEN a successful order WHEN onShowOrderedCard THEN status refreshed and account screen shown`() = runTest {
        // Act
        val model = createModel(testScope = this)
        model.onShowOrderedCard()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(WALLET_ID) }
        verify(exactly = 1) { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN a refresh already running WHEN onShowOrderedCard again THEN status fetched and popped once`() =
        runTest {
            // Act
            val model = createModel(testScope = this)
            model.onShowOrderedCard()
            model.onShowOrderedCard()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(WALLET_ID) }
            verify(exactly = 1) { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails) }
        }

    private fun SlotNavigation<TangemPayOrderCardNavigation>.trackSlot(): List<TangemPayOrderCardNavigation?> {
        val tracked = mutableListOf<TangemPayOrderCardNavigation?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private fun createModel(testScope: TestScope) = TangemPayOrderCardModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardComponent.Params(
                userWalletId = WALLET_ID,
                onAddFundsRequested = onAddFundsRequested,
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getCustomerOffers = getCustomerOffers,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    ).also { model = it }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun virtualOffer(): Offer = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "spec", orderType = OrderType.UNKNOWN),
    )

    private companion object {
        val WALLET_ID = UserWalletId("1234567890ABCDEF")
    }
}