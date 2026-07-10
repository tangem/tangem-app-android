package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for the [StateBuilder.getSwapButtonEnabled] path as exposed via
 * [StateBuilder.createQuotesLoadedState].
 *
 * The change under test:
 *   val isSwapTxReady = isTangemPayWithdrawal || swapFee != null
 *
 * Truth table asserted here:
 * | isTangemPay | swapFee | blocking notification | expected isEnabled |
 * |-------------|---------|----------------------|--------------------|
 * | true        | null    | none                 | true               |
 * | true        | null    | present              | false              |
 * | false       | null    | none                 | false              |
 * | false       | non-null| none                 | true               |
 * | false       | non-null| present              | false              |
 */
@DisplayName("StateBuilder — swap button enabled logic (isTangemPayWithdrawal gate)")
internal class StateBuilderSwapButtonTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val appRouter: AppRouter = mockk()

    private lateinit var sut: StateBuilder

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns AppCurrency.Default
        every { isAccountsModeProvider() } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            appRouter = appRouter,
        )
    }

    @Nested
    @DisplayName("Tangem Pay withdrawal (Payment account)")
    inner class `Tangem Pay withdrawal` {

        @Test
        @DisplayName("should enable swap button when Payment account, swapFee is null, and no blocking notifications")
        fun `should enable swap button when Payment account and swapFee null and no blocking notifications`() {
            val paymentAccount = Account.Payment(userWalletId)
            val state = buildQuotesLoadedStateFor(
                account = paymentAccount,
                hasOutgoingTransaction = false,
                permissionState = PermissionDataState.Empty,
            )
            val baseHolder = buildInputtableHolder()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseHolder,
                quoteModel = state,
                feeCryptoCurrencyStatus = null,
                swapProvider = buildProvider(ExchangeProviderType.CEX),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                swapFee = null,
                feeError = null,
                isHighNetworkFee = false,
            )

            assertThat(result.swapButton.isEnabled).isTrue()
        }

        @Test
        @DisplayName("should disable swap button when Payment account, swapFee is null, but a blocking notification is present")
        fun `should disable swap button when Payment account and swapFee null and blocking notification`() {
            val paymentAccount = Account.Payment(userWalletId)
            val state = buildQuotesLoadedStateFor(
                account = paymentAccount,
                // hasOutgoingTransaction=true produces a SwapNotificationUM.Error which blocks the button
                hasOutgoingTransaction = true,
                permissionState = PermissionDataState.Empty,
            )
            val baseHolder = buildInputtableHolder()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseHolder,
                quoteModel = state,
                feeCryptoCurrencyStatus = null,
                swapProvider = buildProvider(ExchangeProviderType.CEX),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                swapFee = null,
                feeError = null,
                isHighNetworkFee = false,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }
    }

    @Nested
    @DisplayName("Non-Pay account (CryptoPortfolio)")
    inner class `Non-Pay account` {

        @Test
        @DisplayName("should disable swap button when CryptoPortfolio account and swapFee is null")
        fun `should disable swap button when CryptoPortfolio account and swapFee null`() {
            val cryptoAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
            val state = buildQuotesLoadedStateFor(
                account = cryptoAccount,
                hasOutgoingTransaction = false,
                permissionState = PermissionDataState.Empty,
            )
            val baseHolder = buildInputtableHolder()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseHolder,
                quoteModel = state,
                feeCryptoCurrencyStatus = null,
                swapProvider = buildProvider(ExchangeProviderType.CEX),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                swapFee = null,
                feeError = null,
                isHighNetworkFee = false,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }

        @Test
        @DisplayName("should enable swap button when CryptoPortfolio account, swapFee is non-null, and no blocking notifications")
        fun `should enable swap button when CryptoPortfolio account and swapFee non-null and no blocking notifications`() {
            val cryptoAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
            val state = buildQuotesLoadedStateFor(
                account = cryptoAccount,
                hasOutgoingTransaction = false,
                permissionState = PermissionDataState.Empty,
            )
            val baseHolder = buildInputtableHolder()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseHolder,
                quoteModel = state,
                feeCryptoCurrencyStatus = null,
                swapProvider = buildProvider(ExchangeProviderType.CEX),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                swapFee = buildSwapFee(),
                feeError = null,
                isHighNetworkFee = false,
            )

            assertThat(result.swapButton.isEnabled).isTrue()
        }

        @Test
        @DisplayName("should disable swap button when CryptoPortfolio account, swapFee is non-null, but a blocking notification is present")
        fun `should disable swap button when CryptoPortfolio account and swapFee non-null and blocking notification`() {
            val cryptoAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
            val state = buildQuotesLoadedStateFor(
                account = cryptoAccount,
                // PermissionRequired triggers SwapNotificationUM.Info.PermissionNeeded — in the blocking list
                hasOutgoingTransaction = false,
                permissionState = PermissionDataState.PermissionRequired(
                    isResetApproval = false,
                    spenderAddress = "0xspender",
                ),
            )
            val baseHolder = buildInputtableHolder()

            val result = sut.createQuotesLoadedState(
                uiStateHolder = baseHolder,
                quoteModel = state,
                feeCryptoCurrencyStatus = null,
                swapProvider = buildProvider(ExchangeProviderType.CEX),
                additionalBadge = ProviderState.AdditionalBadge.Empty,
                swapFee = buildSwapFee(),
                feeError = null,
                isHighNetworkFee = false,
            )

            assertThat(result.swapButton.isEnabled).isFalse()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a [SwapStateHolder] whose send/receive cards are [SwapCardState.SwapCardData] with
     * [TransactionCardType.Inputtable] type — required by [StateBuilder.createQuotesLoadedState].
     */
    private fun buildInputtableHolder(): SwapStateHolder {
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val toStatus = buildSwapCurrencyStatus(coldWallet)
        val emptyAmountState = SwapState.EmptyAmountState(stringReference("$0.00"))
        val loading = sut.createInitialLoadingState()
        return sut.createInitialReadyState(
            uiStateHolder = loading,
            emptyAmountState = emptyAmountState,
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
        )
    }

    /**
     * Builds a minimal [SwapState.QuotesLoadedState] with the given [account] on the from-currency
     * and configurable notification triggers.
     *
     * @param hasOutgoingTransaction when true, [SwapNotificationsFactory] adds a
     *   [SwapNotificationUM.Error.TransactionInProgressWarning] — a blocking Error notification.
     * @param permissionState when [PermissionDataState.PermissionRequired], adds a
     *   [SwapNotificationUM.Info.PermissionNeeded] — also in the blocking list.
     */
    private fun buildQuotesLoadedStateFor(
        account: Account,
        hasOutgoingTransaction: Boolean,
        permissionState: PermissionDataState,
    ): SwapState.QuotesLoadedState {
        val networkRawId = Blockchain.Ethereum.toNetworkId()

        val networkId = mockk<Network.ID>(relaxed = true) {
            every { rawId } returns Network.RawID(networkRawId)
        }
        val network = mockk<Network>(relaxed = true) {
            every { rawId } returns networkRawId
            every { id } returns networkId
            every { currencySymbol } returns "ETH"
            every { name } returns "Ethereum"
        }
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { this@mockk.network } returns network
            every { this@mockk.symbol } returns "ETH"
            every { this@mockk.decimals } returns 18
        }
        val networkAddress = mockk<NetworkAddress>(relaxed = true) {
            every { defaultAddress } returns NetworkAddress.Address(
                value = "0xTest",
                type = NetworkAddress.Address.Type.Primary,
            )
        }
        val statusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("1")
            every { this@mockk.networkAddress } returns networkAddress
            every { pendingTransactions } returns emptySet()
        }
        val cryptoCurrencyStatus = CryptoCurrencyStatus(currency = currency, value = statusValue)

        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }

        val fromSwapCurrencyStatus = SwapCurrencyStatus(
            userWallet = userWallet,
            status = cryptoCurrencyStatus,
            account = account,
        )
        val toSwapCurrencyStatus = buildSwapCurrencyStatusWithCryptoPortfolio(coldWallet)

        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                swapCurrencyStatus = fromSwapCurrencyStatus,
                amountFiat = BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = SwapAmount(BigDecimal("0.5"), 18),
                swapCurrencyStatus = toSwapCurrencyStatus,
                amountFiat = BigDecimal.ZERO,
            ),
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.Sufficient,
                hasOutgoingTransaction = hasOutgoingTransaction,
            ),
            permissionState = permissionState,
            swapDataModel = null,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildProvider(ExchangeProviderType.CEX),
        )
    }

    private fun buildSwapCurrencyStatusWithCryptoPortfolio(userWallet: UserWallet): SwapCurrencyStatus {
        val walletId = userWallet.walletId
        val account = Account.CryptoPortfolio.createMainAccount(walletId)
        val currency: CryptoCurrency = mockk(relaxed = true) {
            every { symbol } returns "BTC"
            every { decimals } returns 8
            every { network } returns mockk(relaxed = true) {
                every { id } returns mockk(relaxed = true)
                every { name } returns "Bitcoin"
                every { currencySymbol } returns "BTC"
            }
        }
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("1.0")
        }
        val cryptoCurrencyStatus = CryptoCurrencyStatus(currency = currency, value = statusValue)
        return SwapCurrencyStatus(
            userWallet = userWallet,
            status = cryptoCurrencyStatus,
            account = account,
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

    private fun buildSwapFee(): SwapFee {
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns BigDecimal("0.001")
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        val feeTokenStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
        return SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = feeTokenStatus,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
        )
    }
}