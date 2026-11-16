package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.IsEvmGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
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
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/** A sponsored swap must not ask the user to top up the native coin the sponsor pays the fee in. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapNotificationsFactorySponsoredFeeTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isEvmGaslessFeeSupportedForNetwork: IsEvmGaslessFeeSupportedForNetwork = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = Provider { AppCurrency.Default }

    private val factory = SwapNotificationsFactory(
        actions = actions,
        isEvmGaslessFeeSupportedForNetwork = isEvmGaslessFeeSupportedForNetwork,
        appCurrencyProvider = appCurrencyProvider,
    )

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(isEvmGaslessFeeSupportedForNetwork)
        every { isEvmGaslessFeeSupportedForNetwork(any()) } returns true
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN balance status WHEN getConfirmationStateNotifications THEN UnableToCoverFee only when not sponsored`(
        model: WarningModel,
    ) {
        // Arrange
        val quoteModel = buildQuotesLoadedState(balanceStatus = model.balanceStatus)

        // Act
        val notifications = factory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = buildCoinFeeStatus(),
            swapFee = null,
            feeError = null,
            isFeeCurrencyInStack = false,
        )

        // Assert
        val hasWarning = notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }
        assertThat(hasWarning).isEqualTo(model.hasUnableToCoverFeeWarning)
    }

    data class WarningModel(val balanceStatus: SwapBalanceStatus, val hasUnableToCoverFeeWarning: Boolean)

    private fun provideTestModels() = listOf(
        WarningModel(
            balanceStatus = SwapBalanceStatus.SponsoredFee(fee = mockk<TransactionFeeExtended>()),
            hasUnableToCoverFeeWarning = false,
        ),
        WarningModel(
            balanceStatus = SwapBalanceStatus.InsufficientFee(feeCurrencyName = "Solana", feeCurrencySymbol = "SOL"),
            hasUnableToCoverFeeWarning = true,
        ),
    )

    private fun buildSolanaNetwork(): Network = mockk(relaxed = true) {
        every { rawId } returns "solana"
        every { name } returns "Solana"
        every { currencySymbol } returns "SOL"
    }

    private fun buildTokenFromStatus(): SwapCurrencyStatus {
        val network = buildSolanaNetwork()
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
            account = Account.Personal.createMainAccount(userWalletId),
        )
    }

    private fun buildCoinFeeStatus(): CryptoCurrencyStatus {
        val network = buildSolanaNetwork()
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { this@mockk.network } returns network
            every { symbol } returns "SOL"
            every { name } returns "Solana"
            every { decimals } returns 9
        }
        val statusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal.ZERO
        }
        return CryptoCurrencyStatus(currency = currency, value = statusValue)
    }

    private fun buildQuotesLoadedState(balanceStatus: SwapBalanceStatus): SwapState.QuotesLoadedState {
        val toStatusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("1")
        }
        val toCurrency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { network } returns buildSolanaNetwork()
            every { symbol } returns "BTC"
            every { decimals } returns 8
        }
        val toSwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = CryptoCurrencyStatus(currency = toCurrency, value = toStatusValue),
            account = Account.Personal.createMainAccount(userWalletId),
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
                balanceStatus = balanceStatus,
                hasOutgoingTransaction = false,
            ),
            permissionState = PermissionDataState.Empty,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildProvider(),
            txType = null,
            isRestricted = false,
        )
    }

    private fun buildProvider(): SwapProvider = SwapProvider(
        providerId = "p",
        rateTypes = listOf(RateType.FLOAT),
        name = "Provider",
        type = ExchangeProviderType.DEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        isRecommended = false,
        slippage = null,
        isExtraIdSupported = false,
    )
}