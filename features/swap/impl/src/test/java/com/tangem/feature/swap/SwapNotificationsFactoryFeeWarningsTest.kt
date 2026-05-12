package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.RateType
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.domain.models.ui.TxFee
import com.tangem.feature.swap.domain.models.ui.TxFeeState
import com.tangem.feature.swap.model.SwapNotificationsFactory
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Characterization tests for fee-related notifications produced by [SwapNotificationsFactory].
 *
 * Pinned behavior:
 *  - [SwapNotificationUM.Error.UnableToCoverFeeWarning]:
 *      adds when `feeState = NotEnough` AND `isBalanceEnough = true` AND
 *      `permissionState != PermissionLoading` AND fee currency != fromCurrency,
 *      AND NOT (gasless network AND CEX provider).
 *      Suppressed for CEX-on-gasless-network. Re-added unconditionally when
 *      `includeFeeInAmount is BalanceNotEnough`.
 *  - [NotificationUM.Warning.FeeCoverageNotification]: triggers on
 *      `includeFeeInAmount is Included` AND a fee is selected AND no existential deposit.
 *  - [SwapNotificationUM.Info.PermissionNeeded]: triggers on `permissionState is PermissionRequired`.
 *  - [SwapNotificationUM.Error.TransactionInProgressWarning]: triggers on
 *      `hasOutgoingTransaction = true` (when `permissionState is not PermissionLoading`).
 *  - `hideFee = true` short-circuits `maybeAddUnableCoverFeeWarning` only.
 *
 * [REDACTED_TASK_KEY] — these guard the redesign that consolidates fee-state into `SwapFee` /
 * `FeeBucket`. Phase 5 will rewrite the factory; these tests stay green throughout.
 */
internal class SwapNotificationsFactoryFeeWarningsTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()

    private val sut: SwapNotificationsFactory by lazy {
        SwapNotificationsFactory(
            actions = actions,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        )
    }

    private val ethNetworkMock: Network = mockk(relaxed = true) {
        every { name } returns "Ethereum"
        every { currencySymbol } returns "ETH"
        every { rawId } returns "ethereum"
    }
    private val fromCurrency: CryptoCurrency = mockk<CryptoCurrency.Coin>(relaxed = true) {
        every { network } returns ethNetworkMock
        every { symbol } returns "ETH"
        every { decimals } returns 18
        every { name } returns "Ethereum"
    }
    private val differentFeeCurrency: CryptoCurrency = mockk<CryptoCurrency.Token>(relaxed = true) {
        every { network } returns ethNetworkMock
        every { symbol } returns "USDC"
        every { decimals } returns 6
        every { name } returns "USD Coin"
    }

    // ---------- UnableToCoverFeeWarning ----------

    @Test
    fun `UnableToCoverFeeWarning is added when feeState NotEnough and balance enough and not gasless and fee currency differs`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.NotEnough(currencyName = "Ethereum", currencySymbol = "ETH"),
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
        )
        val feeStatus = buildFeeStatus(differentFeeCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }).isTrue()
    }

    @Test
    fun `UnableToCoverFeeWarning is suppressed when gasless is available for CEX provider`() {
        // Given — CEX + supported network → suppressed
        every { isGaslessFeeSupportedForNetwork(any()) } returns true
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.NotEnough(currencyName = "Ethereum", currencySymbol = "ETH"),
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.CEX,
        )
        val feeStatus = buildFeeStatus(differentFeeCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }).isFalse()
    }

    @Test
    fun `UnableToCoverFeeWarning is added even when gasless is available IF includeFeeInAmount is BalanceNotEnough`() {
        // Given — CEX + supported network BUT BalanceNotEnough overrides the suppression.
        every { isGaslessFeeSupportedForNetwork(any()) } returns true
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.NotEnough(currencyName = "Ethereum", currencySymbol = "ETH"),
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.BalanceNotEnough,
            providerType = ExchangeProviderType.CEX,
        )
        val feeStatus = buildFeeStatus(differentFeeCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }).isTrue()
    }

    @Test
    fun `UnableToCoverFeeWarning is not added when hideFee true`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.NotEnough(currencyName = "Ethereum", currencySymbol = "ETH"),
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
        )
        val feeStatus = buildFeeStatus(differentFeeCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = true,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }).isFalse()
    }

    @Test
    fun `UnableToCoverFeeWarning is not added when feeState is Enough`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
        )
        val feeStatus = buildFeeStatus(differentFeeCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }).isFalse()
    }

    // ---------- FeeCoverageNotification ----------

    @Test
    fun `FeeCoverageNotification is added when includeFeeInAmount is Included with a selected fee`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val singleFee = buildLegacyFee(FeeType.NORMAL)
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Included(SwapAmount(BigDecimal("0.99"), 18)),
            providerType = ExchangeProviderType.CEX,
            txFeeState = TxFeeState.SingleFeeState(singleFee),
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is NotificationUM.Warning.FeeCoverageNotification }).isTrue()
    }

    @Test
    fun `FeeCoverageNotification is not added when includeFeeInAmount is Excluded`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val singleFee = buildLegacyFee(FeeType.NORMAL)
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.CEX,
            txFeeState = TxFeeState.SingleFeeState(singleFee),
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is NotificationUM.Warning.FeeCoverageNotification }).isFalse()
    }

    // ---------- PermissionNeeded ----------

    @Test
    fun `PermissionNeeded is added when permissionState is PermissionRequired`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.PermissionRequired(
                isResetApproval = false,
                spenderAddress = "0xSpender",
            ),
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Info.PermissionNeeded }).isTrue()
    }

    @Test
    fun `PermissionNeeded is not added when permissionState is Empty`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Info.PermissionNeeded }).isFalse()
    }

    // ---------- TransactionInProgressWarning / ApprovalInProgressWarning ----------

    @Test
    fun `ApprovalInProgressWarning is added when permissionState is PermissionLoading`() {
        // Given — PermissionLoading short-circuits to ApprovalInProgressWarning (an Error subtype)
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.PermissionLoading,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
            hasOutgoingTransaction = false,
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.ApprovalInProgressWarning }).isTrue()
    }

    @Test
    fun `TransactionInProgressWarning is added when hasOutgoingTransaction true and permission not loading`() {
        // Given
        every { isGaslessFeeSupportedForNetwork(any()) } returns false
        val quoteModel = buildQuoteModel(
            feeState = SwapFeeState.Enough,
            isBalanceEnough = true,
            permissionState = PermissionDataState.Empty,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
            providerType = ExchangeProviderType.DEX,
            hasOutgoingTransaction = true,
        )
        val feeStatus = buildFeeStatus(fromCurrency)

        // When
        val notifications = sut.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeStatus,
            selectedFeeType = FeeType.NORMAL,
            hideFee = false,
        )

        // Then
        assertThat(notifications.any { it is SwapNotificationUM.Error.TransactionInProgressWarning }).isTrue()
    }

    // region — local helpers

    @Suppress("LongParameterList")
    private fun buildQuoteModel(
        feeState: SwapFeeState = SwapFeeState.Enough,
        isBalanceEnough: Boolean = true,
        permissionState: PermissionDataState = PermissionDataState.Empty,
        includeFeeInAmount: IncludeFeeInAmount = IncludeFeeInAmount.Excluded,
        providerType: ExchangeProviderType = ExchangeProviderType.DEX,
        txFeeState: TxFeeState = TxFeeState.Empty,
        hasOutgoingTransaction: Boolean = false,
    ): SwapState.QuotesLoadedState {
        val fromStatus = buildSwapCurrencyStatusForFromCurrency()
        val toStatus = buildSwapCurrencyStatusForFromCurrency()
        val fromInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(BigDecimal("1.0"), 18),
            amountFiat = BigDecimal("100.00"),
            swapCurrencyStatus = fromStatus,
        )
        val toInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(BigDecimal("0.05"), 18),
            amountFiat = BigDecimal("100.00"),
            swapCurrencyStatus = toStatus,
        )
        return SwapState.QuotesLoadedState(
            fromTokenInfo = fromInfo,
            toTokenInfo = toInfo,
            priceImpact = PriceImpact.Empty,
            preparedSwapConfigState = PreparedSwapConfigState(
                isBalanceEnough = isBalanceEnough,
                feeState = feeState,
                hasOutgoingTransaction = hasOutgoingTransaction,
                includeFeeInAmount = includeFeeInAmount,
            ),
            permissionState = permissionState,
            txFee = txFeeState,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = SwapProvider(
                providerId = "p",
                rateTypes = listOf(RateType.FLOAT),
                name = "TestProvider",
                type = providerType,
                imageLarge = "",
                termsOfUse = null,
                privacyPolicy = null,
                isRecommended = false,
                slippage = null,
                isExtraIdSupported = false,
            ),
        )
    }

    private fun buildSwapCurrencyStatusForFromCurrency(): SwapCurrencyStatus {
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("1.0")
            every { fiatRate } returns BigDecimal("2000.00")
            every { fiatAmount } returns BigDecimal("2000.00")
            every { networkAddress } returns mockk<NetworkAddress>(relaxed = true)
            every { pendingTransactions } returns emptySet()
        }
        val cryptoCurrencyStatus = CryptoCurrencyStatus(currency = fromCurrency, value = statusValue)
        val userWallet: UserWallet = mockk(relaxed = true) {
            every { walletId } returns UserWalletId("aabbccdd")
        }
        val account = com.tangem.domain.models.account.Account.CryptoPortfolio.createMainAccount(userWallet.walletId)
        return SwapCurrencyStatus(
            userWallet = userWallet,
            status = cryptoCurrencyStatus,
            account = account,
        )
    }

    private fun buildFeeStatus(currency: CryptoCurrency): CryptoCurrencyStatus {
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("0.5")
        }
        return CryptoCurrencyStatus(currency = currency, value = statusValue)
    }

    private fun buildLegacyFee(feeType: FeeType): TxFee.Legacy {
        val fee: Fee = mockk(relaxed = true)
        return TxFee.Legacy(
            feeValue = BigDecimal("0.001"),
            feeFiatFormatted = "$2.00",
            feeCryptoFormatted = "0.001 ETH",
            feeIncludeOtherNativeFee = BigDecimal.ZERO,
            feeFiatFormattedWithNative = "$2.00",
            feeCryptoFormattedWithNative = "0.001 ETH",
            cryptoSymbol = "ETH",
            feeType = feeType,
            fee = fee,
        )
    }

    // endregion
}