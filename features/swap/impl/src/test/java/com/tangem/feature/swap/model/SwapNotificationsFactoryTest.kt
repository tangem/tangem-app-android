package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTxType
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.RateType
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for [SwapNotificationsFactory.getConfirmationStateNotifications], focused on the
 * `UnableToCoverFeeWarning` gating ([REDACTED_TASK_KEY]):
 *
 * | flow                          | gasless network | expected for InsufficientFee   |
 * |-------------------------------|-----------------|--------------------------------|
 * | CEX                           | no              | warning shown (the bug fix)    |
 * | CEX                           | yes             | suppressed (fee → token)      |
 * | DEX (txType=null)             | yes             | warning shown (DEX unchanged)  |
 * | DEX + txType=SEND (CEX-like)  | yes             | suppressed like a real CEX     |
 * | DEX + txType=SEND (CEX-like)  | no              | warning shown                  |
 */
internal class SwapNotificationsFactoryTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = Provider { AppCurrency.Default }
    private val appRouter: AppRouter = mockk()

    private val factory = SwapNotificationsFactory(
        actions = actions,
        isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        appCurrencyProvider = appCurrencyProvider,
    )

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(isGaslessFeeSupportedForNetwork, appRouter)
        every { appRouter.stack } returns emptyList()
    }

    @Test
    fun `GIVEN CEX and no gasless support WHEN insufficient fee THEN cover fee warning shown`() {
        // Arrange
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuotesLoadedState(providerType = ExchangeProviderType.CEX)

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            appRouter = appRouter,
        )

        // Assert
        val warning = notifications.filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>().single()
        assertThat(warning.currencyName).isEqualTo("Ethereum")
        assertThat(warning.currencySymbol).isEqualTo("ETH")
    }

    @Test
    fun `GIVEN CEX and gasless support WHEN insufficient fee THEN cover fee warning suppressed`() {
        // Arrange
        every { isGaslessFeeSupportedForNetwork(any()) } returns true
        val quoteModel = buildQuotesLoadedState(providerType = ExchangeProviderType.CEX)

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            appRouter = appRouter,
        )

        // Assert
        assertThat(notifications.filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>()).isEmpty()
    }

    @Test
    fun `GIVEN DEX and gasless support WHEN insufficient fee THEN cover fee warning shown`() {
        // Arrange
        every { isGaslessFeeSupportedForNetwork(any()) } returns true
        val quoteModel = buildQuotesLoadedState(providerType = ExchangeProviderType.DEX)

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            appRouter = appRouter,
        )

        // Assert
        assertThat(notifications.filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>()).hasSize(1)
    }

    @Test
    fun `GIVEN DEX with SEND txType and gasless support WHEN insufficient fee THEN warning suppressed like CEX`() {
        // Arrange
        every { isGaslessFeeSupportedForNetwork(any()) } returns true
        val quoteModel = buildQuotesLoadedState(
            providerType = ExchangeProviderType.DEX,
            txType = ExpressTxType.SEND,
        )

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            appRouter = appRouter,
        )

        // Assert
        assertThat(notifications.filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>()).isEmpty()
    }

    @Test
    fun `GIVEN DEX with SEND txType and no gasless support WHEN insufficient fee THEN warning shown`() {
        // Arrange
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuotesLoadedState(
            providerType = ExchangeProviderType.DEX,
            txType = ExpressTxType.SEND,
        )

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            appRouter = appRouter,
        )

        // Assert
        assertThat(notifications.filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>()).hasSize(1)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildEthNetwork(): Network = mockk(relaxed = true) {
        every { rawId } returns "ethereum"
        every { name } returns "Ethereum"
        every { currencySymbol } returns "ETH"
    }

    /** Token from-currency, so the fee is paid in a different (native coin) currency. */
    private fun buildTokenFromStatus(): SwapCurrencyStatus {
        val network = buildEthNetwork()
        val currency = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { this@mockk.network } returns network
            every { symbol } returns "USDT"
            every { name } returns "Tether"
            every { decimals } returns 6
        }
        val statusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("100")
            every { pendingTransactions } returns emptySet()
        }
        return SwapCurrencyStatus(
            userWallet = userWallet,
            status = CryptoCurrencyStatus(currency = currency, value = statusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
    }

    private fun buildCoinFeeStatus(): CryptoCurrencyStatus {
        val network = buildEthNetwork()
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { this@mockk.network } returns network
            every { symbol } returns "ETH"
            every { name } returns "Ethereum"
            every { decimals } returns 18
        }
        val statusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal.ZERO
        }
        return CryptoCurrencyStatus(currency = currency, value = statusValue)
    }

    private fun buildQuotesLoadedState(
        providerType: ExchangeProviderType,
        txType: ExpressTxType? = null,
    ): SwapState.QuotesLoadedState {
        val toStatusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("1")
        }
        val toCurrency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { network } returns buildEthNetwork()
            every { symbol } returns "BTC"
            every { decimals } returns 8
        }
        val toSwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = CryptoCurrencyStatus(currency = toCurrency, value = toStatusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("50"), 6),
                swapCurrencyStatus = buildTokenFromStatus(),
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 8),
                swapCurrencyStatus = toSwapCurrencyStatus,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.InsufficientFee(
                    feeCurrencyName = "Ethereum",
                    feeCurrencySymbol = "ETH",
                ),
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildProvider(providerType),
            txType = txType,
        )
    }

    private fun buildProvider(type: ExchangeProviderType): SwapProvider = SwapProvider(
        providerId = "p",
        rateTypes = listOf(RateType.FLOAT),
        name = "Provider",
        type = type,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        isRecommended = false,
        slippage = null,
        isExtraIdSupported = false,
    )
}