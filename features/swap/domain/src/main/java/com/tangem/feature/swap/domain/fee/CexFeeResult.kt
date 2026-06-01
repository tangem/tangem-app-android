package com.tangem.feature.swap.domain.fee

/**
 * Result of calculating the CEX swap transaction fee.
 *
 * [REDACTED_TASK_KEY] — produced by `CexSwapFeeCalculator`. Mirrors the data points that the CEX path of
 * `SwapInteractorImpl.loadFeeForSwapTransaction` (overload 2) and `getFeeForCex` compute today.
 *
 * @param transactionFee the patched fee. For EVM the 5% gas-limit bump from
 *   `PatchEthGasLimitForSwap.SEND_PERCENTAGE` has already been applied. The variant —
 *   [TransactionFeeResult.Loaded] vs [TransactionFeeResult.LoadedExtended] — depends on the
 *   selected fee strategy: native fee → `Loaded`; gasless / explicit token → `LoadedExtended`.
 */
data class CexFeeResult(
    val transactionFee: TransactionFeeResult,
)