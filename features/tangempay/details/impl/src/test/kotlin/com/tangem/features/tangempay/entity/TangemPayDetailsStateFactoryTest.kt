package com.tangem.features.tangempay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.addFundsButton
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.withdrawButton
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class TangemPayDetailsStateFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    private val activeUnfrozenCard = tangemPayCard()

    private val factory = TangemPayDetailsStateFactory(
        onBack = {},
        onOpenMenu = {},
        intents = intents,
        isRedesignEnabled = true,
        isRemoveAccountEnabled = true,
        isMultipleCardsEnabled = true,
        isTiersPlusPlanEnabled = true,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    @ParameterizedTest
    @MethodSource("provideButtonStateCases")
    fun `GIVEN status source WHEN getLoadedState THEN action buttons gated by freshness but card tile only by error`(
        case: ButtonStateCase,
    ) {
        // Arrange
        val status = loadedStatus(statusSource = case.source, statusError = case.error)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val actionButtonsEnabled = state.balanceBlockState.actionButtons.map { it.config.isEnabled }
        assertThat(actionButtonsEnabled).containsExactly(case.expectedEnabled, case.expectedEnabled)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isEqualTo(case.expectedEnabled)
        // The card tile is intentionally NOT source-gated: it stays clickable on stale data as long as
        // there is no error, so the user can still view the card details offline (which self-gate actions).
        assertThat(state.balanceBlockState.cardsBlockState?.cards?.single()?.isEnabled)
            .isEqualTo(case.expectedCardEnabled)
    }

    @Test
    fun `GIVEN actual status with only frozen card WHEN getLoadedState THEN action buttons disabled`() {
        // Arrange
        val frozenCard = activeUnfrozenCard.copy(frozenState = TangemPayCardFrozenState.Frozen)
        val status = loadedStatus(
            statusSource = StatusSource.ACTUAL,
            statusError = null,
            statusCards = listOf(frozenCard),
        )

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState.actionButtons.map { it.config.isEnabled }).containsExactly(false, false)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isTrue()
    }

    @Test
    fun `GIVEN actual status with issuing card WHEN getLoadedState THEN add card disabled`() {
        // Arrange
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

    @ParameterizedTest
    @MethodSource("provideBalanceCases")
    fun `GIVEN fresh status WHEN getLoadedState THEN withdraw gated by balance but add funds enabled`(
        case: BalanceCase,
    ) {
        // Arrange
        val status = loadedStatus(availableForWithdrawal = case.availableForWithdrawal)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isEqualTo(case.expectedWithdrawEnabled)
    }

    @Test
    fun `GIVEN deactivated with positive balance WHEN getDeactivatedState THEN withdraw enabled`() {
        // Act
        val state = factory.getDeactivatedState(hasWithdrawableBalance = true)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN deactivated with zero balance WHEN getDeactivatedState THEN withdraw disabled`() {
        // Act
        val state = factory.getDeactivatedState(hasWithdrawableBalance = false)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN withdraw disabled WHEN getActionButtonsConfig THEN withdraw disabled and add funds enabled`() {
        // Act
        val buttons = factory.getActionButtonsConfig(isAddFundsEnabled = true, isWithdrawEnabled = false)

        // Assert
        assertThat(buttons.addFundsButton.isEnabled).isTrue()
        assertThat(buttons.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN both enabled WHEN getActionButtonsConfig THEN both buttons enabled`() {
        // Act
        val buttons = factory.getActionButtonsConfig(isAddFundsEnabled = true, isWithdrawEnabled = true)

        // Assert
        assertThat(buttons.addFundsButton.isEnabled).isTrue()
        assertThat(buttons.withdrawButton.isEnabled).isTrue()
    }

    private fun loadedStatus(
        statusSource: StatusSource = StatusSource.ACTUAL,
        statusError: PaymentAccountStatusValue.Error? = null,
        statusCards: List<TangemPayCard> = listOf(activeUnfrozenCard),
        availableForWithdrawal: BigDecimal = BigDecimal.TEN,
    ): PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns statusSource
        every { error } returns statusError
        every { cards } returns statusCards
        every { balance } returns PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal.ZERO,
                currency = "USD",
            ),
            cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                id = "id",
                chainId = 1L,
                depositAddress = "address",
                tokenContractAddress = "contract",
                balance = BigDecimal.ZERO,
            ),
            availableForWithdrawal = availableForWithdrawal,
        )
    }

    internal data class ButtonStateCase(
        val source: StatusSource,
        val error: PaymentAccountStatusValue.Error?,
        val expectedEnabled: Boolean,
        val expectedCardEnabled: Boolean,
    )

    internal data class BalanceCase(
        val availableForWithdrawal: BigDecimal,
        val expectedWithdrawEnabled: Boolean,
    )

    private companion object {
        @JvmStatic
        fun provideBalanceCases() = listOf(
            BalanceCase(availableForWithdrawal = BigDecimal.ZERO, expectedWithdrawEnabled = false),
            BalanceCase(availableForWithdrawal = BigDecimal.TEN, expectedWithdrawEnabled = true),
            BalanceCase(availableForWithdrawal = BigDecimal("-1"), expectedWithdrawEnabled = false),
        )

        @JvmStatic
        fun provideButtonStateCases() = listOf(
            // Fresh data from the network -> actions allowed, card tile clickable.
            ButtonStateCase(
                source = StatusSource.ACTUAL,
                error = null,
                expectedEnabled = true,
                expectedCardEnabled = true,
            ),
            // Cache restored from disk before a refresh confirms it -> actions blocked, card tile clickable.
            ButtonStateCase(
                source = StatusSource.CACHE,
                error = null,
                expectedEnabled = false,
                expectedCardEnabled = true,
            ),
            // Internet unavailable, only cache left ([REDACTED_TASK_KEY] case 1) -> actions blocked, card tile clickable.
            ButtonStateCase(
                source = StatusSource.ONLY_CACHE,
                error = null,
                expectedEnabled = false,
                expectedCardEnabled = true,
            ),
            // Expired refresh token, only cache left ([REDACTED_TASK_KEY] case 2) -> everything blocked by the error.
            ButtonStateCase(
                source = StatusSource.ONLY_CACHE,
                error = PaymentAccountStatusValue.Error.NotSynced,
                expectedEnabled = false,
                expectedCardEnabled = false,
            ),
            // Transient error overlaid on actual data -> everything blocked by the error.
            ButtonStateCase(
                source = StatusSource.ACTUAL,
                error = PaymentAccountStatusValue.Error.Unavailable,
                expectedEnabled = false,
                expectedCardEnabled = false,
            ),
        )
    }
}