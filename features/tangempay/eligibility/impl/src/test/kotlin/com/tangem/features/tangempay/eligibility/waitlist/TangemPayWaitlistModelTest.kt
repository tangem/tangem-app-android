package com.tangem.features.tangempay.eligibility.waitlist

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.usecase.JoinTangemPayWaitlistUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayWaitlistModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val joinWaitlistUseCase: JoinTangemPayWaitlistUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, urlOpener, joinWaitlistUseCase, analytics, onDismiss)
        coEvery { joinWaitlistUseCase(any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN params WHEN model created THEN initial state and screen opened event sent`() = runTest {
        // Act
        val model = createModel(testScope = this)

        // Assert
        val state = model.uiState.value
        assertThat(state.email).isEmpty()
        assertThat(state.emailError).isNull()
        assertThat(state.isMarketingConsentGiven).isFalse()
        assertThat(state.isSaveEnabled).isFalse()
        assertThat(state.isSaveInProgress).isFalse()
        verify(exactly = 1) {
            analytics.send(match { it is TangemPayAnalyticsEvents.Eligibility.WaitlistScreenOpened })
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN email error shown WHEN email changed THEN email updated, error cleared and save enabled`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(INVALID_EMAIL)
        model.uiState.value.onSaveClick()

        // Act
        model.uiState.value.onEmailChange(VALID_EMAIL)

        // Assert
        val state = model.uiState.value
        assertThat(state.email).isEqualTo(VALID_EMAIL)
        assertThat(state.emailError).isNull()
        assertThat(state.isSaveEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN blank email WHEN email changed THEN save disabled`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onEmailChange("   ")

        // Assert
        assertThat(model.uiState.value.isSaveEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN entered email WHEN clear clicked THEN email reset and save disabled`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)

        // Act
        model.uiState.value.onEmailClearClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.email).isEmpty()
        assertThat(state.emailError).isNull()
        assertThat(state.isSaveEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN consent not given WHEN consent clicked twice THEN consent toggled on and off`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onMarketingConsentClick()
        val afterFirstClick = model.uiState.value.isMarketingConsentGiven
        model.uiState.value.onMarketingConsentClick()

        // Assert
        assertThat(afterFirstClick).isTrue()
        assertThat(model.uiState.value.isMarketingConsentGiven).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN privacy policy clicked THEN privacy policy url opened`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onPrivacyPolicyClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith("https://tangem.com/docs/en/privacy-policy.pdf") }) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN dismiss called THEN onDismiss invoked`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onDismiss()

        // Assert
        verify(exactly = 1) { onDismiss() }
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN invalid email WHEN save clicked THEN format error shown and waitlist not joined`(email: String) =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.onEmailChange(email)

            // Act
            model.uiState.value.onSaveClick()
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.emailError).isEqualTo(resourceReference(R.string.common_error_email_incorrect_format))
            assertThat(state.isSaveInProgress).isFalse()
            coVerify(exactly = 0) { joinWaitlistUseCase(any(), any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN valid email with spaces WHEN save clicked THEN waitlist joined with trimmed email and params`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.onEmailChange("  $VALID_EMAIL  ")
            model.uiState.value.onMarketingConsentClick()

            // Act
            model.uiState.value.onSaveClick()
            advanceUntilIdle()

            // Assert
            val expectedEntry = TangemPayWaitlistEntry(
                email = VALID_EMAIL,
                product = PRODUCT,
                countryCode = COUNTRY_CODE,
                regionCode = REGION_CODE,
                isMarketingConsentGiven = true,
            )
            coVerify(exactly = 1) { joinWaitlistUseCase(USER_WALLET_ID, expectedEntry, any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN join in progress WHEN state observed THEN save locked and input ignored`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)

        // Act
        model.uiState.value.onSaveClick()
        model.uiState.value.onEmailChange(OTHER_EMAIL)
        model.uiState.value.onMarketingConsentClick()
        model.uiState.value.onSaveClick()

        // Assert
        val state = model.uiState.value
        assertThat(state.isSaveInProgress).isTrue()
        assertThat(state.isSaveEnabled).isFalse()
        assertThat(state.email).isEqualTo(VALID_EMAIL)
        assertThat(state.isMarketingConsentGiven).isFalse()
        advanceUntilIdle()
        coVerify(exactly = 1) { joinWaitlistUseCase(any(), any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN join succeeds WHEN save clicked THEN saved event sent, sheet dismissed and wallet opened`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)
        val message = slot<SnackbarMessage>()

        // Act
        model.uiState.value.onSaveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(match { it is TangemPayAnalyticsEvents.Eligibility.WaitlistSaved })
        }
        verify(exactly = 1) { onDismiss() }
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message).isEqualTo(resourceReference(R.string.tangempay_waitlist_joined_message))
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet, onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN join fails with invalid email WHEN save clicked THEN format error shown and save unlocked`() = runTest {
        // Arrange
        coEvery { joinWaitlistUseCase(any(), any(), any()) } returns VisaApiError.WaitlistInvalidEmail.left()
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)

        // Act
        model.uiState.value.onSaveClick()
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.emailError).isEqualTo(resourceReference(R.string.common_error_email_incorrect_format))
        assertThat(state.isSaveInProgress).isFalse()
        assertThat(state.isSaveEnabled).isTrue()
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { onDismiss() }
        verify(exactly = 0) { router.replaceAll(*anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN join fails with other error WHEN save clicked THEN error dialog shown and save unlocked`() = runTest {
        // Arrange
        coEvery { joinWaitlistUseCase(any(), any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)
        val message = slot<DialogMessage>()

        // Act
        model.uiState.value.onSaveClick()
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.emailError).isNull()
        assertThat(state.isSaveInProgress).isFalse()
        assertThat(state.isSaveEnabled).isTrue()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.title).isEqualTo(resourceReference(R.string.common_something_went_wrong))
        assertThat(message.captured.message).isEqualTo(resourceReference(R.string.common_try_again_later))
        verify(exactly = 0) { onDismiss() }
        verify(exactly = 0) { router.replaceAll(*anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN join failed WHEN save retried THEN same idempotency key used`() = runTest {
        // Arrange
        val keys = mutableListOf<String>()
        coEvery { joinWaitlistUseCase(any(), any(), capture(keys)) } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            Unit.right(),
        )
        val model = createModel(testScope = this)
        model.uiState.value.onEmailChange(VALID_EMAIL)
        model.uiState.value.onSaveClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onSaveClick()
        advanceUntilIdle()

        // Assert
        assertThat(keys).hasSize(2)
        assertThat(keys.distinct()).hasSize(1)
        model.onDestroy()
    }

    private fun provideTestModels() = listOf(
        "",
        "   ",
        "user",
        "user@",
        "@tangem.com",
        "user@tangem",
        "user@tangem.c",
        "user name@tangem.com",
        "user@@tangem.com",
    )

    private fun createModel(testScope: TestScope): TangemPayWaitlistModel = TangemPayWaitlistModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        uiMessageSender = uiMessageSender,
        urlOpener = urlOpener,
        joinWaitlistUseCase = joinWaitlistUseCase,
        analytics = analytics,
        paramsContainer = MutableParamsContainer(
            TangemPayWaitlistComponent.Params(
                userWalletId = USER_WALLET_ID,
                product = PRODUCT,
                countryCode = COUNTRY_CODE,
                regionCode = REGION_CODE,
                onDismiss = onDismiss,
            ),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        val PRODUCT = TangemPayProduct.PAYMENT_ACCOUNT
        const val COUNTRY_CODE = "US"
        const val REGION_CODE = "CA"
        const val VALID_EMAIL = "user@tangem.com"
        const val OTHER_EMAIL = "other@tangem.com"
        const val INVALID_EMAIL = "user@tangem"
    }
}