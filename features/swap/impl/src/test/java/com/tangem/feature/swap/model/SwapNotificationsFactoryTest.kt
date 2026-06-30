package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.UiActions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Regression coverage for the duplicate "Invalid amount" (MinimumAmountError) banner in the regular
 * (quotes-loaded) swap flow. For a BTC dust-change amount, two independent dust checks in
 * [SwapNotificationsFactory.getConfirmationStateNotifications] →
 * [SwapNotificationsFactory.maybeAddDomainWarnings] both add an identical MinimumAmountError:
 * the SDK validation ([BlockchainSdkError.TransactionDustChangeError]) and the manual `checkDustLimits`
 * change-below-dust branch. The factory must collapse the duplicate so only one banner is shown.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapNotificationsFactoryTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)

    private val sut = SwapNotificationsFactory(
        actions = actions,
        isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
    )

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @Test
    fun `GIVEN dust-change validation error and change below dust WHEN getConfirmationStateNotifications THEN single MinimumAmountError`() =
        runTest {
            // Arrange — balance 1.0, sending 0.99, dust 0.02 → leftover change 0.01 (< dust) triggers the
            // manual checkDustLimits path, while validationResult = TransactionDustChangeError triggers the SDK
            // path. Both add an identical MinimumAmountError; the fix must collapse them into one.
            val quoteModel = buildQuotesLoadedState(
                balance = BigDecimal("1.0"),
                amount = BigDecimal("0.99"),
                dustValue = BigDecimal("0.02"),
                validationResult = BlockchainSdkError.TransactionDustChangeError,
            )

            // Act
            val result = sut.getConfirmationStateNotifications(
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapFee = null,
                feeError = null,
                appRouter = appRouter,
            )

            // Assert
            assertThat(result.filterIsInstance<NotificationUM.Error.MinimumAmountError>()).hasSize(1)
        }

    @Test
    fun `GIVEN manual dust limit only and no validation error WHEN getConfirmationStateNotifications THEN single MinimumAmountError`() =
        runTest {
            // Arrange — same change-below-dust condition but no SDK validation error: only the manual path fires.
            val quoteModel = buildQuotesLoadedState(
                balance = BigDecimal("1.0"),
                amount = BigDecimal("0.99"),
                dustValue = BigDecimal("0.02"),
                validationResult = null,
            )

            // Act
            val result = sut.getConfirmationStateNotifications(
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapFee = null,
                feeError = null,
                appRouter = appRouter,
            )

            // Assert
            assertThat(result.filterIsInstance<NotificationUM.Error.MinimumAmountError>()).hasSize(1)
        }

    @Test
    fun `GIVEN no dust value and no validation error WHEN getConfirmationStateNotifications THEN no MinimumAmountError`() =
        runTest {
            // Arrange — comfortable amount, no dust value, no validation error.
            val quoteModel = buildQuotesLoadedState(
                balance = BigDecimal("1.0"),
                amount = BigDecimal("0.5"),
                dustValue = null,
                validationResult = null,
            )

            // Act
            val result = sut.getConfirmationStateNotifications(
                quoteModel = quoteModel,
                feeCryptoCurrencyStatus = null,
                swapFee = null,
                feeError = null,
                appRouter = appRouter,
            )

            // Assert
            assertThat(result.filterIsInstance<NotificationUM.Error.MinimumAmountError>()).isEmpty()
        }

    private fun buildQuotesLoadedState(
        balance: BigDecimal,
        amount: BigDecimal,
        dustValue: BigDecimal?,
        validationResult: Throwable?,
    ): SwapState.QuotesLoadedState {
        val fromStatus = buildCoinStatus(balance = balance)
        val toStatus = buildCoinStatus(balance = BigDecimal("1.0"))
        return SwapState.QuotesLoadedState(
            fromTokenInfo = buildTokenInfo(swapCurrencyStatus = fromStatus, amount = amount),
            toTokenInfo = buildTokenInfo(swapCurrencyStatus = toStatus, amount = BigDecimal("1.0")),
            swapProvider = buildProvider(),
            priceImpact = PriceImpact.Empty,
            currencyCheck = buildCurrencyCheck(dustValue = dustValue),
            validationResult = validationResult,
            minAdaValue = null,
        )
    }

    private fun buildTokenInfo(swapCurrencyStatus: SwapCurrencyStatus, amount: BigDecimal): TokenSwapInfo =
        TokenSwapInfo(
            tokenAmount = SwapAmount(value = amount, decimals = swapCurrencyStatus.currency.decimals),
            amountFiat = amount * BigDecimal("2000"),
            swapCurrencyStatus = swapCurrencyStatus,
        )

    private fun buildCurrencyCheck(dustValue: BigDecimal?): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = dustValue,
        reserveAmount = null,
        minimumSendAmount = null,
        existentialDeposit = null,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
    )

    private fun buildCoinStatus(balance: BigDecimal): SwapCurrencyStatus {
        val coin = buildCoin()
        val statusValue: CryptoCurrencyStatus.Loaded = mockk(relaxed = true) {
            every { amount } returns balance
        }
        return SwapCurrencyStatus(
            userWallet = coldWallet,
            status = CryptoCurrencyStatus(currency = coin, value = statusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
    }

    private fun buildCoin(): CryptoCurrency.Coin = mockk(relaxed = true) {
        every { id } returns mockk(relaxed = true)
        every { network } returns mockk(relaxed = true) {
            every { rawId } returns "bitcoin"
            every { name } returns "Bitcoin"
            every { currencySymbol } returns "BTC"
        }
        every { name } returns "Bitcoin"
        every { symbol } returns "BTC"
        every { decimals } returns 8
    }

    private fun buildProvider(type: ExchangeProviderType = ExchangeProviderType.CEX): SwapProvider =
        mockk(relaxed = true) {
            every { this@mockk.type } returns type
        }
}