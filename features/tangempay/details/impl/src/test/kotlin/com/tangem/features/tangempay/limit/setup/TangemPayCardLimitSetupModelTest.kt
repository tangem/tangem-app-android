package com.tangem.features.tangempay.limit.setup

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardLimit
import com.tangem.domain.models.pay.TangemPayCardLimitData
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.SetTangemPayCardLimitUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class TangemPayCardLimitSetupModelTest {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val setLimitUseCase: SetTangemPayCardLimitUseCase = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val initialCard = TangemPayCard(
        id = cardId,
        hasPinCode = false,
        displayName = null,
        isFrozen = false,
        lastDigits = "1234",
        limit = null,
    )

    private val initialStatus: AccountStatus.Payment = AccountStatus.Payment(
        account = Account.Payment(userWalletId = userWalletId),
        value = mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
            every { cards } returns listOf(initialCard)
        },
    )

    private val params = TangemPayDetailsContainerComponent.Params(initialStatus = initialStatus)

    private fun createModel(
        adminLimit: BigDecimal? = BigDecimal("1000"),
    ): TangemPayCardLimitSetupModel {
        val cardWithLimit = TangemPayCard(
            id = cardId,
            hasPinCode = false,
            displayName = null,
            isFrozen = false,
            lastDigits = "1234",
            limit = TangemPayCardLimitData(
                actualCardLimit = null,
                adminCardLimit = adminLimit?.let {
                    TangemPayCardLimit(
                        amount = adminLimit,
                        period = TangemPayCardLimitPeriod.DAY,
                    )
                }
            ),
            isReissuing = false,
        )
        val statusWithLimit: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { cards } returns listOf(cardWithLimit)
            every { currencyCode } returns "USD"
        }
        val paymentStatusWithLimit: AccountStatus.Payment = mockk(relaxed = true) {
            every { value } returns statusWithLimit
        }
        every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(paymentStatusWithLimit)

        return TangemPayCardLimitSetupModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = TestingCoroutineDispatcherProvider(),
            router = router,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            setTangemPayCardLimitUseCase = setLimitUseCase,
            uiMessageSender = uiMessageSender,
            analytics = analytics,
        )
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    fun `GIVEN amount WHEN changed THEN submit button reflects validity`(
        amount: String,
        expectedEnabled: Boolean,
    ) {
        val model = createModel()

        model.uiState.value.amountFieldModel.onValueChange("100")
        model.uiState.value.amountFieldModel.onValueChange(amount)

        assertThat(model.uiState.value.isSubmitButtonEnabled).isEqualTo(expectedEnabled)
        model.onDestroy()
    }

    @Test
    fun `GIVEN max limit null WHEN changed THEN submit button is enabled`() {
        val model = createModel(adminLimit = null)

        model.uiState.value.amountFieldModel.onValueChange("100")

        assertThat(model.uiState.value.isSubmitButtonEnabled).isTrue()
        model.onDestroy()
    }

    @Nested
    inner class Analytics {
        @Test
        fun `GIVEN model WHEN init THEN LimitManagementOpened is sent`() {
            val model = createModel()

            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.LimitManagementOpened()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN model WHEN onSubmitClick THEN LimitChangeConfirmed is sent`() {
            val model = createModel()

            model.uiState.value.amountFieldModel.onValueChange("100")
            model.uiState.value.onSubmitClick()
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.LimitChangeConfirmed("100")) }

            model.onDestroy()
        }
    }

    @Nested
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

    private companion object {
        @JvmStatic
        fun provideTestCases() = listOf(
            Arguments.of("0", false),
            Arguments.of("0.99", false),
            Arguments.of("1", true),
            Arguments.of("100", true),
            Arguments.of("-1", false),
            Arguments.of("", false),
            Arguments.of("abc", true),
            Arguments.of("1001", false),
        )
    }
}