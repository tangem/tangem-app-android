package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapSlippageUM
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StateBuilderSlippageTest {

    private val userWalletId = UserWalletId("aabbccdd")
    private val userWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    private val sut = StateBuilder(
        actions = mockk<UiActions>(relaxed = true),
        isBalanceHiddenProvider = Provider { false },
        appCurrencyProvider = Provider { AppCurrency.Default },
        isAccountsModeProvider = Provider { false },
        isGaslessFeeSupportedForNetwork = mockk(),
        appRouter = mockk(),
        isChooseTokenPulseEnabled = false,
        isCustomSlippageEnabled = true,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN shown slippage WHEN quotes load THEN slippage kept only for Personal destination`(
        model: QuotesLoadingModel,
    ) {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(userWallet)
        val toStatus = buildSwapCurrencyStatus(userWallet).copy(account = model.toAccount)
        val shownSlippage = SwapSlippageUM.Content(value = stringReference("0.5%"), isHighlighted = false, onClick = {})
        val uiState = sut.createInitialReadyState(
            uiStateHolder = sut.createInitialLoadingState(),
            emptyAmountState = SwapState.EmptyAmountState(zeroAmountEquivalent = stringReference("$0.00")),
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = buildSwapCurrencyStatus(userWallet),
        ).copy(slippageUM = shownSlippage)

        // Act
        val actual = sut.createQuotesLoadingState(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            uiStateHolder = uiState,
        )

        // Assert
        val expected = if (model.isKept) shownSlippage else SwapSlippageUM.Hidden
        assertThat(actual.slippageUM).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        QuotesLoadingModel(toAccount = Account.Personal.createMainAccount(userWalletId), isKept = true),
        QuotesLoadingModel(toAccount = Account.Payment(userWalletId), isKept = false),
        QuotesLoadingModel(toAccount = Account.Money(userWalletId), isKept = false),
        QuotesLoadingModel(toAccount = Account.Prediction(userWalletId), isKept = false),
        QuotesLoadingModel(toAccount = Account.Collectibles(userWalletId), isKept = false),
    )

    data class QuotesLoadingModel(val toAccount: Account, val isKept: Boolean)
}