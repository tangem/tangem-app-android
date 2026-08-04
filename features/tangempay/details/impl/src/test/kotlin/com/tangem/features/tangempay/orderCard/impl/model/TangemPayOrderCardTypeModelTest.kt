package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CardDeliveryContext
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CardDeliveryQuoteRepository
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardTypeComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderCardType
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.util.Locale

internal class TangemPayOrderCardTypeModelTest {

    private val userWalletId = UserWalletId("123")
    private val usd = Currency.getInstance("USD")
    private val originalLocale = Locale.getDefault()

    private val router: Router = mockk(relaxed = true)
    private val customerOffersRepository: CustomerOffersRepository = mockk()
    private val cardDeliveryQuoteRepository: CardDeliveryQuoteRepository = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val featureToggles: TangemPayFeatureToggles = mockk()

    private val status: AccountStatus.Payment = mockk {
        every { value } returns PaymentAccountStatusValue.Loading
    }

    private var model: TangemPayOrderCardTypeModel? = null

    @BeforeEach
    fun setUp() {
        Locale.setDefault(Locale.US)
        every { featureToggles.isPlasticCardOrderEnabled } returns true
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(status)
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            offers(Offer.Type.CARD_ISSUE_VIRTUAL_RAIN, Offer.Type.TANGEM_PAY_PLASTIC_VISA).right()
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns quote().right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN fee positive and sufficient balance WHEN model created THEN plastic default state`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isError).isFalse()
        assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
        assertThat(state.plastic?.feeState).isEqualTo(TangemPayOrderCardTypeUM.FeeState.Default)
        assertThat(state.plastic?.country).isEqualTo("US")
        assertThat(state.plastic?.deliveryEtaMaxBusinessDays).isEqualTo(20)
        assertThat(state.plastic?.deliveryFee).contains("$")
        assertThat(state.plastic?.deliveryFee).contains("10")
    }

    @Test
    fun `GIVEN delivery fee waived WHEN model created THEN free delivery state`() = runTest {
        // Arrange
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns quote(isWaived = true).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.plastic?.feeState).isEqualTo(TangemPayOrderCardTypeUM.FeeState.FreeDelivery)
    }

    @Test
    fun `GIVEN fee positive and insufficient balance WHEN model created THEN insufficient funds state`() = runTest {
        // Arrange
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns quote(hasSufficientBalance = false).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.plastic?.feeState).isEqualTo(TangemPayOrderCardTypeUM.FeeState.InsufficientFunds)
    }

    @Test
    fun `GIVEN fee waived and insufficient balance WHEN model created THEN free delivery wins`() = runTest {
        // Arrange
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns quote(isWaived = true, hasSufficientBalance = false).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.plastic?.feeState).isEqualTo(TangemPayOrderCardTypeUM.FeeState.FreeDelivery)
    }

    @Test
    fun `GIVEN zero fee and insufficient balance WHEN model created THEN default state`() = runTest {
        // Arrange
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns quote(feeAmount = BigDecimal.ZERO, hasSufficientBalance = false).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.plastic?.feeState).isEqualTo(TangemPayOrderCardTypeUM.FeeState.Default)
    }

    @Test
    fun `GIVEN plastic toggle disabled WHEN model created THEN virtual only and no quote requested`() = runTest {
        // Arrange
        every { featureToggles.isPlasticCardOrderEnabled } returns false

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availableTypes).containsExactly(OrderCardType.Virtual)
        assertThat(model.state.value.plastic).isNull()
        coVerify(exactly = 0) { cardDeliveryQuoteRepository.getCardDeliveryQuote(any(), any()) }
    }

    @Test
    fun `GIVEN no plastic offer WHEN model created THEN virtual only and no quote requested`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            offers(Offer.Type.CARD_ISSUE_VIRTUAL_RAIN).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.isError).isFalse()
        assertThat(model.state.value.availableTypes).containsExactly(OrderCardType.Virtual)
        assertThat(model.state.value.plastic).isNull()
        coVerify(exactly = 0) { cardDeliveryQuoteRepository.getCardDeliveryQuote(any(), any()) }
    }

    @Test
    fun `GIVEN virtual offer present WHEN model created THEN virtual issue fee resolved`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.virtual.issueFee).contains("$")
        assertThat(model.state.value.virtual.issueFee).contains("5")
    }

    @Test
    fun `GIVEN offers request fails WHEN model created THEN error state`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.isError).isTrue()
        assertThat(model.state.value.isLoading).isFalse()
    }

    @Test
    fun `GIVEN plastic offer present but quote fails WHEN model created THEN error state`() = runTest {
        // Arrange
        coEvery {
            cardDeliveryQuoteRepository.getCardDeliveryQuote(userWalletId, CardDeliveryContext.ISSUE)
        } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.isError).isTrue()
    }

    private fun createModel(testScope: TestScope) = TangemPayOrderCardTypeModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardTypeComponent.Params(
                userWalletId = userWalletId,
                onSelectVirtual = {},
                onSelectPlastic = {},
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getCustomerOffers = GetCustomerOffersUseCase(customerOffersRepository),
        cardDeliveryQuoteRepository = cardDeliveryQuoteRepository,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        featureToggles = featureToggles,
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

    private fun offers(vararg types: Offer.Type): List<Offer> = types.map { type ->
        Offer(
            type = type,
            fee = Offer.Fee(amount = BigDecimal("5.00"), currency = usd),
            data = Offer.Data(specificationName = "spec", orderType = OrderType.UNKNOWN),
        )
    }

    private fun quote(
        isWaived: Boolean = false,
        hasSufficientBalance: Boolean = true,
        feeAmount: BigDecimal = BigDecimal("10.00"),
    ): CardDeliveryQuote = CardDeliveryQuote(
        country = "US",
        isPlasticAvailable = true,
        isDeliveryFeeWaived = isWaived,
        deliveryFee = CardDeliveryQuote.DeliveryFee(amount = feeAmount, currency = usd),
        deliveryEta = CardDeliveryQuote.DeliveryEta(minBusinessDays = 1, maxBusinessDays = 20),
        hasSufficientBalance = hasSufficientBalance,
    )
}