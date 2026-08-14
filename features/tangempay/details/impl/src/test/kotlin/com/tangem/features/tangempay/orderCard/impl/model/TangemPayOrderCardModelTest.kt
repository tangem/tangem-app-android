package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.right
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
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun `GIVEN add funds requested WHEN callback THEN flow closed`() = runTest {
        // Act
        val model = createModel(testScope = this)
        model.onAddFundsForCardIssue()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    private fun createModel(testScope: TestScope) = TangemPayOrderCardModel(
        paramsContainer = MutableParamsContainer(TangemPayOrderCardComponent.Params(userWalletId = WALLET_ID)),
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