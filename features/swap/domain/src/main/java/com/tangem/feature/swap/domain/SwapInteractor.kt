package com.tangem.feature.swap.domain

import arrow.core.Either
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import java.math.BigDecimal

interface SwapInteractor {

    suspend fun getPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        filterProviderTypes: List<ExchangeProviderType>,
    ): Either<ExpressError, List<SwapPairLeast>>

    suspend fun findProvidersForPairWithCheck(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider>

    suspend fun getUnfulfilledReceiveRequirement(toSwapCurrencyStatus: SwapCurrencyStatus): AssetRequirementsCondition?

    fun findProvidersForPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider>

    fun extractFromSwapCurrencyFromPair(
        pair: SwapPairLeast,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ): SwapCurrencyStatus?

    @Throws(IllegalStateException::class)
    suspend fun findBestQuote(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        reduceBalanceBy: BigDecimal,
    ): Map<SwapProvider, SwapState>

    /**
     * Branch selection:
     *  - CEX, native fee → `sendTransactionUseCase`
     *  - CEX, gasless / token fee (`fee.transactionFeeResult is LoadedExtended` and
     *    `fee.selectedFeeToken.currency is CryptoCurrency.Token`) → `createAndSendGaslessTransactionUseCase`

     *  - DEX (Solana) → compiled tx signed as-is. `fee` is carried for analytics / UI only.
     *
     * @param fee the user-selected fee for the transaction. Required for DEX (non-Solana) and CEX;
     *   may be `null` for Solana DEX and the Tangem Pay withdrawal short-circuit.
     */
    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun onSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        amountToSwap: String,
        balanceStatus: SwapBalanceStatus,
        fee: SwapFee?,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
        integratedApproval: IntegratedApprovalData? = null,
    ): SwapTransactionState

    /**
     * Patches an existing [SwapState.QuotesLoadedState] with a freshly resolved [SwapFee] without re-fetching quotes.
     *
     * Recomputes `preparedSwapConfigState.balanceStatus`, plus `currencyCheck` and `validationResult`.
     *
     * **Idempotent**: applying the same [SwapFee] twice yields an equal state.
     */
    suspend fun applySwapFee(
        state: SwapState.QuotesLoadedState,
        fee: SwapFee,
        lastReducedBalanceBy: BigDecimal,
    ): SwapState.QuotesLoadedState

    /**
     * Returns token in wallet balance
     *
     * @param token
     */
    fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount

    @Suppress("LongParameterList")
    suspend fun storeSwapTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapProvider: SwapProvider,
        swapDataModel: SwapDataModel,
        timestamp: Long,
        txExternalUrl: String? = null,
        txExternalId: String? = null,
        averageDuration: Int? = null,
    )

    /**
     * Unified swap-fee entry point. Single fee load API used by all providers types (DEX, DEX_BRIDGE, CEX).
     *
     * Delegates to `DexSwapFeeCalculator` for DEX/DEX_BRIDGE or to `CexSwapFeeCalculator` for CEX,
     * then wraps the result in a [SwapFee].
     *
     * Flow is resolved by [txType], matching the quote-stage `resolveQuoteFlow`: a DEX/DEX_BRIDGE
     * provider whose quote returned `txType=SEND` (swap-xyz native transfer) takes the CEX-style
     * fee path even though [swapData] is `null`. `txType=SWAP`/`null` keeps the DEX path.
     *
     * The DEX path consumes the pre-fetched [swapData] (which carries the `ExpressTransactionModel.DEX` payload);
     * the CEX path computes the fee directly from `amount`.
     * When [swapData] is `null` on the DEX path the call short-circuits to `Left(GetFeeError.UnknownError)` —
     * callers must ensure swap data has resolved before triggering fee load.
     *
     * Native-fallback semantics on the CEX gasless path are preserved: when
     * [selectedFeeToken] is `null`, `EstimateFeeForGaslessTxUseCase` is invoked and chooses
     * native vs token internally. The returned `SwapFee.selectedFeeToken` is non-null —
     * resolved from gasless's chosen token or from the native coin status when gasless picked
     * native.
     *
     * @param swapData pre-fetched DEX exchange data; pass `null` for CEX providers.
     * @param selectedFeeToken the currency the user picked to pay the fee. `null` triggers the
     *   gasless / native-default path on CEX.
     */
    @Suppress("LongParameterList")
    suspend fun loadSwapFee(
        quotesLoadedState: SwapState.QuotesLoadedState,
        fromStatus: SwapCurrencyStatus,
        toStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapData: SwapDataModel?,
        selectedFeeToken: CryptoCurrencyStatus?,
        isGasless: Boolean,
        txType: ExpressTxType? = null,
    ): Either<GetFeeError, SwapFee>

    fun integratedApprovalFallback(fromSwapCurrencyStatus: SwapCurrencyStatus, spenderAddress: String)

    /**
     * Builds the on-chain ERC-20 approval transaction for [fromStatus] / [spenderAddress]
     * with an amount derived from [approveType] (null for `UNLIMITED`, the swap amount for
     * `LIMITED`), loads its [com.tangem.blockchain.common.transaction.TransactionFee] and returns
     * both as [IntegratedApprovalData].
     *
     * Used by the integrated approve+swap flow when
     * `SwapFeatureToggles.isSwapIntegratedApproveEnabled` is ON and the quote requires an
     * allowance bump.
     *
     * @param approvalAmount LIMITED-mode swap amount (the user-input amount). Used when
     *   [approveType] is `LIMITED`; ignored for `UNLIMITED`.
     */
    suspend fun loadIntegratedApprovalData(
        fromStatus: SwapCurrencyStatus,
        spenderAddress: String,
        approveType: ApproveType,
        approvalAmount: BigDecimal,
    ): Either<GetFeeError, IntegratedApprovalData>
}