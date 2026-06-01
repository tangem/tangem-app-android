package com.tangem.feature.swap.domain.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.transaction.models.TransactionFeeExtended

/**
 * Result of a swap-fee calculation.
 *
 * [REDACTED_TASK_KEY] — extracted from `SwapInteractorImpl.kt` into its own file alongside the other
 * `fee` package types ([DexFeeResult], [CexFeeResult], [DexSwapFeeCalculator],
 * [CexSwapFeeCalculator]). No behavioral change; this is purely a relocation.
 *
 * Two variants are required because the SDK exposes two fee shapes:
 *  - [Loaded] wraps a [TransactionFee] (native fee path).
 *  - [LoadedExtended] wraps a [TransactionFeeExtended] (gasless / token-fee path).
 *
 * The [from] factories let call-sites build the right variant without inspecting the concrete
 * type at the call site.
 */
sealed class TransactionFeeResult {
    class Loaded(val fee: TransactionFee) : TransactionFeeResult()
    class LoadedExtended(val fee: TransactionFeeExtended) : TransactionFeeResult()

    companion object {
        fun from(fee: TransactionFee) = Loaded(fee)
        fun from(fee: TransactionFeeExtended) = LoadedExtended(fee)
    }
}