package com.tangem.features.tangempay.orderCard.impl.model

import androidx.annotation.StringRes
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.EventMessage
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.IssuePlasticCardUseCase
import com.tangem.domain.pay.usecase.ReissuePlasticCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardIntent
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardDataComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Error
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form
import com.tangem.utils.CountryNames
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
private const val SOURCE_PRODUCT_INSTANCE_ID = "pi_source_0001"
private const val SOURCE_CARD_ID = "card_source_0001"
private const val SOURCE_CARD_EMBOSS_NAME = "V ARASAKA"
private val REISSUE_INTENT = TangemPayOrderCardIntent.ReissuePlastic(
    sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
    deliveryEtaMaxBusinessDays = 20,
)

internal class TangemPayOrderCardDataModelTest {

    private val userWalletId = UserWalletId("123")

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val onboardingRepository: OnboardingRepository = mockk()
    private val issuePlasticCard: IssuePlasticCardUseCase = mockk()
    private val reissuePlasticCard: ReissuePlasticCardUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private var submitted: PlasticCardOrder? = null
    private val submittedKeys = mutableListOf<String>()
    private var acceptedEmail: String? = null
    private var acceptedProductInstanceId: String? = null
    private var isClosed: Boolean = false
    private var model: TangemPayOrderCardDataModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(analytics)
        submittedKeys.clear()
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
        coEvery { issuePlasticCard(userWalletId, any(), any()) } coAnswers {
            submitted = secondArg()
            submittedKeys += thirdArg<String>()
            createdOrder().right()
        }
        coEvery { reissuePlasticCard(userWalletId, any(), any(), any()) } coAnswers {
            submitted = thirdArg()
            submittedKeys += arg<String>(n = 3)
            createdOrder(productInstanceId = REISSUED_PRODUCT_INSTANCE_ID).right()
        }
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
        assertThat(model.form.country).isEqualTo(CountryNames.getDisplayName(COUNTRY))
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
        advanceUntilIdle()

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
        advanceUntilIdle()

        // Assert
        assertThat(submitted).isEqualTo(
            PlasticCardOrder(
                embossName = "JOHNNY SILVERHAND",
                shippingAddress = ShippingAddress(
                    firstName = "Johnny",
                    lastName = "Silverhand",
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
        advanceUntilIdle()

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
        advanceUntilIdle()

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
        advanceUntilIdle()

        // Assert
        assertThat(model.form.city.error).isEqualTo(OrderFieldError.NonLatin)
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
        assertThat(model.form.embossName.error).isEqualTo(OrderFieldError.NonLatin)
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
        assertThat(model.form.country).isEqualTo(CountryNames.getDisplayName(COUNTRY))
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

    @Test
    fun `GIVEN order accepted WHEN order clicked THEN navigates to success with the customer email`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(acceptedEmail).isEqualTo(EMAIL)
        assertThat(acceptedProductInstanceId).isEqualTo(ORDERED_PRODUCT_INSTANCE_ID)
        assertThat(model.form.isSubmitting).isFalse()
    }

    @Test
    fun `GIVEN order rejected WHEN order clicked THEN error shown and stays on the form`() = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(acceptedEmail).isNull()
        assertThat(model.form.isSubmitting).isFalse()
        assertThat(model.form.isOrderEnabled).isTrue()
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN the reissue intent WHEN order clicked THEN a reissue is submitted for the source card`() = runTest {
        // Arrange
        val model = createLoadedModel(intent = REISSUE_INTENT)
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            reissuePlasticCard(userWalletId, SOURCE_PRODUCT_INSTANCE_ID, any(), any())
        }
        coVerify(exactly = 0) { issuePlasticCard(any(), any(), any()) }
        assertThat(acceptedEmail).isEqualTo(EMAIL)
        assertThat(acceptedProductInstanceId).isEqualTo(REISSUED_PRODUCT_INSTANCE_ID)
    }

    @Test
    fun `GIVEN the reissue intent WHEN the form loads THEN the emboss name is taken from the source card`() = runTest {
        // Act
        val model = createLoadedModel(intent = REISSUE_INTENT)

        // Assert
        assertThat(model.form.embossName.value).isEqualTo(SOURCE_CARD_EMBOSS_NAME)
        assertThat(model.form.embossName.isEditable).isFalse()
    }

    @Test
    fun `GIVEN the issue intent WHEN the form loads THEN the emboss name is empty and editable`() = runTest {
        // Act
        val model = createLoadedModel()

        // Assert
        assertThat(model.form.embossName.value).isEmpty()
        assertThat(model.form.embossName.isEditable).isTrue()
    }

    @Test
    fun `GIVEN a source card without an emboss name WHEN the address is filled THEN order is enabled`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
            customerInfo(sourceCardEmbossName = null).right()
        val model = createLoadedModel(intent = REISSUE_INTENT)

        // Act
        model.fillValidForm()

        // Assert
        assertThat(model.form.embossName.value).isEmpty()
        assertThat(model.form.isOrderEnabled).isTrue()
    }

    @Test
    fun `GIVEN the reissue intent WHEN order clicked THEN the source card emboss name is submitted`() =
        runTest {
            // Arrange
            val model = createLoadedModel(intent = REISSUE_INTENT)
            model.fillValidForm()

            // Act
            model.form.onOrderClick()
            advanceUntilIdle()

            // Assert
            assertThat(submitted).isEqualTo(
                PlasticCardOrder(
                    embossName = SOURCE_CARD_EMBOSS_NAME,
                    shippingAddress = ShippingAddress(
                        firstName = "Johnny",
                        lastName = "Silverhand",
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
    fun `GIVEN the reissue intent WHEN submit fails THEN nothing is accepted and the form stays editable`() = runTest {
        // Arrange
        coEvery { reissuePlasticCard(userWalletId, any(), any(), any()) } returns
            VisaApiError.CardReissuePlasticActiveOrderExists.left()
        val model = createLoadedModel(intent = REISSUE_INTENT)
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(acceptedEmail).isNull()
        assertThat(model.form.isSubmitting).isFalse()
        assertThat(model.form.isOrderEnabled).isTrue()
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN the issue intent WHEN order clicked THEN no reissue is submitted`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { issuePlasticCard(userWalletId, any(), any()) }
        coVerify(exactly = 0) { reissuePlasticCard(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN submit in flight WHEN order clicked again THEN the order is created once`() = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } coAnswers {
            delay(SLOW_LOAD_MS)
            createdOrder().right()
        }
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        runCurrent()
        assertThat(model.form.isSubmitting).isTrue()
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { issuePlasticCard(userWalletId, any(), any()) }
        assertThat(acceptedEmail).isEqualTo(EMAIL)
    }

    @Test
    fun `GIVEN order rejected WHEN retried THEN a second order is created and success is reached`() = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createLoadedModel()
        model.fillValidForm()
        model.form.onOrderClick()
        advanceUntilIdle()
        assertThat(acceptedEmail).isNull()

        // Act
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns createdOrder().right()
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { issuePlasticCard(userWalletId, any(), any()) }
        assertThat(acceptedEmail).isEqualTo(EMAIL)
    }

    @Test
    fun `GIVEN invalid form WHEN order clicked THEN no order is created`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm(city = "Москва")

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { issuePlasticCard(any(), any(), any()) }
        assertThat(acceptedEmail).isNull()
    }

    @Test
    fun `GIVEN order rejected WHEN retried from the sheet THEN the same key and payload are replayed`() = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } coAnswers {
            submitted = secondArg()
            submittedKeys += thirdArg<String>()
            VisaApiError.ServerUnavailable.left()
        }
        val model = createLoadedModel()
        model.fillValidForm()
        model.form.onOrderClick()
        advanceUntilIdle()
        val firstOrder = submitted

        // Act
        model.form.city.onValueChange("Pacifica")
        retryLastMessage()
        advanceUntilIdle()

        // Assert
        assertThat(submittedKeys).hasSize(2)
        assertThat(submittedKeys[1]).isEqualTo(submittedKeys[0])
        assertThat(submitted).isEqualTo(firstOrder)
    }

    @Test
    fun `GIVEN a non-retryable rejection WHEN order clicked THEN the sheet offers no retry`() = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(lastMessageButtons()).hasSize(1)
    }

    @ParameterizedTest
    @MethodSource("rejectionCases")
    fun `GIVEN a rejection with a known reason WHEN order clicked THEN that reason is shown`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns testModel.error.left()
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(lastMessageTitle()).isEqualTo(resourceReference(testModel.title))
        assertThat(lastMessageButtons()).hasSize(testModel.buttonCount)
        assertThat(model.form.isSubmitting).isFalse()
    }

    @ParameterizedTest
    @MethodSource("rejectionCases")
    fun `GIVEN a rejection sheet WHEN closed THEN the flow is left only when the form cannot fix it`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { issuePlasticCard(userWalletId, any(), any()) } returns testModel.error.left()
        val model = createLoadedModel()
        model.fillValidForm()
        model.form.onOrderClick()
        advanceUntilIdle()

        // Act
        closeLastMessage()

        // Assert
        assertThat(isClosed).isEqualTo(testModel.closesFlow)
    }

    @ParameterizedTest
    @MethodSource("reissueRejectionCases")
    fun `GIVEN a reissue rejection with a known reason WHEN order clicked THEN that reason is shown`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { reissuePlasticCard(userWalletId, any(), any(), any()) } returns testModel.error.left()
        val model = createLoadedModel(intent = REISSUE_INTENT)
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        assertThat(lastMessageTitle()).isEqualTo(resourceReference(testModel.title))
        assertThat(lastMessageButtons()).hasSize(testModel.buttonCount)
        assertThat(model.form.isSubmitting).isFalse()
    }

    @ParameterizedTest
    @MethodSource("reissueRejectionCases")
    fun `GIVEN a reissue rejection sheet WHEN closed THEN the flow is left only when the form cannot fix it`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { reissuePlasticCard(userWalletId, any(), any(), any()) } returns testModel.error.left()
        val model = createLoadedModel(intent = REISSUE_INTENT)
        model.fillValidForm()
        model.form.onOrderClick()
        advanceUntilIdle()

        // Act
        closeLastMessage()

        // Assert
        assertThat(isClosed).isEqualTo(testModel.closesFlow)
    }

    internal data class RejectionModel(
        val error: VisaApiError,
        @StringRes val title: Int,
        val buttonCount: Int = 1,
        val closesFlow: Boolean = true,
    )

    internal data class LoadErrorModel(
        val country: String? = COUNTRY,
        val phoneMask: String? = PHONE_MASK,
        val email: String? = EMAIL,
        val requestFails: Boolean = false,
    )

    @Test
    fun `GIVEN the address screen WHEN the model is created THEN the screen opened event is sent`() = runTest {
        // Act
        createLoadedModel()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.AddressScreenOpened>()) }
    }

    @Test
    fun `GIVEN a valid form WHEN order clicked twice THEN the order card clicked event is sent once`() = runTest {
        // Arrange
        val model = createLoadedModel()
        model.fillValidForm()

        // Act
        model.form.onOrderClick()
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.OrderCardClicked>()) }
    }

    @Test
    fun `GIVEN an incomplete form WHEN order clicked THEN no order card clicked event is sent`() = runTest {
        // Arrange
        val model = createLoadedModel()

        // Act
        model.form.onOrderClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.OrderCardClicked>()) }
    }

    private val TangemPayOrderCardDataModel.form: Form
        get() = state.value as Form

    private fun lastMessageButtons(): List<MessageBottomSheetUM.Button> {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        return (slot.captured as BottomSheetMessage).messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.Button>()
    }

    private fun lastMessageTitle(): TextReference? {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        return (slot.captured as BottomSheetMessage).messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.InfoBlock>()
            .firstNotNullOfOrNull { it.title }
    }

    private fun closeLastMessage() {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        val sheet = (slot.captured as BottomSheetMessage).messageBottomSheetUM
        sheet.elements.filterIsInstance<MessageBottomSheetUM.Button>().first().onClick?.invoke(sheet.closeScope)
    }

    private fun retryLastMessage() {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        val sheet = (slot.captured as BottomSheetMessage).messageBottomSheetUM
        sheet.elements.filterIsInstance<MessageBottomSheetUM.Button>().last().onClick?.invoke(sheet.closeScope)
    }

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

    private fun createdOrder(productInstanceId: String = ORDERED_PRODUCT_INSTANCE_ID) = Order(
        id = "plastic-issue-order",
        customerId = "customer",
        type = OrderType.CARD_ISSUE_PLASTIC_RAIN,
        status = OrderStatus.NEW,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = productInstanceId,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private fun TestScope.createLoadedModel(
        intent: TangemPayOrderCardIntent = TangemPayOrderCardIntent.Issue,
    ): TangemPayOrderCardDataModel = createModel(testScope = this, intent = intent).also { advanceUntilIdle() }

    private fun createModel(
        testScope: TestScope,
        intent: TangemPayOrderCardIntent = TangemPayOrderCardIntent.Issue,
    ) = TangemPayOrderCardDataModel(
        paramsContainer = MutableParamsContainer(
            TangemPayOrderCardDataComponent.Params(
                userWalletId = userWalletId,
                intent = intent,
                onOrderAccepted = { email, productInstanceId ->
                    acceptedEmail = email
                    acceptedProductInstanceId = productInstanceId
                },
                onClose = { isClosed = true },
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
        router = router,
        onboardingRepository = onboardingRepository,
        issuePlasticCard = issuePlasticCard,
        reissuePlasticCard = reissuePlasticCard,
        uiMessageSender = uiMessageSender,
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
        sourceCardEmbossName: String? = SOURCE_CARD_EMBOSS_NAME,
    ) = CustomerInfo(
        customerId = "c1",
        paymentAccount = null,
        productInstances = listOf(sourceProductInstance()),
        cards = listOf(sourceCard(embossName = sourceCardEmbossName)),
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

    private fun sourceProductInstance() = CustomerInfo.ProductInstance(
        id = SOURCE_PRODUCT_INSTANCE_ID,
        cardId = SOURCE_CARD_ID,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = CustomerInfo.ProductInstance.Status.ACTIVE,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.CARD,
    )

    private fun sourceCard(embossName: String?) = CustomerInfo.CardInfo(
        cardId = SOURCE_CARD_ID,
        cardStatus = TangemPayCard.Status.ACTIVE,
        lastFourDigits = "1234",
        isPinSet = true,
        images = emptyList(),
        embossName = embossName,
        cardType = TangemPayCardType.PHYSICAL,
    )

    companion object {

        private const val ORDERED_PRODUCT_INSTANCE_ID = "pi-ordered"
        private const val REISSUED_PRODUCT_INSTANCE_ID = "pi-reissued"

        @JvmStatic
        fun masklessCases() = listOf(null, "", "(###) ###-####", "+1 (XXX) XXX")

        @JvmStatic
        fun loadErrorCases() = listOf(
            LoadErrorModel(requestFails = true),
            LoadErrorModel(email = null),
            LoadErrorModel(country = null, phoneMask = null),
        )

        @JvmStatic
        fun reissueRejectionCases() = listOf(
            RejectionModel(
                error = VisaApiError.CardReissuePlasticInvalidShippingAddress,
                title = R.string.tangempay_order_card_error_invalid_address,
                closesFlow = false,
            ),
            RejectionModel(
                error = VisaApiError.CardReissuePlasticInsufficientBalance,
                title = R.string.tangempay_order_card_error_insufficient_balance,
            ),
            RejectionModel(
                error = VisaApiError.CardReissuePlasticActiveOrderExists,
                title = R.string.tangempay_order_card_error_active_order,
            ),
            RejectionModel(
                error = VisaApiError.CardReissuePlasticNotAvailable,
                title = R.string.tangempay_order_card_error_offer_unavailable,
            ),
            RejectionModel(
                error = VisaApiError.CardReissuePlasticInvalidSourceCard,
                title = R.string.tangempay_order_card_error_invalid_source_card,
            ),
        )

        @JvmStatic
        fun rejectionCases() = listOf(
            RejectionModel(
                error = VisaApiError.CardIssueInvalidShippingAddress,
                title = R.string.tangempay_order_card_error_invalid_address,
                closesFlow = false,
            ),
            RejectionModel(
                error = VisaApiError.CardIssueInsufficientBalance,
                title = R.string.tangempay_order_card_error_insufficient_balance,
            ),
            RejectionModel(
                error = VisaApiError.CardIssueActiveOrderExists,
                title = R.string.tangempay_order_card_error_active_order,
            ),
            RejectionModel(
                error = VisaApiError.CardIssueOfferNotAvailable,
                title = R.string.tangempay_order_card_error_offer_unavailable,
            ),
        )
    }
}