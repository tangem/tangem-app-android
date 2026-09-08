package com.tangem.features.tangempay.withdraw.hub.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.features.tangempay.withdraw.hub.TangemPayWithdrawHubComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayWithdrawHubModelTest {

    private val userWalletId = UserWalletId("123")
    private val token: CryptoCurrency.Token = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val currencyFactory: TangemPayCurrencyFactory = mockk()
    private val withdrawRepository: TangemPayWithdrawRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, currencyFactory, withdrawRepository, uiMessageSender)
        every { currencyFactory.create(userWalletId) } returns token
        coEvery { withdrawRepository.hasWithdrawOrder(userWalletId) } returns false
    }

    @Test
    fun `GIVEN model WHEN back clicked THEN router pops`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN within portfolio clicked THEN hub popped and withdraw swap pushed`() {
        // Arrange
        val model = createModel()
        val expectedRoute = AppRoute.Swap(
            userWalletId = userWalletId,
            fromCryptoCurrency = token,
            screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
            accountFlow = AccountFlow.Withdraw,
        )

        // Act
        model.uiState.value.swapSection.withinPortfolio.onClick()

        // Assert
        verifyOrder {
            router.pop()
            router.push(expectedRoute)
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN active withdrawal WHEN within portfolio clicked THEN in progress message shown and no navigation`() {
        // Arrange
        coEvery { withdrawRepository.hasWithdrawOrder(userWalletId) } returns true
        val model = createModel()

        // Act
        model.uiState.value.swapSection.withinPortfolio.onClick()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 0) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    private fun createModel(): TangemPayWithdrawHubModel = TangemPayWithdrawHubModel(
        paramsContainer = MutableParamsContainer(TangemPayWithdrawHubComponent.Params(userWalletId = userWalletId)),
        dispatchers = dispatchers,
        router = router,
        tangemPayCurrencyFactory = currencyFactory,
        tangemPayWithdrawRepository = withdrawRepository,
        uiMessageSender = uiMessageSender,
    )
}