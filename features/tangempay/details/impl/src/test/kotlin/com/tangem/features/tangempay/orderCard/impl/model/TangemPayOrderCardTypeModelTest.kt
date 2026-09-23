package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardTypeComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderCardType
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM.FeeState
import com.tangem.features.tangempay.orderCard.impl.ui.state.imageUrlFor
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.CountryNames
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

private const val PLASTIC_IMAGE_URL = "https://images.us.paera.com/Physical-main.png"
private const val EXPECTED_MAX_BUSINESS_DAYS = 4

internal class TangemPayOrderCardTypeModelTest {

    private val userWalletId = UserWalletId("123")
    private val usd = Currency.getInstance("USD")
    private val originalLocale = Locale.getDefault()

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
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
        clearMocks(analytics)
        Locale.setDefault(Locale.US)
        selectedPlasticEta = null
        every { featureToggles.isPlasticCardOrderEnabled } returns true
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(status)
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            customerOffers(virtualOffer(), plasticOffer()).right()
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
            assertThat(plastic?.country).isEqualTo(CountryNames.getDisplayName("US"))
            assertThat(plastic?.deliveryEta)
                .isEqualTo(TangemPayOrderCardTypeUM.DeliveryEta(minBusinessDays = 2, maxBusinessDays = 4))
            assertThat(plastic?.deliveryFee).contains("$")
            assertThat(plastic?.deliveryFee).contains("21.69")
        }

    @Test
    fun `GIVEN whole delivery fee WHEN model created THEN fee shown without fractional digits`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            customerOffers(virtualOffer(), plasticOffer(feeAmount = BigDecimal("10.00"))).right()

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
            customerOffers(
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
            customerOffers(virtualOffer(), plasticOffer(feeAmount = BigDecimal.ZERO)).right()

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
            customerOffers(virtualOffer(), plasticOffer(feeAmount = BigDecimal.ZERO)).right()
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
            coEvery { customerOffersRepository.getOffers(userWalletId) } returns customerOffers(virtualOffer()).right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.isError).isFalse()
            assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
            assertThat(state.plastic)
                .isEqualTo(TangemPayOrderCardTypeUM.Plastic.Unavailable(country = CountryNames.getDisplayName("US")))
        }

    @Test
    fun `GIVEN artwork of a skipped plastic offer WHEN model created THEN the plastic page shows it`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns CustomerOffers(
            orderable = listOf(virtualOffer()),
            artwork = mapOf(Offer.Type.CARD_ISSUE_PLASTIC_RAIN to PLASTIC_IMAGE_URL),
        ).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.plastic.offerImageUrl).isEqualTo(PLASTIC_IMAGE_URL)
        assertThat(state.imageUrlFor(OrderCardType.Plastic)).isEqualTo(PLASTIC_IMAGE_URL)
    }

    @Test
    fun `GIVEN plastic offer without delivery eta WHEN model created THEN plastic is unavailable`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns
            customerOffers(virtualOffer(), plasticOffer(deliveryEta = null)).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.isError).isFalse()
        assertThat(state.availableTypes).containsExactly(OrderCardType.Virtual, OrderCardType.Plastic).inOrder()
        assertThat(state.plastic)
            .isEqualTo(TangemPayOrderCardTypeUM.Plastic.Unavailable(country = CountryNames.getDisplayName("US")))
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
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns customerOffers(virtualOffer()).right()
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

    @Test
    fun `GIVEN the order type screen WHEN the model is created THEN the screen opened event is sent`() = runTest {
        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardTypeSelectionScreenOpened>())
        }
    }

    @Test
    fun `GIVEN insufficient balance WHEN the plastic page was never shown THEN no not-enough-money event`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal("1.00")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat((model.state.value.plastic as TangemPayOrderCardTypeUM.Plastic.Available).feeState)
            .isEqualTo(FeeState.InsufficientFunds)
        verify(exactly = 0) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.DeliveryCostNotEnoughMoneyShowed>())
        }
    }

    @Test
    fun `GIVEN insufficient balance WHEN the plastic page is shown twice THEN one not-enough-money event`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal("1.00")).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onTypeClick(OrderCardType.Plastic)
        model.state.value.onTypeSwipe(OrderCardType.Virtual)
        model.state.value.onTypeSwipe(OrderCardType.Plastic)

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.DeliveryCostNotEnoughMoneyShowed>())
        }
    }

    @Test
    fun `GIVEN the plastic page shown before loading WHEN insufficient balance loads THEN the event is sent`() =
        runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(availableBalance = BigDecimal("1.00")).right()
            val model = createModel(testScope = this)

            // Act
            model.state.value.onTypeSwipe(OrderCardType.Plastic)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.DeliveryCostNotEnoughMoneyShowed>())
            }
        }

    @Test
    fun `GIVEN sufficient balance WHEN the plastic page is shown THEN no not-enough-money event`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onTypeClick(OrderCardType.Plastic)

        // Assert
        verify(exactly = 0) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.DeliveryCostNotEnoughMoneyShowed>())
        }
    }

    @Test
    fun `GIVEN no virtual offer WHEN loaded THEN the virtual tab is hidden`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns customerOffers(plasticOffer()).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.availableTypes).containsExactly(OrderCardType.Plastic)
    }

    @Test
    fun `GIVEN an empty offer list WHEN loaded THEN it is not an error and plastic is unavailable`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns customerOffers().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.isError).isFalse()
        assertThat(state.availableTypes).containsExactly(OrderCardType.Plastic)
        assertThat(state.plastic).isInstanceOf(TangemPayOrderCardTypeUM.Plastic.Unavailable::class.java)
        assertThat(state.virtual.issueFee).isEmpty()
    }

    @Test
    fun `GIVEN no virtual offer and insufficient balance WHEN loaded THEN not-enough-money event is sent`() = runTest {
        // Arrange
        coEvery { customerOffersRepository.getOffers(userWalletId) } returns customerOffers(plasticOffer()).right()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(availableBalance = BigDecimal("1.00")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.DeliveryCostNotEnoughMoneyShowed>())
        }
    }

    @Test
    fun `GIVEN the type pills WHEN each is tapped THEN the matching type clicked event is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onTypeClick(OrderCardType.Virtual)
        model.state.value.onTypeClick(OrderCardType.Plastic)

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.VirtualTypeClicked>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.PlasticTypeClicked>()) }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardTypeSwiped>()) }
    }

    @Test
    fun `GIVEN a swipe between types WHEN reported THEN the swiped event is sent without a type clicked event`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onTypeSwipe(OrderCardType.Plastic)

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardTypeSwiped>()) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.PlasticTypeClicked>()) }
        }

    @Test
    fun `GIVEN the select button WHEN tapped on each page THEN the matching select event is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onSelectVirtual()
        model.state.value.onSelectPlastic()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.VirtualSelectClicked>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.PlasticSelectClicked>()) }
        assertThat(selectedPlasticEta).isEqualTo(EXPECTED_MAX_BUSINESS_DAYS)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PlanBackground {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a customer tariff plan WHEN the payment account emits THEN the plan background matches it`(
            testModel: PlanBackgroundModel,
        ) = runTest {
            // Arrange
            every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(loadedStatus(testModel.planState))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.isBasicPlan).isEqualTo(testModel.isBasicPlan)
        }

        @Test
        fun `GIVEN a plus plan WHEN the payment account stops reporting a plan THEN the plus background is kept`() =
            runTest {
                // Arrange
                val statuses = flowOf(loadedStatus(planState(isBasicTier = false)), loadedStatus(planState = null))
                every { paymentAccountStatusSupplier(userWalletId) } returns statuses

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.state.value.isBasicPlan).isFalse()
            }

        private fun provideTestModels() = listOf(
            PlanBackgroundModel(planState = planState(isBasicTier = true), isBasicPlan = true),
            PlanBackgroundModel(planState = planState(isBasicTier = false), isBasicPlan = false),
            PlanBackgroundModel(planState = null, isBasicPlan = true),
        )
    }

    private fun planState(isBasicTier: Boolean) = tariffPlanState(
        tariff = customerTariffPlan(
            plan = tariffPlan(tierId = if (isBasicTier) "BASIC" else "PLUS", isBasicTier = isBasicTier),
        ),
    )

    private fun loadedStatus(planState: TangemPayTariffPlanState?): AccountStatus.Payment {
        val loaded = mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
            every { tariffPlan } returns planState
        }
        return mockk { every { value } returns loaded }
    }

    private fun createModel(testScope: TestScope) = TangemPayOrderCardTypeModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardTypeComponent.Params(
                userWalletId = userWalletId,
                onSelectVirtual = {},
                onSelectPlastic = { selectedPlasticEta = it },
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
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

    @Test
    fun `GIVEN the plastic offer carries a MAIN image WHEN loaded THEN the plastic page uses it`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val plastic = model.state.value.plastic as TangemPayOrderCardTypeUM.Plastic.Available
        assertThat(plastic.offerImageUrl).isEqualTo(PLASTIC_IMAGE_URL)
        assertThat(model.state.value.imageUrlFor(OrderCardType.Plastic)).isEqualTo(PLASTIC_IMAGE_URL)
    }

    @Test
    fun `GIVEN the plastic offer has no image WHEN loaded THEN the plastic page falls back to the card image`() =
        runTest {
            // Arrange
            coEvery { customerOffersRepository.getOffers(userWalletId) } returns
                customerOffers(virtualOffer(), plasticOffer(mainImageUrl = null)).right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val plastic = model.state.value.plastic as TangemPayOrderCardTypeUM.Plastic.Available
            assertThat(plastic.offerImageUrl).isNull()
            assertThat(model.state.value.imageUrlFor(OrderCardType.Plastic))
                .isEqualTo(model.state.value.cardImageUrl)
        }

    @Test
    fun `GIVEN the virtual offer has no image WHEN loaded THEN the virtual page falls back to the card image`() =
        runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.virtual.offerImageUrl).isNull()
            assertThat(model.state.value.imageUrlFor(OrderCardType.Virtual))
                .isEqualTo(model.state.value.cardImageUrl)
        }

    private fun plasticOffer(
        feeAmount: BigDecimal = BigDecimal("21.69"),
        deliveryEta: Offer.DeliveryEta? = Offer.DeliveryEta(minBusinessDays = 2, maxBusinessDays = 4),
        mainImageUrl: String? = PLASTIC_IMAGE_URL,
    ) = Offer(
        type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
        fee = Offer.Fee(amount = feeAmount, currency = usd),
        data = Offer.Data(
            specificationName = "SP_000008",
            orderType = OrderType.CARD_ISSUE_PLASTIC_RAIN,
            deliveryEta = deliveryEta,
        ),
        mainImageUrl = mainImageUrl,
    )

    internal data class PlanBackgroundModel(
        val planState: TangemPayTariffPlanState?,
        val isBasicPlan: Boolean,
    ) {
        override fun toString(): String = "plan=${planState?.tariff?.plan?.tierId ?: "none"}"
    }

    private fun customerInfo(availableBalance: BigDecimal? = BigDecimal("100.00")) = CustomerInfo(
        customerId = "cust_1",
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = availableBalance?.let {
            PaymentAccountStatusValue.FiatBalance(availableBalance = it, currency = "USD")
        },
        tariffPlan = null,
        country = "US",
        email = "j.silverhand@gmail.com",
    )
}

private fun customerOffers(vararg offers: Offer) =
    CustomerOffers(orderable = offers.toList(), artwork = emptyMap())