package com.tangem.feature.swap.domain.models.ui

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import java.math.BigDecimal

/**
 * Unified swap-fee result returned by `SwapInteractor.loadSwapFee`.
 *
 * [REDACTED_TASK_KEY] — Phase 3 of the swap fee redesign. Replaces the historical pair of
 * `TxFeeSealedState` (carrying `TxFee.Legacy` / `TxFee.FeeComponent`) plus the loose
 * `TransactionFeeResult` so callers no longer need to inspect concrete fee shapes.
 *
 * This type lives alongside the legacy types through Phase 3 and 4. Phase 5 deletes `TxFee`,
 * `TxFeeState`, `FeeType` and the two `loadFeeForSwapTransaction` overloads.
 *
 * @property fee the concrete [Fee] that will be signed and broadcast on-chain. For
 *   `TransactionFee.Single`-shaped responses this is the only choice; for
 *   `TransactionFee.Choosable`-shaped responses it is the bucket selected by the user (or the
 *   default MARKET tier when no selection has been made).
 * @property transactionFeeResult the full transaction-fee payload returned by the underlying
 *   use case. Preserved verbatim so it can be passed through to gasless send flows
 *   (`CreateAndSendGaslessTransactionUseCase` requires the [TransactionFeeResult.LoadedExtended]
 *   variant) without re-fetching.
 * @property selectedFeeToken the currency that pays the fee. Never null after this phase —
 *   for native fees it is the from-token's native coin status; for gasless / token-fee paths it
 *   is whatever token the user (or `EstimateFeeForGaslessTxUseCase`) selected. Used by
 *   downstream balance checks and analytics.
 * @property otherNativeFee bridge protocol fee (e.g. carried by `ExpressTransactionModel.DEX
 *   .otherNativeFeeWei` for DEX_BRIDGE providers). Always [BigDecimal.ZERO] unless the provider
 *   is `DEX_BRIDGE`. Propagated from [com.tangem.feature.swap.domain.fee.DexFeeResult].
 * @property feeBucket tier classifier derived from the parent [TransactionFee] shape (see
 *   [FeeBucket] mapping table). Drives analytics; will replace `FeeType.getNameForAnalytics()`
 *   in Phase 5.
 */
data class SwapFee(
    val fee: Fee,
    val transactionFeeResult: TransactionFeeResult,
    val selectedFeeToken: CryptoCurrencyStatus,
    val otherNativeFee: BigDecimal,
    val feeBucket: FeeBucket,
)