package com.tangem.features.tangempay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TangemPayDetailsStateFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    private val activeUnfrozenCard = TangemPayCard(
        id = "card_1",
        productInstanceId = "pi_card_1",
        cardStatus = TangemPayCard.Status.ACTIVE,
        hasPinCode = false,
        displayName = null,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        lastDigits = "1234",
        limit = null,
        state = TangemPayCardState.Active,
    )

    private fun createFactory() = TangemPayDetailsStateFactory(
        onBack = {},
        onOpenMenu = {},
        intents = intents,
        isRedesignEnabled = true,
        isRemoveAccountEnabled = true,
        isMultipleCardsEnabled = true,
    )

    private fun loadedStatus(
        statusSource: StatusSource,
        statusError: PaymentAccountStatusValue.Error?,
        statusCards: List<TangemPayCard> = listOf(activeUnfrozenCard),
    ): PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns statusSource
        every { error } returns statusError
        every { cards } returns statusCards
    }

    @ParameterizedTest
    @MethodSource("provideButtonStateCases")
    fun `GIVEN status source WHEN getLoadedState THEN buttons enabled only for actual data without error`(
        case: ButtonStateCase,
    ) {
        // Arrange
        val factory = createFactory()
        val status = loadedStatus(statusSource = case.source, statusError = case.error)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val actionButtonsEnabled = state.balanceBlockState.actionButtons.map { it.isEnabled }
        assertThat(actionButtonsEnabled).containsExactly(case.expectedEnabled, case.expectedEnabled)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isEqualTo(case.expectedEnabled)
    }

    @Test
    fun `GIVEN actual status with only frozen card WHEN getLoadedState THEN action buttons disabled`() {
        // Arrange
        val factory = createFactory()
        val frozenCard = activeUnfrozenCard.copy(frozenState = TangemPayCardFrozenState.Frozen)
        val status = loadedStatus(
            statusSource = StatusSource.ACTUAL,
            statusError = null,
            statusCards = listOf(frozenCard),
        )

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState.actionButtons.map { it.isEnabled }).containsExactly(false, false)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isTrue()
    }

    @Test
    fun `GIVEN actual status with issuing card WHEN getLoadedState THEN add card disabled`() {
        // Arrange
        val factory = createFactory()
        val issuingCard = activeUnfrozenCard.copy(state = TangemPayCardState.Issuing)
        val status = loadedStatus(
            statusSource = StatusSource.ACTUAL,
            statusError = null,
            statusCards = listOf(activeUnfrozenCard, issuingCard),
        )

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isFalse()
    }

    internal data class ButtonStateCase(
        val source: StatusSource,
        val error: PaymentAccountStatusValue.Error?,
        val expectedEnabled: Boolean,
    )

    private companion object {
        @JvmStatic
        fun provideButtonStateCases() = listOf(
            // Fresh data from the network -> actions allowed.
            ButtonStateCase(source = StatusSource.ACTUAL, error = null, expectedEnabled = true),
            // Cache restored from disk before a refresh confirms it -> actions blocked.
            ButtonStateCase(source = StatusSource.CACHE, error = null, expectedEnabled = false),
            // Internet unavailable, only cache left ([REDACTED_TASK_KEY] case 1) -> actions blocked.
            ButtonStateCase(source = StatusSource.ONLY_CACHE, error = null, expectedEnabled = false),
            // Expired refresh token, only cache left ([REDACTED_TASK_KEY] case 2) -> actions blocked.
            ButtonStateCase(
                source = StatusSource.ONLY_CACHE,
                error = PaymentAccountStatusValue.Error.NotSynced,
                expectedEnabled = false,
            ),
            // Transient error overlaid on actual data -> actions blocked.
            ButtonStateCase(
                source = StatusSource.ACTUAL,
                error = PaymentAccountStatusValue.Error.Unavailable,
                expectedEnabled = false,
            ),
        )
    }
}