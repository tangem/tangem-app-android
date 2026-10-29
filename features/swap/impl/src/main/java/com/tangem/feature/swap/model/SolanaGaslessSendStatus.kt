package com.tangem.feature.swap.model

import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapState
import java.math.BigDecimal

/**
 * The balance status to send a swap with. Only the selected provider's quote gets the fee applied, so a quote shown
 * before the next fee result — one switched to from another provider, for instance — still carries
 * [SwapBalanceStatus.Pending]. Every other flow tolerates a pending status at send, but a sponsored Solana swap sent
 * as pending goes out unsponsored from an empty account. Only a sponsored outcome replaces the pending status, so
 * other flows keep their behaviour.
 *
 * A sponsorship stored before the service refused this screen's swap is dropped: a fee patch still running at the
 * refusal can write one back, and after MaybeSent a second sponsored send could swap twice.
 */
internal suspend fun SwapInteractor.resolveSendBalanceStatus(
    quoteState: SwapState.QuotesLoadedState,
    fee: SwapFee?,
    lastReducedBalanceBy: BigDecimal,
    isSolanaGaslessSuppressed: Boolean,
): SwapBalanceStatus {
    val status = quoteState.preparedSwapConfigState.balanceStatus
    if (status is SwapBalanceStatus.SponsoredFee && isSolanaGaslessSuppressed) return SwapBalanceStatus.Pending
    if (status !is SwapBalanceStatus.Pending || fee?.isSponsored != true) return status

    val resolved = applySwapFee(
        state = quoteState,
        fee = fee,
        lastReducedBalanceBy = lastReducedBalanceBy,
        isSolanaGaslessSuppressed = isSolanaGaslessSuppressed,
    ).preparedSwapConfigState.balanceStatus
    return resolved as? SwapBalanceStatus.SponsoredFee ?: status
}