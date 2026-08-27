package com.tangem.features.tangempay.orderCard.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardDataComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Error
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

private const val COUNTRY = "US"
private const val EMAIL = "j.silverhand@gmail.com"
private const val PHONE_MASK = "+1 (###) ###-####"
private const val SLOW_LOAD_MS = 1_000L

internal class TangemPayOrderCardDataModelTest {

    private val userWalletId = UserWalletId("123")

    private val router: Router = mockk(relaxed = true)
    private val onboardingRepository: OnboardingRepository = mockk()

    private var submitted: PlasticCardOrder? = null
    private var isClosed: Boolean = false
    private var model: TangemPayOrderCardDataModel? = null

    @BeforeEach
    fun setUp() {
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN customer info loaded WHEN model created THEN form shown with country email and phone mask`() = runTest {
        // Act
        val model = createLoadedModel()

        // Assert
        assertThat(model.state.value).isInstanceOf(Form::class.java)
        assertThat(model.form.country).isEqualTo(COUNTRY)
        assertThat(model.form.email).isEqualTo(EMAIL)
        assertThat(model.form.phoneMask).isEqualTo(PHONE_MASK)
    }

    @ParameterizedTest
    @MethodSource("loadErrorCases")
    fun `GIVEN unusable customer info WHEN model created THEN error state`(case: LoadErrorModel) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            if (case.requestFails) {
                VisaApiError.Unspecified.left()
            } else {
                customerInfo(country = case.country, phoneMask = case.phoneMask, email = case.email).right()
            }

        // Act
        val model = createLoadedModel()

        // Assert
        assertThat(model.state.value).isInstanceOf(Error::class.java)
    }

    @ParameterizedTest
    @MethodSource("masklessCases")
    fun `GIVEN no usable phone mask WHEN model created THEN form shown with free-form phone`(
        phoneMask: String?,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(phoneMask = phoneMask).right()

        // Act
        val model = createLoadedModel()

        // Assert
        assertThat(model.state.value).isInstanceOf(Form::class.java)
        assertThat(model.form.phoneMask).isEmpty()
    }

    @Test
    fun `GIVEN no phone mask WHEN a full international number entered THEN submitted as E164`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo(phoneMask = null).right()
        val model = createLoadedModel()

        // Act
        model.fillValidForm(phoneDigits = "380501234567")
        model.form.onOrderClick()

        // Assert
        assertThat(model.form.isOrderEnabled).isTrue()
        assertThat(submitted?.shippingAddress?.phone).isEqualTo("+380501234567")
    }

    @Test
    fun `GIVEN no phone mask WHEN too few digits entered THEN order stays disabled`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo(phoneMask = null).right()
        val model = createLoadedModel()

        // Act
        model.fillValidForm(phoneDigits = "12345")

        // Assert
        assertThat(model.form.isOrderEnabled).isFalse()
    }

    @Test
    fun `GIVEN empty form WHEN model created THEN order disabled`() = runTest {
        // Act
        val model = createLoadedModel()

        // Assert
        assertThat(model.form.isOrderEnabled).isFalse()
    }

    @Test
    fun `GIVEN all fields valid WHEN order clicked THEN shipping address submitted with E164 phone`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm()
        assertThat(model.form.isOrderEnabled).isTrue()

        // Act
        model.form.onOrderClick()

        // Assert
        assertThat(submitted).isEqualTo(
            PlasticCardOrder(
                embossName = "JOHNNY SILVERHAND",
                shippingAddress = ShippingAddress(
                    firstName = "Johnny",
                    lastName = "Silverhand",
                    email = EMAIL,
                    region = "California",
                    city = "Night City",
                    line1 = "Crescent st. 24",
                    line2 = "Apt. 56",
                    postalCode = "0000",
                    phone = "+12345678901",
                ),
            ),
        )
    }

    @Test
    fun `GIVEN blank optional line2 WHEN order clicked THEN line2 is null`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(line2 = "")

        // Act
        model.form.onOrderClick()

        // Assert
        assertThat(submitted?.shippingAddress?.line2).isNull()
    }

    @Test
    fun `GIVEN surrounding whitespace WHEN order clicked THEN values trimmed`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(embossName = "  JOHNNY SILVERHAND  ")

        // Act
        model.form.onOrderClick()

        // Assert
        assertThat(submitted?.embossName).isEqualTo("JOHNNY SILVERHAND")
    }

    @Test
    fun `GIVEN accented latin address WHEN filled THEN no error and submittable`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.fillValidForm(city = "São Paulo", region = "Île-de-France", lastName = "Müller")

        // Assert
        assertThat(model.form.city.error).isNull()
        assertThat(model.form.lastName.error).isNull()
        assertThat(model.form.isOrderEnabled).isTrue()
    }

    @Test
    fun `GIVEN non-latin city WHEN still typing THEN no error yet but order disabled`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.fillValidForm(city = "Москва")

        // Assert
        assertThat(model.form.city.error).isNull()
        assertThat(model.form.isOrderEnabled).isFalse()
    }

    @Test
    fun `GIVEN non-latin city WHEN field blurred THEN error shown and not submitted`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(city = "Москва")

        // Act
        model.form.city.onFocusChange(false)
        model.form.onOrderClick()

        // Assert
        assertThat(model.form.city.error).isEqualTo(OrderFieldError.Invalid)
        assertThat(submitted).isNull()
    }

    @Test
    fun `GIVEN incomplete phone WHEN field blurred THEN error shown and order disabled`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(phoneDigits = "234")

        // Act
        model.form.phone.onFocusChange(false)

        // Assert
        assertThat(model.form.phone.error).isEqualTo(OrderFieldError.Invalid)
        assertThat(model.form.isOrderEnabled).isFalse()
        assertThat(submitted).isNull()
    }

    @Test
    fun `GIVEN error shown after blur WHEN editing again THEN error cleared`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(city = "Москва")
        model.form.city.onFocusChange(false)

        // Act
        model.form.city.onValueChange("Night City")

        // Assert
        assertThat(model.form.city.error).isNull()
        assertThat(model.form.isOrderEnabled).isTrue()
    }

    @Test
    fun `GIVEN focus gained on invalid field WHEN not blurred THEN no error`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(city = "Москва")

        // Act
        model.form.city.onFocusChange(true)

        // Assert
        assertThat(model.form.city.error).isNull()
    }

    @Test
    fun `GIVEN empty required field WHEN blurred THEN error shown and order disabled`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.form.postalCode.onFocusChange(false)

        // Assert
        assertThat(model.form.postalCode.error).isEqualTo(OrderFieldError.Required)
        assertThat(model.form.isOrderEnabled).isFalse()
    }

    @Test
    fun `GIVEN empty optional line2 WHEN blurred THEN no error`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.form.addressLine2.onFocusChange(false)

        // Assert
        assertThat(model.form.addressLine2.error).isNull()
    }

    @Test
    fun `GIVEN non-ascii emboss name WHEN blurred THEN invalid error and order disabled`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(embossName = "José")

        // Act
        model.form.embossName.onFocusChange(false)

        // Assert
        assertThat(model.form.embossName.error).isEqualTo(OrderFieldError.Invalid)
        assertThat(model.form.isOrderEnabled).isFalse()
    }

    @Test
    fun `GIVEN number pasted with the country code WHEN entered THEN the code is dropped`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.form.phone.onValueChange("+1 (234) 567-8901")

        // Assert
        assertThat(model.form.phone.value).isEqualTo("2345678901")
    }

    @Test
    fun `GIVEN a complete number WHEN one more digit typed THEN the number is unchanged`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.form.phone.onValueChange("2345678901")

        // Act
        model.form.phone.onValueChange("23456789019")

        // Assert
        assertThat(model.form.phone.value).isEqualTo("2345678901")
    }

    @Test
    fun `GIVEN phone digits beyond the mask WHEN entered THEN value is capped and non-digits stripped`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.form.phone.onValueChange("(234) 567-8901234")

        // Assert
        assertThat(model.form.phone.value).isEqualTo("2345678901")
    }

    @Test
    fun `GIVEN load failed WHEN retried successfully THEN form shown`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns VisaApiError.Unspecified.left()
        val model = createLoadedModel()
        assertThat(model.state.value).isInstanceOf(Error::class.java)

        // Act
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
        (model.state.value as Error).onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.form.phoneMask).isEqualTo(PHONE_MASK)
    }

    @Test
    fun `GIVEN a slow retry in flight WHEN retried again THEN the superseded load cannot overwrite state`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns VisaApiError.Unspecified.left()
        val model = createLoadedModel()
        val errorState = model.state.value as Error
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } coAnswers {
            delay(SLOW_LOAD_MS)
            VisaApiError.Unspecified.left()
        }

        // Act
        errorState.onRetry()
        runCurrent()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
        errorState.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value).isInstanceOf(Form::class.java)
        assertThat(model.form.country).isEqualTo(COUNTRY)
    }

    @Test
    fun `GIVEN model WHEN back clicked THEN router popped`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.state.value.onBackClick()

        // Assert
        verify { router.pop() }
    }

    @Test
    fun `GIVEN model WHEN close clicked THEN close callback invoked`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.state.value.onCloseClick()

        // Assert
        assertThat(isClosed).isTrue()
    }

    internal data class LoadErrorModel(
        val country: String? = COUNTRY,
        val phoneMask: String? = PHONE_MASK,
        val email: String? = EMAIL,
        val requestFails: Boolean = false,
    )

    private val TangemPayOrderCardDataModel.form: Form
        get() = state.value as Form

    private fun TangemPayOrderCardDataModel.fillValidForm(
        embossName: String = "JOHNNY SILVERHAND",
        firstName: String = "Johnny",
        lastName: String = "Silverhand",
        region: String = "California",
        city: String = "Night City",
        line1: String = "Crescent st. 24",
        line2: String = "Apt. 56",
        postalCode: String = "0000",
        phoneDigits: String = "2345678901",
    ) {
        form.embossName.onValueChange(embossName)
        form.firstName.onValueChange(firstName)
        form.lastName.onValueChange(lastName)
        form.region.onValueChange(region)
        form.city.onValueChange(city)
        form.addressLine1.onValueChange(line1)
        form.addressLine2.onValueChange(line2)
        form.postalCode.onValueChange(postalCode)
        form.phone.onValueChange(phoneDigits)
    }

    private fun TestScope.createLoadedModel(): TangemPayOrderCardDataModel =
        createModel(testScope = this).also { advanceUntilIdle() }

    private fun createModel(testScope: TestScope) = TangemPayOrderCardDataModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardDataComponent.Params(
                userWalletId = userWalletId,
                onOrderSubmitted = { submitted = it },
                onClose = { isClosed = true },
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        onboardingRepository = onboardingRepository,
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

    private fun customerInfo(
        country: String? = COUNTRY,
        phoneMask: String? = PHONE_MASK,
        email: String? = EMAIL,
    ) = CustomerInfo(
        customerId = "c1",
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = null,
        cryptoBalance = null,
        availableForWithdrawal = BigDecimal.ZERO,
        tariffPlan = null,
        country = country,
        phoneMask = phoneMask,
        email = email,
    )

    companion object {

        @JvmStatic
        fun masklessCases() = listOf(null, "", "(###) ###-####", "+1 (XXX) XXX")

        @JvmStatic
        fun loadErrorCases() = listOf(
            LoadErrorModel(requestFails = true),
            LoadErrorModel(email = null),
            LoadErrorModel(country = null, phoneMask = null),
        )
    }
}