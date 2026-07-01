package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StateBuilderUpdateSwapAmountTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val swapFeatureToggles: SwapFeatureToggles = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk()

    private val appCurrency = AppCurrency.Default

    private val userWalletId = UserWalletId("aabbccdd")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    private lateinit var sut: StateBuilder

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns appCurrency
        every { isAccountsModeProvider() } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            swapFeatureToggles = swapFeatureToggles,
            appRouter = appRouter,
        )
    }

    private fun readyState(fromStatus: SwapCurrencyStatus): SwapStateHolder = sut.createInitialReadyState(
        uiStateHolder = sut.createInitialLoadingState(),
        emptyAmountState = SwapState.EmptyAmountState(zeroAmountEquivalent = stringReference("$0.00")),
        fromSwapCurrencyStatus = fromStatus,
        toSwapCurrencyStatus = buildSwapCurrencyStatus(coldWallet),
    )

    private val SwapStateHolder.sendCard: SwapCardState.SwapCardData
        get() = sendCardData as SwapCardState.SwapCardData

    @Test
    fun `GIVEN crypto input WHEN updateSwapAmount THEN field shows crypto value and equivalent is fiat`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet) // fiatRate = 2000
        val base = readyState(fromStatus)

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = false,
        )

        // Assert
        val field = result.sendCard.amountField!!
        assertThat(field.value).isEqualTo("0.5")
        assertThat(field.isFiatValue).isFalse()
        assertThat(field.cryptoAmount.value).isEqualTo(BigDecimal("0.5"))
        assertThat(field.isValuePasted).isFalse()
        // 0.5 * 2000 = 1000 fiat
        assertThat(result.sendCard.amountEquivalent).isEqualTo(
            stringReference(
                BigDecimal("1000.00").format {
                    fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                },
            ),
        )
    }

    @Test
    fun `GIVEN fiat input WHEN updateSwapAmount THEN field marked fiat and equivalent is crypto`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet) // fiatRate = 2000
        val base = readyState(fromStatus)

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "0.5", // crypto authoritative amount
            fieldValue = "1000", // displayed fiat value
            isFiatValue = true,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = false,
        )

        // Assert
        val field = result.sendCard.amountField!!
        assertThat(field.value).isEqualTo("1000")
        assertThat(field.isFiatValue).isTrue()
        assertThat(field.cryptoAmount.value).isEqualTo(BigDecimal("0.5"))
        // equivalent line shows the crypto amount when entering fiat
        assertThat(result.sendCard.amountEquivalent).isEqualTo(
            stringReference(BigDecimal("0.5").format { crypto(fromStatus.currency) }),
        )
    }

    @Test
    fun `GIVEN fiat input but fiat rate unavailable WHEN updateSwapAmount THEN field falls back to crypto display`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatusNoFiatRate(coldWallet)
        val base = readyState(buildSwapCurrencyStatus(coldWallet))

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = true,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = false,
        )

        // Assert
        val field = result.sendCard.amountField!!
        // isFiatValue collapses to false because the rate is unavailable
        assertThat(field.isFiatValue).isFalse()
        assertThat(field.isFiatUnavailable).isTrue()
    }

    @Test
    fun `GIVEN amount below min WHEN updateSwapAmount THEN send card reports WrongAmount error`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val base = readyState(fromStatus)

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = BigDecimal("1"),
            isPastedAmount = false,
        )

        // Assert
        val inputtable = result.sendCard.type as TransactionCardType.Inputtable
        assertThat(inputtable.inputError).isEqualTo(TransactionCardType.InputError.WrongAmount)
    }

    @Test
    fun `GIVEN amount at or above min WHEN updateSwapAmount THEN send card has no input error`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val base = readyState(fromStatus)

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "2",
            fieldValue = "2",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = BigDecimal("1"),
            isPastedAmount = false,
        )

        // Assert
        val inputtable = result.sendCard.type as TransactionCardType.Inputtable
        assertThat(inputtable.inputError).isEqualTo(TransactionCardType.InputError.Empty)
    }

    @Test
    fun `GIVEN pasted amount WHEN updateSwapAmount THEN field flags value as pasted`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val base = readyState(fromStatus)

        // Act
        val result = sut.updateSwapAmount(
            uiState = base,
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = true,
        )

        // Assert
        assertThat(result.sendCard.amountField!!.isValuePasted).isTrue()
    }

    @Test
    fun `GIVEN send card is not SwapCardData WHEN updateSwapAmount THEN uiState returned unchanged`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val loadingState = sut.createInitialLoadingState() // send card is Empty, not SwapCardData

        // Act
        val result = sut.updateSwapAmount(
            uiState = loadingState,
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = false,
        )

        // Assert
        assertThat(result).isSameInstanceAs(loadingState)
    }

    @Test
    fun `GIVEN ready state WHEN createQuotesEmptyAmountState THEN receive amount resets to zero and button disabled`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val base = sut.updateSwapAmount(
            uiState = readyState(fromStatus),
            amountRaw = "0.5",
            fieldValue = "0.5",
            isFiatValue = false,
            fromSwapCurrencyStatus = fromStatus,
            minTxAmount = null,
            isPastedAmount = false,
        )
        val zeroEquivalent = stringReference("$0.00")

        // Act
        val result = sut.createQuotesEmptyAmountState(
            uiStateHolder = base,
            emptyAmountState = SwapState.EmptyAmountState(zeroAmountEquivalent = zeroEquivalent),
            fromSwapCurrencyStatus = fromStatus,
        )

        // Assert
        val receiveCard = result.receiveCardData as SwapCardState.SwapCardData
        assertThat(receiveCard.amountField!!.value).isEqualTo("0")
        assertThat(result.sendCard.amountEquivalent).isEqualTo(zeroEquivalent)
        assertThat(receiveCard.amountEquivalent).isEqualTo(zeroEquivalent)
        assertThat(result.swapButton.isEnabled).isFalse()
        assertThat(result.isInsufficientFunds).isFalse()
        assertThat(result.notifications).isEmpty()
    }

    @Test
    fun `GIVEN receive card is not SwapCardData WHEN createQuotesEmptyAmountState THEN uiState returned unchanged`() {
        // Arrange
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        // createInitialReadyState builds SwapCardData send + SwapCardData receive, but loading state has Empty cards
        val loadingState = sut.createInitialLoadingState()

        // Act
        val result = sut.createQuotesEmptyAmountState(
            uiStateHolder = loadingState,
            emptyAmountState = SwapState.EmptyAmountState(zeroAmountEquivalent = stringReference("$0.00")),
            fromSwapCurrencyStatus = fromStatus,
        )

        // Assert
        assertThat(result).isSameInstanceAs(loadingState)
    }

    private fun buildSwapCurrencyStatusNoFiatRate(userWallet: UserWallet): SwapCurrencyStatus {
        val status = buildSwapCurrencyStatus(userWallet)
        every { status.status.value.fiatRate } returns null
        return status
    }
}