package com.tangem.feature.swap.domain.transfer

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.GetTronFeeNotificationShowCountUseCase
import com.tangem.domain.notifications.IncrementNotificationsShowCountUseCase
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.features.swap.SwapFeatureToggles
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferInteractorImplTest {

    private val swapFeatureToggles: SwapFeatureToggles = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk()
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    private val sendTransactionUseCase: SendTransactionUseCase = mockk()
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk()
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase = mockk()
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()
    private val tangemPayWithdrawUseCase: TangemPayWithdrawUseCase = mockk()
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase = mockk(relaxed = true)
    private val getTronFeeNotificationShowCountUseCase: GetTronFeeNotificationShowCountUseCase = mockk(relaxed = true)
    private val incrementNotificationsShowCountUseCase: IncrementNotificationsShowCountUseCase = mockk(relaxed = true)
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase = mockk()
    private val validateTransactionUseCase: ValidateTransactionUseCase = mockk()

    private val sut = SwapTransferInteractorImpl(
        swapFeatureToggles = swapFeatureToggles,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
        getFeeUseCase = getFeeUseCase,
        getFeeForGaslessUseCase = getFeeForGaslessUseCase,
        createTransferTransactionUseCase = createTransferTransactionUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
        getCurrencyCheckUseCase = getCurrencyCheckUseCase,
        isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
        tangemPayWithdrawUseCase = tangemPayWithdrawUseCase,
        getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
        getTronFeeNotificationShowCountUseCase = getTronFeeNotificationShowCountUseCase,
        incrementNotificationsShowCountUseCase = incrementNotificationsShowCountUseCase,
        getAssetRequirementsUseCase = getAssetRequirementsUseCase,
        validateTransactionUseCase = validateTransactionUseCase,
    )

    @BeforeEach
    fun setup() {
        coEvery { getAssetRequirementsUseCase(any(), any()) } returns null.right()
        coEvery { validateTransactionUseCase(any(), any(), any(), any(), any(), any()) } returns Unit.right()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // region updateTransfer

    @Test
    fun `GIVEN unparsable amount WHEN updateTransfer THEN return EmptyAmountState in transfer mode`() = runTest {
        val appCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
        )
        every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false

        val result = sut.updateTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = "abc",
            feePaidCurrencyStatus = null,
            fee = null,
        )

        assertThat(result).isInstanceOf(SwapState.EmptyAmountState::class.java)
        assertThat((result as SwapState.EmptyAmountState).isTransferMode).isTrue()
        verify { getSelectedAppCurrencyUseCase() }
        verify { getBalanceHidingSettingsUseCase.isBalanceHidden() }
        coVerify { isAccountsModeEnabledUseCase.invokeSync() }
    }

    @Test
    fun `GIVEN valid amount WHEN updateTransfer THEN return Transfer state with mirrored from-and-to swap info`() =
        runTest {
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = BigDecimal("1.6"),
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(true)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
            val currencyCheck = buildCurrencyCheck()
            coEvery {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            } returns currencyCheck
            coEvery {
                isAmountSubtractAvailableUseCase(any(), any(), any())
            } returns false.right()

            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1,5",
                feePaidCurrencyStatus = null,
                fee = null,
            )

            val expectedAmount = BigDecimal("1.5")
            val expectedFiat = BigDecimal("15.0")
            val expected = SwapState.Transfer(
                userWallet = userWallet,
                fromTokenInfo = TokenSwapInfo(
                    tokenAmount = SwapAmount(expectedAmount, FROM_DECIMALS),
                    swapCurrencyStatus = fromCurrencyStatus,
                    amountFiat = expectedFiat,
                ),
                toTokenInfo = TokenSwapInfo(
                    tokenAmount = SwapAmount(expectedAmount, TO_DECIMALS),
                    swapCurrencyStatus = toCurrencyStatus,
                    amountFiat = expectedFiat,
                ),
                cryptoCurrencyWarning = null,
                isInsufficientBalance = false,
                appCurrency = appCurrency,
                isBalanceHidden = true,
                isAccountsMode = true,
                isFeeCoverage = false,
                sendingAmount = expectedAmount,
                tronFeeNotificationShowCount = 0,
                isAmountSubtractAvailable = false,
                currencyCheck = currencyCheck,
            )
            assertThat(result).isEqualTo(expected)
            coVerify { isAccountsModeEnabledUseCase.invokeSync() }
            verify { getBalanceHidingSettingsUseCase.isBalanceHidden() }
        }

    @Test
    fun `GIVEN insufficient amount WHEN updateTransfer THEN return state with insufficient amount`() = runTest {
        val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
        val userWallet: UserWallet = mockk(relaxed = true)
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            fiatRate = BigDecimal.TEN,
            amount = BigDecimal("1.4"),
            userWallet = userWallet,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            userWallet = userWallet,
        )
        every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(true)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
        val currencyCheck = buildCurrencyCheck()
        coEvery {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns currencyCheck
        coEvery {
            isAmountSubtractAvailableUseCase(any(), any(), any())
        } returns false.right()

        val result = sut.updateTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = "1,5",
            feePaidCurrencyStatus = null,
            fee = null,
        )

        val expectedAmount = BigDecimal("1.5")
        val expectedFiat = BigDecimal("15.0")
        val expected = SwapState.Transfer(
            userWallet = userWallet,
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(expectedAmount, FROM_DECIMALS),
                swapCurrencyStatus = fromCurrencyStatus,
                amountFiat = expectedFiat,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(expectedAmount, TO_DECIMALS),
                swapCurrencyStatus = toCurrencyStatus,
                amountFiat = expectedFiat,
            ),
            cryptoCurrencyWarning = null,
            isInsufficientBalance = true,
            appCurrency = appCurrency,
            isBalanceHidden = true,
            isAccountsMode = true,
            isFeeCoverage = false,
            sendingAmount = expectedAmount,
            tronFeeNotificationShowCount = 0,
            isAmountSubtractAvailable = false,
            currencyCheck = currencyCheck,
        )
        assertThat(result).isEqualTo(expected)
        coVerify { isAccountsModeEnabledUseCase.invokeSync() }
        verify { getBalanceHidingSettingsUseCase.isBalanceHidden() }
    }

    @Test
    fun `GIVEN subtract available and fee fills the gap WHEN updateTransfer THEN isFeeCoverage is true and sendingAmount is reduced by fee`() =
        runTest {
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val balance = BigDecimal("1.5")
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = balance,
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            val feeValue = BigDecimal("0.2")
            val fee: Fee = mockk(relaxed = true) {
                every { amount.value } returns feeValue
            }
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
            coEvery {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            } returns buildCurrencyCheck()
            coEvery {
                isAmountSubtractAvailableUseCase(any(), any(), any())
            } returns true.right()

            // entered amount = full balance → balance < amount + fee, balance > fee, balance >= amount
            // → isFeeCoverage = true, sendingAmount = entered - fee (== balance - fee at max)
            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = balance.toPlainString(),
                feePaidCurrencyStatus = null,
                fee = fee,
            ) as SwapState.Transfer

            assertThat(result.isFeeCoverage).isTrue()
            assertThat(result.sendingAmount).isEqualTo(balance - feeValue)
            assertThat(result.isSendingAmountLoading).isFalse()
        }

    @Test
    fun `GIVEN subtract available and sub-max amount in coverage zone WHEN updateTransfer THEN sendingAmount is entered minus fee`() =
        runTest {
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val balance = BigDecimal("1.5")
            val feeValue = BigDecimal("0.2")
            // entered is below the balance but still within one fee of it → coverage applies, yet the
            // received amount must track the entered amount (entered - fee), not clamp to balance - fee.
            val enteredAmount = BigDecimal("1.45")
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = balance,
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            val fee: Fee = mockk(relaxed = true) {
                every { amount.value } returns feeValue
            }
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
            coEvery {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            } returns buildCurrencyCheck()
            coEvery {
                isAmountSubtractAvailableUseCase(any(), any(), any())
            } returns true.right()

            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = enteredAmount.toPlainString(),
                feePaidCurrencyStatus = null,
                fee = fee,
            ) as SwapState.Transfer

            assertThat(result.isFeeCoverage).isTrue()
            assertThat(result.sendingAmount).isEqualTo(enteredAmount - feeValue)
            // it must NOT clamp to balance - fee
            assertThat(result.sendingAmount).isNotEqualTo(balance - feeValue)
        }

    @Test
    fun `GIVEN subtract available but fee not loaded yet WHEN updateTransfer THEN isSendingAmountLoading is true`() =
        runTest {
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val balance = BigDecimal("1.5")
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = balance,
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
            coEvery {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = any(),
                )
            } returns buildCurrencyCheck()
            coEvery {
                isAmountSubtractAvailableUseCase(any(), any(), any())
            } returns true.right()

            // subtraction is possible but the fee has not loaded yet (fee = null) → the received amount
            // depends on the fee, so it can't be known yet and must be reported as loading.
            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = balance.toPlainString(),
                feePaidCurrencyStatus = null,
                fee = null,
            ) as SwapState.Transfer

            assertThat(result.isSendingAmountLoading).isTrue()
        }

    @Test
    fun `GIVEN destination address WHEN updateTransfer THEN currency check requested with recipient and isAccountFunded flows through`() =
        runTest {
            // Arrange
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = BigDecimal("1.6"),
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
                destinationAddress = DESTINATION_ADDRESS,
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
            // Stub matches only when the destination address is forwarded as recipientAddress; the
            // returned check has isAccountFunded = true (buildCurrencyCheck default).
            val fundedCheck = buildCurrencyCheck()
            coEvery {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = DESTINATION_ADDRESS,
                )
            } returns fundedCheck
            coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()

            // Act
            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1,5",
                feePaidCurrencyStatus = null,
                fee = null,
            ) as SwapState.Transfer

            // Assert
            assertThat(result.currencyCheck?.isAccountFunded).isTrue()
            coVerify {
                getCurrencyCheckUseCase(
                    userWalletId = any(),
                    currencyStatus = any(),
                    feeCurrencyStatus = any(),
                    amount = any(),
                    fee = any(),
                    feeCurrencyBalanceAfterTransaction = any(),
                    recipientAddress = DESTINATION_ADDRESS,
                )
            }
        }

    @Test
    fun `GIVEN Cardano token fee WHEN updateTransfer THEN minAdaValue flows through to Transfer state`() = runTest {
        // Arrange
        val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
        val userWallet: UserWallet = mockk(relaxed = true)
        val expectedMinAdaValue = BigDecimal("1.444443")
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            fiatRate = BigDecimal.TEN,
            amount = BigDecimal("1.6"),
            userWallet = userWallet,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            userWallet = userWallet,
        )
        val fee: Fee.CardanoToken = mockk(relaxed = true) {
            every { amount.value } returns BigDecimal("0.2")
            every { minAdaValue } returns expectedMinAdaValue
        }
        every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns buildCurrencyCheck()
        coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()

        // Act
        val result = sut.updateTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = "1,5",
            feePaidCurrencyStatus = null,
            fee = fee,
        ) as SwapState.Transfer

        // Assert
        assertThat(result.minAdaValue).isEqualTo(expectedMinAdaValue)
    }

    @Test
    fun `GIVEN fee paid in the from-currency WHEN updateTransfer THEN feeCurrencyBalanceAfterTx is balance minus amount minus fee`() =
        runTest {
            // Arrange
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val feeBalance = BigDecimal("2.0")
            val feeValue = BigDecimal("0.1")
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = feeBalance,
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            // fee is paid in the same currency as the from-token → identical currency id → the sending
            // amount is deducted from the fee balance too.
            val feePaidCurrencyStatus = buildFeeCurrencyStatus(
                currency = fromCurrencyStatus.currency,
                amount = feeBalance,
            )
            val fee: Fee = mockk(relaxed = true) { every { amount.value } returns feeValue }
            stubBaseFlows(appCurrency)
            coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()
            coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns null.right()
            val feeBalances = mutableListOf<BigDecimal?>()
            stubGetCurrencyCheckCapturingFeeBalance(feeBalances)

            // Act
            sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1.5",
                feePaidCurrencyStatus = feePaidCurrencyStatus,
                fee = fee,
            )

            // Assert
            assertThat(feeBalances.single()).isEqualTo(feeBalance - BigDecimal("1.5") - feeValue)
        }

    @Test
    fun `GIVEN fee paid in a different currency WHEN updateTransfer THEN feeCurrencyBalanceAfterTx is fee balance minus fee only`() =
        runTest {
            // Arrange
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val feeBalance = BigDecimal("2.0")
            val feeValue = BigDecimal("0.1")
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = BigDecimal("5.0"),
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            // fee is paid in a separate currency (distinct id) → the sending amount must NOT be
            // deducted from the fee balance.
            val feePaidCurrencyStatus = buildFeeCurrencyStatus(
                currency = buildDistinctCoin(),
                amount = feeBalance,
            )
            val fee: Fee = mockk(relaxed = true) { every { amount.value } returns feeValue }
            stubBaseFlows(appCurrency)
            coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()
            coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns null.right()
            val feeBalances = mutableListOf<BigDecimal?>()
            stubGetCurrencyCheckCapturingFeeBalance(feeBalances)

            // Act
            sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1.5",
                feePaidCurrencyStatus = feePaidCurrencyStatus,
                fee = fee,
            )

            // Assert
            assertThat(feeBalances.single()).isEqualTo(feeBalance - feeValue)
        }

    @Test
    fun `GIVEN no fee currency status WHEN updateTransfer THEN feeCurrencyBalanceAfterTx is null`() = runTest {
        // Arrange
        val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
        val userWallet: UserWallet = mockk(relaxed = true)
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            fiatRate = BigDecimal.TEN,
            amount = BigDecimal("2.0"),
            userWallet = userWallet,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            userWallet = userWallet,
        )
        val fee: Fee = mockk(relaxed = true) { every { amount.value } returns BigDecimal("0.1") }
        stubBaseFlows(appCurrency)
        coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()
        val feeBalances = mutableListOf<BigDecimal?>()
        stubGetCurrencyCheckCapturingFeeBalance(feeBalances)

        // Act
        sut.updateTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = "1.5",
            feePaidCurrencyStatus = null,
            fee = fee,
        )

        // Assert
        assertThat(feeBalances.single()).isNull()
    }

    @Test
    fun `GIVEN fee currency present but fee not loaded WHEN updateTransfer THEN feeCurrencyBalanceAfterTx is null`() =
        runTest {
            // Arrange
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk(relaxed = true)
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
                amount = BigDecimal("2.0"),
                userWallet = userWallet,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            val feePaidCurrencyStatus = buildFeeCurrencyStatus(
                currency = buildDistinctCoin(),
                amount = BigDecimal("2.0"),
            )
            stubBaseFlows(appCurrency)
            coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()
            coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns null.right()
            val feeBalances = mutableListOf<BigDecimal?>()
            stubGetCurrencyCheckCapturingFeeBalance(feeBalances)

            // Act: fee not loaded yet → feeValue is null → balance-after-tx cannot be computed
            sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1.5",
                feePaidCurrencyStatus = feePaidCurrencyStatus,
                fee = null,
            )

            // Assert
            assertThat(feeBalances.single()).isNull()
        }

    // endregion

    // region loadFee

    @Test
    fun `GIVEN valid amount and destination WHEN loadFee THEN return TransactionFee from use case`() = runTest {
        val userWalletId: UserWalletId = mockk()
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        val network: Network = mockk()
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            userWallet = userWallet,
            network = network,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = DESTINATION_ADDRESS,
        )
        val transactionData: TransactionData.Uncompiled = mockk()
        val transactionFee: TransactionFee = mockk()
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        } returns transactionData.right()
        coEvery {
            getFeeUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
            )
        } returns transactionFee.right()

        val result = sut.loadFee(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = BigDecimal("1.5"),
        )

        assertThat(result).isEqualTo(transactionFee.right())
        coVerify {
            getFeeUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
            )
        }
    }

    // endregion

    // region loadFeeExtended

    @Test
    fun `GIVEN valid amount and destination WHEN loadFeeExtended THEN return TransactionFeeExtended`() = runTest {
        val userWalletId: UserWalletId = mockk()
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        val network: Network = mockk()
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            userWallet = userWallet,
            network = network,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = DESTINATION_ADDRESS,
        )
        val transactionData: TransactionData.Uncompiled = mockk()
        val feeExtended: TransactionFeeExtended = mockk()
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        } returns transactionData.right()
        coEvery {
            getFeeForGaslessUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
            )
        } returns feeExtended.right()

        val result = sut.loadFeeExtended(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = BigDecimal("2.0"),
            selectedToken = null,
        )

        assertThat(result).isEqualTo(feeExtended.right())
        coVerify {
            createTransferTransactionUseCase(
                amount = any(),
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        }
        coVerify {
            getFeeForGaslessUseCase(
                userWallet = userWallet,
                network = network,
                transactionData = transactionData,
            )
        }
    }

    // endregion

    // region sendTransfer

    @Test
    fun `GIVEN missing destination WHEN sendTransfer THEN return DataError`() = runTest {
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = null,
        )

        val result = sut.sendTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            sendingAmount = BigDecimal("1.0"),
            fee = mockk(),
            transactionFeeResult = mockk(),
        )

        assertThat(result).isInstanceOf(arrow.core.Either.Left::class.java)
    }

    @Test
    fun `GIVEN coin and Loaded fee WHEN sendTransfer THEN forward tx hash from sendTransactionUseCase`() = runTest {
        val userWalletId: UserWalletId = mockk()
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        val network: Network = mockk()
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            userWallet = userWallet,
            network = network,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = DESTINATION_ADDRESS,
        )
        val fee: Fee = mockk()
        val txData: TransactionData.Uncompiled = mockk()
        val transactionFeeResult = TransactionFeeResult.Loaded(mockk())
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                fee = fee,
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        } returns txData.right()
        coEvery {
            sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
        } returns TX_HASH.right()

        val result = sut.sendTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            sendingAmount = BigDecimal("1.0"),
            fee = fee,
            transactionFeeResult = transactionFeeResult,
        )

        assertThat(result).isEqualTo(TX_HASH.right())
        coVerify {
            sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
        }
        coVerify(exactly = 0) {
            createAndSendGaslessTransactionUseCase(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN LoadedExtended fee with TokenCurrency normal fee WHEN sendTransfer THEN route via createAndSendGaslessTransactionUseCase`() =
        runTest {
            val userWalletId: UserWalletId = mockk()
            val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
            val network: Network = mockk()
            val fromCurrencyStatus = buildTokenCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                userWallet = userWallet,
                network = network,
            )
            val toCurrencyStatus = buildTokenCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                destinationAddress = DESTINATION_ADDRESS,
            )
            val fee: Fee = mockk()
            val txData: TransactionData.Uncompiled = mockk()
            val transactionFeeExtended: TransactionFeeExtended = mockk {
                every { transactionFee } returns mockk {
                    every { normal } returns mockk<Fee.Ethereum.TokenCurrency>()
                }
            }
            val transactionFeeResult = TransactionFeeResult.LoadedExtended(transactionFeeExtended)
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(),
                    fee = fee,
                    memo = null,
                    destination = DESTINATION_ADDRESS,
                    userWalletId = userWalletId,
                    network = network,
                )
            } returns txData.right()
            coEvery {
                createAndSendGaslessTransactionUseCase(
                    userWallet = userWallet,
                    transactionData = txData,
                    fee = transactionFeeExtended,
                )
            } returns TX_HASH.right()

            val result = sut.sendTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                sendingAmount = BigDecimal("1.0"),
                fee = fee,
                transactionFeeResult = transactionFeeResult,
            )

            assertThat(result).isEqualTo(TX_HASH.right())
            coVerify {
                createAndSendGaslessTransactionUseCase(
                    userWallet = userWallet,
                    transactionData = txData,
                    fee = transactionFeeExtended,
                )
            }
            coVerify(exactly = 0) {
                sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
            }
        }

    @Test
    fun `GIVEN token and Loaded fee WHEN sendTransfer THEN fall back to sendTransactionUseCase`() = runTest {
        val userWalletId: UserWalletId = mockk()
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        val network: Network = mockk()
        val fromCurrencyStatus = buildTokenCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            userWallet = userWallet,
            network = network,
        )
        val toCurrencyStatus = buildTokenCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = DESTINATION_ADDRESS,
        )
        val fee: Fee = mockk()
        val txData: TransactionData.Uncompiled = mockk()
        val transactionFeeResult = TransactionFeeResult.Loaded(mockk())
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                fee = fee,
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        } returns txData.right()
        coEvery {
            sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
        } returns TX_HASH.right()

        val result = sut.sendTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            sendingAmount = BigDecimal("1.0"),
            fee = fee,
            transactionFeeResult = transactionFeeResult,
        )

        assertThat(result).isEqualTo(TX_HASH.right())
        coVerify {
            sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
        }
        coVerify(exactly = 0) {
            createAndSendGaslessTransactionUseCase(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN LoadedExtended fee with non-TokenCurrency normal fee WHEN sendTransfer THEN fall back to sendTransactionUseCase`() =
        runTest {
            val userWalletId: UserWalletId = mockk()
            val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
            val network: Network = mockk()
            val fromCurrencyStatus = buildTokenCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                userWallet = userWallet,
                network = network,
            )
            val toCurrencyStatus = buildTokenCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                destinationAddress = DESTINATION_ADDRESS,
            )
            val fee: Fee = mockk()
            val txData: TransactionData.Uncompiled = mockk()
            val transactionFeeExtended: TransactionFeeExtended = mockk {
                every { transactionFee } returns mockk {
                    every { normal } returns mockk<Fee.Common>()
                }
            }
            val transactionFeeResult = TransactionFeeResult.LoadedExtended(transactionFeeExtended)
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(),
                    fee = fee,
                    memo = null,
                    destination = DESTINATION_ADDRESS,
                    userWalletId = userWalletId,
                    network = network,
                )
            } returns txData.right()
            coEvery {
                sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
            } returns TX_HASH.right()

            val result = sut.sendTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                sendingAmount = BigDecimal("1.0"),
                fee = fee,
                transactionFeeResult = transactionFeeResult,
            )

            assertThat(result).isEqualTo(TX_HASH.right())
            coVerify {
                sendTransactionUseCase(txData = txData, userWallet = userWallet, network = network)
            }
            coVerify(exactly = 0) {
                createAndSendGaslessTransactionUseCase(any(), any(), any())
            }
        }

    @Test
    fun `GIVEN createTransferTransactionUseCase fails WHEN sendTransfer THEN return DataError`() = runTest {
        val userWalletId: UserWalletId = mockk()
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        val network: Network = mockk()
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
            userWallet = userWallet,
            network = network,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
            destinationAddress = DESTINATION_ADDRESS,
        )
        val fee: Fee = mockk()
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                fee = fee,
                memo = null,
                destination = DESTINATION_ADDRESS,
                userWalletId = userWalletId,
                network = network,
            )
        } returns IllegalStateException("boom").left()

        val result = sut.sendTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            sendingAmount = BigDecimal("1.0"),
            fee = fee,
            transactionFeeResult = mockk(),
        )

        assertThat(result).isInstanceOf(arrow.core.Either.Left::class.java)
        val error = (result as arrow.core.Either.Left).value
        assertThat(error).isInstanceOf(SendTransactionError.DataError::class.java)
    }

    // endregion

    // region withdrawTangemPay

    @Test
    fun `GIVEN valid destination and currency id WHEN withdrawTangemPay THEN return WithdrawalResult from use case`() =
        runTest {
            val userWallet: UserWallet = mockk()
            val cryptoAmount = BigDecimal("1.5")
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                destinationAddress = DESTINATION_ADDRESS,
            )
            coEvery {
                tangemPayWithdrawUseCase(
                    userWallet = userWallet,
                    cryptoAmount = cryptoAmount,
                    cryptoCurrencyId = TO_RAW_CURRENCY_ID,
                    receiverCexAddress = DESTINATION_ADDRESS,
                )
            } returns WithdrawalResult.Success.right()

            val result = sut.withdrawTangemPay(
                userWallet = userWallet,
                cryptoAmount = cryptoAmount,
                toSwapCurrencyStatus = toCurrencyStatus,
            )

            assertThat(result).isEqualTo(WithdrawalResult.Success.right())
            coVerify {
                tangemPayWithdrawUseCase(
                    userWallet = userWallet,
                    cryptoAmount = cryptoAmount,
                    cryptoCurrencyId = TO_RAW_CURRENCY_ID,
                    receiverCexAddress = DESTINATION_ADDRESS,
                )
            }
        }

    // endregion

    // region shouldTransferInsteadOfSwap

    @Test
    fun `GIVEN feature toggle disabled WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns false

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isFalse()
        verify { swapFeatureToggles.isSwapSwitchToTransferEnabled }
    }

    @Test
    fun `GIVEN both coins on the same network WHEN shouldTransferInsteadOfSwap THEN return true`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN coins on different networks WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = POLYGON),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN tokens with same network and same contract WHEN shouldTransferInsteadOfSwap THEN return true`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN tokens with same network but different contract WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDC_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN tokens with same contract but different network WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = POLYGON, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN coin from and token to WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN token from and coin to WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isFalse()
    }

    // endregion

    // region helpers

    private fun buildCoin(networkRawId: String): CryptoCurrency.Coin {
        val network: Network = mockk { every { rawId } returns networkRawId }
        return mockk {
            every { this@mockk.network } returns network
        }
    }

    private fun buildToken(networkRawId: String, contractAddress: String): CryptoCurrency.Token {
        val network: Network = mockk { every { rawId } returns networkRawId }
        return mockk {
            every { this@mockk.network } returns network
            every { this@mockk.contractAddress } returns contractAddress
        }
    }

    private fun stubBaseFlows(appCurrency: AppCurrency) {
        every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
    }

    private fun stubGetCurrencyCheckCapturingFeeBalance(feeBalances: MutableList<BigDecimal?>) {
        coEvery {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = captureNullable(feeBalances),
                recipientAddress = any(),
            )
        } returns buildCurrencyCheck()
    }

    private fun buildFeeCurrencyStatus(currency: CryptoCurrency, amount: BigDecimal): CryptoCurrencyStatus {
        val loadedValue: CryptoCurrencyStatus.Loaded = mockk {
            every { this@mockk.amount } returns amount
        }
        return mockk {
            every { this@mockk.value } returns loadedValue
            every { this@mockk.currency } returns currency
        }
    }

    private fun buildDistinctCoin(): CryptoCurrency.Coin {
        val currencyId: CryptoCurrency.ID = mockk()
        return mockk {
            every { this@mockk.id } returns currencyId
            every { this@mockk.network } returns mockk()
        }
    }

    private fun buildCurrencyCheck(
        existentialDeposit: BigDecimal? = null,
        dustValue: BigDecimal? = null,
        reserveAmount: BigDecimal? = null,
    ): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = dustValue,
        reserveAmount = reserveAmount,
        minimumSendAmount = null,
        existentialDeposit = existentialDeposit,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
    )

    private fun buildCurrencyStatus(
        rawCurrencyId: CryptoCurrency.RawID?,
        decimals: Int,
        fiatRate: BigDecimal = BigDecimal.ZERO,
        amount: BigDecimal = BigDecimal.ZERO,
        userWallet: UserWallet = mockk(relaxed = true),
        destinationAddress: String? = null,
        symbol: String = "ETH",
        network: Network = mockk(),
    ): SwapCurrencyStatus {
        val currencyId: CryptoCurrency.ID = mockk {
            every { this@mockk.rawCurrencyId } returns rawCurrencyId
        }
        val currency: CryptoCurrency.Coin = mockk {
            every { this@mockk.id } returns currencyId
            every { this@mockk.decimals } returns decimals
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
        }
        val networkAddress = destinationAddress?.let {
            NetworkAddress.Single(NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Primary))
        }
        val currencyValue: CryptoCurrencyStatus.Value = mockk {
            every { this@mockk.fiatRate } returns fiatRate
            every { this@mockk.networkAddress } returns networkAddress
            every { this@mockk.yieldSupplyStatus } returns null
            every { this@mockk.amount } returns amount
        }
        val status: CryptoCurrencyStatus = mockk {
            every { this@mockk.value } returns currencyValue
            every { this@mockk.currency } returns currency
        }
        return mockk {
            every { this@mockk.currency } returns currency
            every { this@mockk.userWallet } returns userWallet
            every { this@mockk.userWalletId } answers { userWallet.walletId }
            every { this@mockk.status } returns status
        }
    }

    @Suppress("LongParameterList")
    private fun buildTokenCurrencyStatus(
        rawCurrencyId: CryptoCurrency.RawID?,
        decimals: Int,
        userWallet: UserWallet = mockk(relaxed = true),
        destinationAddress: String? = null,
        symbol: String = "USDT",
        network: Network = mockk(),
    ): SwapCurrencyStatus {
        val currencyId: CryptoCurrency.ID = mockk {
            every { this@mockk.rawCurrencyId } returns rawCurrencyId
        }
        val currency: CryptoCurrency.Token = mockk {
            every { this@mockk.id } returns currencyId
            every { this@mockk.decimals } returns decimals
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.contractAddress } returns CONTRACT_ADDRESS
        }
        val networkAddress = destinationAddress?.let {
            NetworkAddress.Single(NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Primary))
        }
        val currencyValue: CryptoCurrencyStatus.Value = mockk {
            every { this@mockk.fiatRate } returns BigDecimal.ZERO
            every { this@mockk.networkAddress } returns networkAddress
            every { this@mockk.yieldSupplyStatus } returns null
            every { this@mockk.amount } returns BigDecimal.ZERO
        }
        val status: CryptoCurrencyStatus = mockk {
            every { this@mockk.value } returns currencyValue
            every { this@mockk.currency } returns currency
        }
        return mockk {
            every { this@mockk.currency } returns currency
            every { this@mockk.userWallet } returns userWallet
            every { this@mockk.status } returns status
        }
    }

    // endregion

    private companion object {
        const val ETHEREUM = "ethereum"
        const val POLYGON = "polygon"
        const val USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val USDC_CONTRACT = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
        const val DESTINATION_ADDRESS = "0xdEaDBeEf00000000000000000000000000000001"
        const val CONTRACT_ADDRESS = "0xCONTRACT00000000000000000000000000000001"
        const val TX_HASH = "0xabc123"
        const val FROM_DECIMALS = 18
        const val TO_DECIMALS = 6
        val FROM_RAW_CURRENCY_ID = CryptoCurrency.RawID(value = "eth")
        val TO_RAW_CURRENCY_ID = CryptoCurrency.RawID(value = "matic")
    }
}