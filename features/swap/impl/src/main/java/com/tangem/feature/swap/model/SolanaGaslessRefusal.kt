package com.tangem.feature.swap.model

import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState

/**
 * A sponsored send refused in a way a retry will not fix, or one that may already have landed: the screen must stop
 * offering the sponsored swap. MaybeSent counts because a rebuilt quote gets a fresh blockhash, so a second sponsored
 * tap could swap twice.
 */
internal val SwapTransactionState.Error.isSolanaGaslessRefusal: Boolean
    get() = when (val error = (this as? SwapTransactionState.Error.SolanaGaslessError)?.error) {
        is SolanaGaslessError.RateLimited,
        is SolanaGaslessError.Rejected,
        is SolanaGaslessError.MaybeSent,
        -> true
        is SolanaGaslessError.ServiceUnavailable -> error.isSponsorshipUnavailable
        else -> false
    }