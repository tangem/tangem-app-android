package com.tangem.feature.swap.domain.fee

import com.tangem.feature.swap.domain.TransactionFeeResult
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Result of calculating the DEX swap transaction fee.
 *
 * [REDACTED_TASK_KEY] — produced by `DexSwapFeeCalculator`. Mirrors the data points that
 * `SwapInteractorImpl.loadFeeForDex` + `getFeeDataForDexSwap` + `getFeeDataForSolanaDexSwap`
 * compute today, but exposes them as a single value type instead of leaking through several
 * private return types.
 *
 * @param transactionFee the fee already patched by `PatchEthGasLimitForSwap` for EVM DEX paths;
 *   raw fee for Solana (no gas-limit bump applies). Solana always returns [TransactionFeeResult.Loaded];
 *   EVM may return [TransactionFeeResult.Loaded] or [TransactionFeeResult.LoadedExtended] depending
 *   on whether a `selectedToken` is supplied (token = LoadedExtended).
 * @param otherNativeFee the bridge protocol fee carried by the express transaction model
 *   (`ExpressTransactionModel.DEX.otherNativeFeeWei` shifted left by the native coin's decimals).
 *   Zero unless the provider is `DEX_BRIDGE`.
 * @param gas the gas value from `ExpressTransactionModel.DEX.gas`, propagated for callers that
 *   need to construct the transaction extras downstream. `null` for non-EVM (Solana) paths.
 */
data class DexFeeResult(
    val transactionFee: TransactionFeeResult,
    val otherNativeFee: BigDecimal,
    val gas: BigInteger?,
)