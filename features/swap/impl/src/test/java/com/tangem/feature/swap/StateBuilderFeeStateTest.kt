package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Characterization tests for `StateBuilder.createFeeState` (private), exercised through the
 * public `createQuotesLoadedState`.
 *
 * Pinned behavior:
 *  - `TxFeeState.Empty` → `FeeItemState.Empty`
 *  - `TxFeeState.SingleFeeState` → `FeeItemState.Content` with `isClickable = false`
 *  - `TxFeeState.MultipleFeeState` + `selectedFeeType = NORMAL` → uses normal fee values, isClickable = true
 *  - `TxFeeState.MultipleFeeState` + `selectedFeeType = PRIORITY` → uses priority fee values, isClickable = true
 *  - `hideFee = true` → always `FeeItemState.Empty` regardless of `txFee`
 *  - `feeCryptoFormattedWithNative` is what populates `FeeItemState.Content.amountCrypto`,
 *    NOT the plain `feeCryptoFormatted`. Same for fiat. (This pins the bridge-fee
 *    "display fee with native as workaround for okx" pathway.)
 *
 * [REDACTED_TASK_KEY] — these exist to guarantee the redesign's `FeeSelectorBlockComponent` carries
 * the same display semantics across the cutover.
 */
internal class StateBuilderFeeStateTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()

    private lateinit var sut: StateBuilder

    private val userWalletId = UserWalletId("aabbccdd")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns AppCurrency.Default
        every { isAccountsModeProvider() } returns false
        every { isGaslessFeeSupportedForNetwork(any()) } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        )
    }

    @Test
    fun `GIVEN TxFeeState Empty WHEN hideFee false THEN fee is FeeItemState Empty`() {
        val baseState = buildReadyState()
        val quoteModel = buildQuoteModel(txFeeState = TxFeeState.Empty)

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
    }

    @Test
    fun `GIVEN SingleFeeState WHEN hideFee false THEN fee Content is not clickable`() {
        val baseState = buildReadyState()
        val singleFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.001 ETH",
            cryptoFormattedWithNative = "0.001 ETH",
            fiatFormatted = "$2.00",
            fiatFormattedWithNative = "$2.00",
        )
        val quoteModel = buildQuoteModel(txFeeState = TxFeeState.SingleFeeState(singleFee))

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        val feeContent = result.fee as FeeItemState.Content
        assertThat(feeContent.isClickable).isFalse()
        // Field source pinning: amountCrypto/fiatFormatted come from the *WithNative variants.
        assertThat(feeContent.amountCrypto).isEqualTo("0.001 ETH")
        assertThat(feeContent.amountFiatFormatted).isEqualTo("$2.00")
    }

    @Test
    fun `GIVEN MultipleFeeState WHEN selectedFeeType NORMAL THEN fee Content has normal fee values and is clickable`() {
        val baseState = buildReadyState()
        val normalFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.001 ETH",
            cryptoFormattedWithNative = "0.001 ETH",
            fiatFormatted = "$2.00",
            fiatFormattedWithNative = "$2.00",
        )
        val priorityFee = buildLegacyFee(
            feeType = FeeType.PRIORITY,
            cryptoFormatted = "0.005 ETH",
            cryptoFormattedWithNative = "0.005 ETH",
            fiatFormatted = "$10.00",
            fiatFormattedWithNative = "$10.00",
        )
        val quoteModel = buildQuoteModel(
            txFeeState = TxFeeState.MultipleFeeState(normalFee = normalFee, priorityFee = priorityFee),
        )

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        val feeContent = result.fee as FeeItemState.Content
        assertThat(feeContent.isClickable).isTrue()
        assertThat(feeContent.feeType).isEqualTo(FeeType.NORMAL)
        assertThat(feeContent.amountCrypto).isEqualTo("0.001 ETH")
        assertThat(feeContent.amountFiatFormatted).isEqualTo("$2.00")
    }

    @Test
    fun `GIVEN MultipleFeeState WHEN selectedFeeType PRIORITY THEN fee Content has priority fee values and is clickable`() {
        val baseState = buildReadyState()
        val normalFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.001 ETH",
            cryptoFormattedWithNative = "0.001 ETH",
            fiatFormatted = "$2.00",
            fiatFormattedWithNative = "$2.00",
        )
        val priorityFee = buildLegacyFee(
            feeType = FeeType.PRIORITY,
            cryptoFormatted = "0.005 ETH",
            cryptoFormattedWithNative = "0.005 ETH",
            fiatFormatted = "$10.00",
            fiatFormattedWithNative = "$10.00",
        )
        val quoteModel = buildQuoteModel(
            txFeeState = TxFeeState.MultipleFeeState(normalFee = normalFee, priorityFee = priorityFee),
        )

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.PRIORITY,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        val feeContent = result.fee as FeeItemState.Content
        assertThat(feeContent.isClickable).isTrue()
        assertThat(feeContent.feeType).isEqualTo(FeeType.PRIORITY)
        assertThat(feeContent.amountCrypto).isEqualTo("0.005 ETH")
        assertThat(feeContent.amountFiatFormatted).isEqualTo("$10.00")
    }

    @Test
    fun `GIVEN hideFee true WHEN any TxFeeState THEN fee is Empty`() {
        val baseState = buildReadyState()
        val singleFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.001 ETH",
            cryptoFormattedWithNative = "0.001 ETH",
            fiatFormatted = "$2.00",
            fiatFormattedWithNative = "$2.00",
        )
        val quoteModel = buildQuoteModel(txFeeState = TxFeeState.SingleFeeState(singleFee))

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = true,
        )

        assertThat(result.fee).isInstanceOf(FeeItemState.Empty::class.java)
    }

    @Test
    fun `GIVEN otherNativeFee greater than feeValue WHEN SingleFeeState THEN amountCrypto reflects the With-Native variant`() {
        // Bridge fee scenario: the WithNative formatted strings differ from the plain ones.
        // StateBuilder.createFeeState picks `feeCryptoFormattedWithNative` (and fiat) — pinning that.
        val baseState = buildReadyState()
        val singleFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.001 ETH",
            cryptoFormattedWithNative = "0.006 ETH", // includes 0.005 bridge native fee
            fiatFormatted = "$2.00",
            fiatFormattedWithNative = "$12.00",
        )
        val quoteModel = buildQuoteModel(txFeeState = TxFeeState.SingleFeeState(singleFee))

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        val feeContent = result.fee as FeeItemState.Content
        assertThat(feeContent.amountCrypto).isEqualTo("0.006 ETH")
        assertThat(feeContent.amountFiatFormatted).isEqualTo("$12.00")
    }

    @Test
    fun `GIVEN otherNativeFee equal to feeValue WHEN SingleFeeState THEN amountCrypto equals plain feeCryptoFormatted`() {
        // No bridge fee → WithNative strings happen to equal the plain strings.
        val baseState = buildReadyState()
        val singleFee = buildLegacyFee(
            feeType = FeeType.NORMAL,
            cryptoFormatted = "0.0007 ETH",
            cryptoFormattedWithNative = "0.0007 ETH",
            fiatFormatted = "$1.40",
            fiatFormattedWithNative = "$1.40",
        )
        val quoteModel = buildQuoteModel(txFeeState = TxFeeState.SingleFeeState(singleFee))

        val result = sut.createQuotesLoadedState(
            uiStateHolder = baseState,
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = null,
            swapProvider = buildSwapProvider(),
            bestRatedProviderId = "provider-id",
            isNeedBestRateBadge = false,
            selectedFeeType = FeeType.NORMAL,
            needApplyFCARestrictions = false,
            hideFee = false,
        )

        val feeContent = result.fee as FeeItemState.Content
        assertThat(feeContent.amountCrypto).isEqualTo("0.0007 ETH")
        assertThat(feeContent.amountFiatFormatted).isEqualTo("$1.40")
    }

    // region — local fixtures

    private fun buildReadyState(): SwapStateHolder {
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val toStatus = buildSwapCurrencyStatus(coldWallet)
        return sut.createInitialReadyState(
            uiStateHolder = sut.createInitialLoadingState(),
            emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = com.tangem.core.ui.extensions.stringReference("$0.00"),
            ),
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
        )
    }

    private fun buildQuoteModel(
        txFeeState: TxFeeState,
    ): SwapState.QuotesLoadedState {
        val fromStatus = buildSwapCurrencyStatus(coldWallet)
        val toStatus = buildSwapCurrencyStatus(coldWallet)
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
                isBalanceEnough = true,
                feeState = SwapFeeState.Enough,
                hasOutgoingTransaction = false,
                includeFeeInAmount = IncludeFeeInAmount.Excluded,
            ),
            permissionState = PermissionDataState.Empty,
            txFee = txFeeState,
            currencyCheck = null,
            validationResult = null,
            minAdaValue = null,
            swapProvider = buildSwapProvider(),
        )
    }

    private fun buildSwapProvider(): SwapProvider = SwapProvider(
        providerId = "provider-id",
        rateTypes = listOf(RateType.FLOAT),
        name = "TestProvider",
        type = ExchangeProviderType.DEX,
        imageLarge = "https://example.com/icon.png",
        termsOfUse = null,
        privacyPolicy = null,
        isRecommended = false,
        slippage = null,
        isExtraIdSupported = false,
    )

    private fun buildLegacyFee(
        feeType: FeeType,
        cryptoFormatted: String,
        cryptoFormattedWithNative: String,
        fiatFormatted: String,
        fiatFormattedWithNative: String,
    ): TxFee.Legacy {
        val fee: Fee = mockk(relaxed = true)
        return TxFee.Legacy(
            feeValue = BigDecimal("0.001"),
            feeFiatFormatted = fiatFormatted,
            feeCryptoFormatted = cryptoFormatted,
            feeIncludeOtherNativeFee = BigDecimal.ZERO,
            feeFiatFormattedWithNative = fiatFormattedWithNative,
            feeCryptoFormattedWithNative = cryptoFormattedWithNative,
            cryptoSymbol = "ETH",
            feeType = feeType,
            fee = fee,
        )
    }

    // endregion
}