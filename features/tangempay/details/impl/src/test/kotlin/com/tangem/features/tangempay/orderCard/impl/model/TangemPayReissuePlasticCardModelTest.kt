package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.orderCard.impl.TangemPayReissuePlasticCardComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayReissuePlasticCardUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

private const val COUNTRY = "Afghanistan"
private const val SOURCE_PRODUCT_INSTANCE_ID = "pi_source_0001"
private const val DELIVERY_ETA_MAX_DAYS = 20

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayReissuePlasticCardModelTest {

    private val userWalletId = UserWalletId("123")

    private val getCustomerOffers: GetCustomerOffersUseCase = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val reissueCardRepository: TangemPayReissueCardRepository = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private var isDismissed: Boolean = false
    private var confirmedEta: Int? = null
    private var model: TangemPayReissuePlasticCardModel? = null

    @BeforeEach
    fun setUp() {
        isDismissed = false
        confirmedEta = null
        clearMocks(getCustomerOffers, onboardingRepository, reissueCardRepository)
        coEvery { getCustomerOffers(userWalletId) } returns listOf(plasticOffer()).right()
        coEvery { reissueCardRepository.getPlasticReissueCardFee(userWalletId) } returns fee().right()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN offer fee and customer info load WHEN model created THEN content shows country fee and eta`() =
        runTest {
            // Act
            val state = createLoadedModel().state.value

            // Assert
            assertThat(state).isEqualTo(
                TangemPayReissuePlasticCardUM.Content(
                    onDismissRequest = (state as TangemPayReissuePlasticCardUM.Content).onDismissRequest,
                    country = COUNTRY,
                    deliveryFee = "$10.00",
                    deliveryEtaMaxBusinessDays = DELIVERY_ETA_MAX_DAYS,
                    isInsufficientFunds = false,
                    onReplaceClick = state.onReplaceClick,
                ),
            )
        }

    @Test
    fun `GIVEN the balance covers the fee WHEN model created THEN replace is enabled`() = runTest {
        // Act
        val state = createLoadedModel().state.value

        // Assert
        assertThat((state as TangemPayReissuePlasticCardUM.Content).isReplaceEnabled).isTrue()
    }

    @Test
    fun `GIVEN the balance is below the fee WHEN model created THEN replace is blocked as not enough money`() =
        runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(availableBalance = BigDecimal("9.99")).right()

            // Act
            val state = createLoadedModel().state.value

            // Assert
            val content = state as TangemPayReissuePlasticCardUM.Content
            assertThat(content.isInsufficientFunds).isTrue()
            assertThat(content.isReplaceEnabled).isFalse()
        }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN incomplete data WHEN model created THEN error state`(model: LoadErrorModel) = runTest {
        // Arrange
        model.arrange()

        // Act
        val state = createLoadedModel().state.value

        // Assert
        assertThat(state).isInstanceOf(TangemPayReissuePlasticCardUM.Error::class.java)
    }

    @Test
    fun `GIVEN an error state WHEN retry succeeds THEN content is shown`() = runTest {
        // Arrange
        coEvery { reissueCardRepository.getPlasticReissueCardFee(userWalletId) } returns
            VisaApiError.ServerUnavailable.left()
        val model = createLoadedModel()
        val error = model.state.value as TangemPayReissuePlasticCardUM.Error
        coEvery { reissueCardRepository.getPlasticReissueCardFee(userWalletId) } returns fee().right()

        // Act
        error.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value).isInstanceOf(TangemPayReissuePlasticCardUM.Content::class.java)
    }

    @Test
    fun `GIVEN content WHEN replace clicked THEN the eta is handed to the order flow and no order is created`() =
        runTest {
            // Arrange
            val model = createLoadedModel()
            val content = model.state.value as TangemPayReissuePlasticCardUM.Content

            // Act
            content.onReplaceClick()

            // Assert
            assertThat(confirmedEta).isEqualTo(DELIVERY_ETA_MAX_DAYS)
            assertThat(isDismissed).isFalse()
        }

    @Test
    fun `GIVEN content WHEN dismissed THEN the flow is closed without confirming`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.onDismiss()

        // Assert
        assertThat(isDismissed).isTrue()
        assertThat(confirmedEta).isNull()
    }

    private fun TestScope.createLoadedModel(): TangemPayReissuePlasticCardModel =
        createModel(testScope = this).also { advanceUntilIdle() }

    private fun createModel(testScope: TestScope) = TangemPayReissuePlasticCardModel(
        paramsContainer = MutableParamsContainer(
            TangemPayReissuePlasticCardComponent.Params(
                userWalletId = userWalletId,
                sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                onDismiss = { isDismissed = true },
                onReplaceConfirmed = { confirmedEta = it },
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        getCustomerOffers = getCustomerOffers,
        onboardingRepository = onboardingRepository,
        reissueCardRepository = reissueCardRepository,
        analytics = analytics,
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

    private fun plasticOffer(deliveryEtaMaxDays: Int? = DELIVERY_ETA_MAX_DAYS) = Offer(
        type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
        fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
        data = Offer.Data(
            specificationName = "SP_000010",
            orderType = OrderType.CARD_ISSUE_PLASTIC_RAIN,
            deliveryEta = deliveryEtaMaxDays?.let {
                Offer.DeliveryEta(minBusinessDays = null, maxBusinessDays = it)
            },
        ),
    )

    private fun fee() = TangemPayReissueCardFee(amount = BigDecimal("10.00"), currencyCode = "USD")

    private fun customerInfo(
        country: String? = COUNTRY,
        availableBalance: BigDecimal = BigDecimal("100.00"),
    ) = CustomerInfo(
        customerId = "c1",
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = PaymentAccountStatusValue.FiatBalance(
            availableBalance = availableBalance,
            currency = "USD",
        ),
        cryptoBalance = null,
        availableForWithdrawal = BigDecimal.ZERO,
        tariffPlan = null,
        country = country,
        phoneMask = null,
        email = "j.silverhand@gmail.com",
    )

    internal data class LoadErrorModel(val name: String, val arrange: () -> Unit) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        LoadErrorModel(name = "fee request fails") {
            coEvery { reissueCardRepository.getPlasticReissueCardFee(userWalletId) } returns
                VisaApiError.ServerUnavailable.left()
        },
        LoadErrorModel(name = "offers request fails") {
            coEvery { getCustomerOffers(userWalletId) } returns VisaApiError.ServerUnavailable.left()
        },
        LoadErrorModel(name = "no plastic offer") {
            coEvery { getCustomerOffers(userWalletId) } returns emptyList<Offer>().right()
        },
        LoadErrorModel(name = "plastic offer without delivery eta") {
            coEvery { getCustomerOffers(userWalletId) } returns
                listOf(plasticOffer(deliveryEtaMaxDays = null)).right()
        },
        LoadErrorModel(name = "customer info request fails") {
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                VisaApiError.ServerUnavailable.left()
        },
        LoadErrorModel(name = "blank kyc country") {
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(country = "  ").right()
        },
    )
}