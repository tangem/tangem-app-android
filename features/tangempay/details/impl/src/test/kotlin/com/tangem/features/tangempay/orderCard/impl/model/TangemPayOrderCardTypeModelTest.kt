package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardTypeComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderCardType
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM.FeeState
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
    private val onboardingRepository: OnboardingRepository = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val featureToggles: TangemPayFeatureToggles = mockk()

    private val status: AccountStatus.Payment = mockk {
        every { value } returns PaymentAccountStatusValue.Loading
    }

    private var model: TangemPayOrderCardTypeModel? = null
    private var selectedPlasticEta: Int? = null

    @BeforeEach
    fun setUp() {
        Locale.setDefault(Locale.US)
        selectedPlasticEta = null
        every { featureToggles.isPlasticCardOrderEnabled } returns true
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(status)
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(virtualOffer(), plasticOffer()).right()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN plastic offer and sufficient balance WHEN model created THEN fee and eta come from the offer`() =
        runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.isError).isFalse()
            assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
            val plastic = state.availablePlastic
            assertThat(plastic?.feeState).isEqualTo(FeeState.Default)
            assertThat(plastic?.country).isEqualTo("US")
            assertThat(plastic?.deliveryEta)
                .isEqualTo(TangemPayOrderCardTypeUM.DeliveryEta(minBusinessDays = 2, maxBusinessDays = 4))
            assertThat(plastic?.deliveryFee).contains("$")
            assertThat(plastic?.deliveryFee).contains("21.69")
        }

    @Test
    fun `GIVEN whole delivery fee WHEN model created THEN fee shown without fractional digits`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(virtualOffer(), plasticOffer(feeAmount = BigDecimal("10.00"))).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.deliveryFee).isEqualTo("$10")
    }

    @Test
    fun `GIVEN plastic offer without min eta WHEN model created THEN only max business days carried`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(
                virtualOffer(),
                plasticOffer(deliveryEta = Offer.DeliveryEta(minBusinessDays = null, maxBusinessDays = 4)),
            ).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.deliveryEta)
            .isEqualTo(TangemPayOrderCardTypeUM.DeliveryEta(minBusinessDays = null, maxBusinessDays = 4))
    }

    @Test
    fun `GIVEN zero delivery fee WHEN model created THEN free delivery state`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(virtualOffer(), plasticOffer(feeAmount = BigDecimal.ZERO)).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val plastic = model.state.value.availablePlastic
        assertThat(plastic?.feeState).isEqualTo(FeeState.FreeDelivery)
        assertThat(plastic?.deliveryFee).isNull()
    }

    @Test
    fun `GIVEN fee above the customer balance WHEN model created THEN insufficient funds state`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal("21.68")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.feeState).isEqualTo(FeeState.InsufficientFunds)
    }

    @Test
    fun `GIVEN fee equal to the customer balance WHEN model created THEN default state`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal("21.69")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.feeState).isEqualTo(FeeState.Default)
    }

    @Test
    fun `GIVEN zero fee and empty balance WHEN model created THEN free delivery wins`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(virtualOffer(), plasticOffer(feeAmount = BigDecimal.ZERO)).right()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal.ZERO).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.feeState).isEqualTo(FeeState.FreeDelivery)
    }

    @Test
    fun `GIVEN no fiat balance WHEN model created THEN insufficient funds state`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = null).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availablePlastic?.feeState).isEqualTo(FeeState.InsufficientFunds)
    }

    @Test
    fun `GIVEN plastic toggle disabled WHEN model created THEN virtual only and no customer info requested`() =
        runTest {
            // Arrange
            every { featureToggles.isPlasticCardOrderEnabled } returns false

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.availableTypes).containsExactly(OrderCardType.Virtual)
            coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
        }

    @Test
    fun `GIVEN no plastic offer WHEN model created THEN plastic is unavailable with the residence country`() =
        runTest {
            // Arrange
            coEvery { customerOffersRepository.getOffers(userWalletId) } returns listOf(virtualOffer()).right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.isError).isFalse()
            assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
            assertThat(state.plastic).isEqualTo(TangemPayOrderCardTypeUM.Plastic.Unavailable(country = "US"))
        }

    @Test
    fun `GIVEN plastic offer without delivery eta WHEN model created THEN plastic is unavailable`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            listOf(virtualOffer(), plasticOffer(deliveryEta = null)).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.isError).isFalse()
        assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
        assertThat(state.plastic).isEqualTo(TangemPayOrderCardTypeUM.Plastic.Unavailable(country = "US"))
    }

    @Test
    fun `GIVEN available plastic WHEN select plastic THEN max business days passed on`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onSelectPlastic()

        // Assert
        assertThat(selectedPlasticEta).isEqualTo(4)
    }

    @Test
    fun `GIVEN unavailable plastic WHEN select plastic THEN selection ignored`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns listOf(virtualOffer()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onSelectPlastic()

        // Assert
        assertThat(selectedPlasticEta).isNull()
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
    fun `GIVEN plastic offer present but customer info fails WHEN model created THEN error state`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.isError).isTrue()
    }

    private val TangemPayOrderCardTypeUM.availablePlastic: TangemPayOrderCardTypeUM.Plastic.Available?
        get() = plastic as? TangemPayOrderCardTypeUM.Plastic.Available

    private fun createModel(testScope: TestScope) = TangemPayOrderCardTypeModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardTypeComponent.Params(
                userWalletId = userWalletId,
                onSelectVirtual = {},
                onSelectPlastic = { selectedPlasticEta = it },
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getCustomerOffers = GetCustomerOffersUseCase(customerOffersRepository),
        onboardingRepository = onboardingRepository,
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

    private fun virtualOffer() = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = usd),
        data = Offer.Data(
            specificationName = "SP_000004",
            orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        ),
    )

    private fun plasticOffer(
        feeAmount: BigDecimal = BigDecimal("21.69"),
        deliveryEta: Offer.DeliveryEta? = Offer.DeliveryEta(minBusinessDays = 2, maxBusinessDays = 4),
    ) = Offer(
        type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
        fee = Offer.Fee(amount = feeAmount, currency = usd),
        data = Offer.Data(
            specificationName = "SP_000008",
            orderType = OrderType.CARD_ISSUE_PLASTIC_RAIN,
            deliveryEta = deliveryEta,
        ),
    )

    private fun customerInfo(availableBalance: BigDecimal? = BigDecimal("100.00")) = CustomerInfo(
        customerId = "cust_1",
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = availableBalance?.let {
            PaymentAccountStatusValue.FiatBalance(availableBalance = it, currency = "USD")
        },
        cryptoBalance = null,
        availableForWithdrawal = BigDecimal.ZERO,
        tariffPlan = null,
        country = "US",
        email = "j.silverhand@gmail.com",
    )
}