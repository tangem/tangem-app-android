package com.tangem.features.tangempay.card.limit.setup

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.usecase.SetTangemPayCardLimitUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class TangemPayCardLimitSetupModelTest : TangemPayModelTestBase() {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val setLimitUseCase: SetTangemPayCardLimitUseCase = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val initialCard = TangemPayCard(
        id = cardId,
        productInstanceId = "pi_$cardId",
        cardStatus = TangemPayCard.Status.ACTIVE,
        hasPinCode = false,
        displayName = null,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        lastDigits = "1234",
        limit = null,
        images = emptyList(),
        state = TangemPayCardState.Active,
        embossName = null,
        cardType = TangemPayCardType.VIRTUAL,
    )

    private val params = TangemPayCardLimitSetupComponent.Params(card = initialCard, userWalletId = userWalletId)

    @BeforeEach
    fun setUp() {
        clearMocks(setLimitUseCase, paymentAccountStatusSupplier, analytics, router, uiMessageSender)
    }

    private fun createModel(
        adminLimit: BigDecimal? = BigDecimal("1000"),
    ): TangemPayCardLimitSetupModel {
        val cardWithLimit = TangemPayCard(
            id = cardId,
            productInstanceId = "pi_$cardId",
            cardStatus = TangemPayCard.Status.ACTIVE,
            hasPinCode = false,
            displayName = null,
            frozenState = TangemPayCardFrozenState.Unfrozen,
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
            images = emptyList(),
            state = TangemPayCardState.Active,
            embossName = null,
            cardType = TangemPayCardType.VIRTUAL,
        )
        val statusWithLimit: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { cards } returns listOf(cardWithLimit)
            every { balance } returns mockk(relaxed = true) {
                every { fiatBalance } returns mockk(relaxed = true) {
                    every { currency } returns "USD"
                }
            }
        }
        val paymentStatusWithLimit: AccountStatus.Payment = mockk(relaxed = true) {
            every { value } returns statusWithLimit
        }
        every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(paymentStatusWithLimit)
        coEvery { setLimitUseCase(any(), any(), any()) } returns Unit.right()

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
        // Arrange
        val model = createModel()
        model.uiState.value.amountFieldModel.onValueChange("100")

        // Act
        model.uiState.value.amountFieldModel.onValueChange(amount)

        // Assert
        assertThat(model.uiState.value.isSubmitButtonEnabled).isEqualTo(expectedEnabled)
        model.onDestroy()
    }

    @Test
    fun `GIVEN max limit null WHEN changed THEN submit button is enabled`() {
        // Arrange
        val model = createModel(adminLimit = null)

        // Act
        model.uiState.value.amountFieldModel.onValueChange("100")

        // Assert
        assertThat(model.uiState.value.isSubmitButtonEnabled).isTrue()
        model.onDestroy()
    }

    @Nested
    inner class Analytics {
        @Test
        fun `GIVEN model WHEN init THEN LimitManagementOpened is sent`() {
            // Act
            val model = createModel()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.LimitManagementOpened()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN model WHEN onSubmitClick THEN LimitChangeConfirmed is sent`() {
            // Arrange
            val model = createModel()
            model.uiState.value.amountFieldModel.onValueChange("100")

            // Act
            model.uiState.value.onSubmitClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.LimitChangeConfirmed("100")) }

            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Presets {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN limit presets WHEN preset clicked THEN amount set to the preset value`(case: PresetModel) {
            // Arrange
            val model = createModel()

            // Act
            case.pick(model.uiState.value.presets).onClick()

            // Assert
            assertThat(model.uiState.value.amountFieldModel.value).isEqualTo(case.expectedAmount)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            PresetModel(name = "first preset -> MIN_LIMIT", pick = { it.first() }, expectedAmount = "1"),
            PresetModel(name = "last preset -> 25000", pick = { it.last() }, expectedAmount = "25000"),
        )
    }

    internal data class PresetModel(
        val name: String,
        val pick: (List<TangemPayCardLimitSetupUM.LimitPresetUM>) -> TangemPayCardLimitSetupUM.LimitPresetUM,
        val expectedAmount: String,
    ) {
        override fun toString(): String = name
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