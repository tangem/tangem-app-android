package com.tangem.features.tangempay.limit.setup

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.SetTangemPayCardLimitUseCase
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCardLimitSetupModelTest {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val setLimitUseCase: SetTangemPayCardLimitUseCase = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val params = TangemPayDetailsContainerComponent.Params(
        userWalletId = userWalletId,
        config = TangemPayDetailsConfig(
            customerId = "customer1",
            cardId = cardId,
            isPinSet = false,
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            cardNumberEnd = "1234",
            chainId = 1,
            isTangemPayDeactivated = false,
            displayName = null,
        ),
    )

    private val testCard = TangemPayCard(
        id = cardId,
        hasPinCode = false,
        displayName = null,
        limit = null,
        isFrozen = false,
        lastDigits = "1234",
    )

    private val loadedStatus: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns StatusSource.ACTUAL
        every { cards } returns listOf(testCard)
        every { currencyCode } returns "USD"
    }

    private val paymentStatus: AccountStatus.Payment = mockk(relaxed = true) {
        every { value } returns loadedStatus
    }

    private fun createModel(): TangemPayCardLimitSetupModel {
        every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(paymentStatus)
        return TangemPayCardLimitSetupModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = TestingCoroutineDispatcherProvider(),
            router = router,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            setTangemPayCardLimitUseCase = setLimitUseCase,
            uiMessageSender = uiMessageSender,
        )
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    fun `GIVEN amount WHEN changed THEN submit button reflects validity`(
        amount: String,
        expectedEnabled: Boolean,
    ) {
        val model = createModel()

        model.uiState.value.amountFieldModel.onValueChange(amount)

        assertThat(model.uiState.value.isSubmitButtonEnabled).isEqualTo(expectedEnabled)
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Presets {

        @Test
        fun `WHEN first preset clicked THEN amount set to MIN_LIMIT`() {
            val model = createModel()

            model.uiState.value.presets.first().onClick()

            assertThat(model.uiState.value.amountFieldModel.value).isEqualTo("1")
            model.onDestroy()
        }

        @Test
        fun `WHEN last preset clicked THEN amount set to 5000`() {
            val model = createModel()

            model.uiState.value.presets.last().onClick()

            assertThat(model.uiState.value.amountFieldModel.value).isEqualTo("25000")
            model.onDestroy()
        }
    }

    private fun provideTestCases() = listOf(
        Arguments.of("0", false),
        Arguments.of("0.99", false),
        Arguments.of("1", true),
        Arguments.of("100", true),
        Arguments.of("-1", false),
        Arguments.of("", false),
        Arguments.of("abc", false),
    )
}