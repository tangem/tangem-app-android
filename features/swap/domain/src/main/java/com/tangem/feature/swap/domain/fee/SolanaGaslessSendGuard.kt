package com.tangem.feature.swap.domain.fee

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.utils.time.CurrentTimeProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a wallet from being offered the sponsored fee while its sponsored swap is being sent (the submit can take
 * ~40 s, and a second sponsored swap must not start meanwhile) and for [REFUSAL_HOLD_MILLIS] after the service
 * refused one, so the user is not walked into the same refusal again.
 */
@Singleton
internal class SolanaGaslessSendGuard @Inject constructor(
    private val currentTimeProvider: CurrentTimeProvider,
) {

    private val sendingWalletIds = ConcurrentHashMap.newKeySet<UserWalletId>()
    private val refusedUntilMillis = ConcurrentHashMap<UserWalletId, Long>()

    fun isBlocked(userWalletId: UserWalletId): Boolean {
        if (userWalletId in sendingWalletIds) return true
        val refusedUntil = refusedUntilMillis[userWalletId] ?: return false
        return refusedUntil > currentTimeProvider.nowMillis()
    }

    /**
     * Runs [send] as the wallet's only sponsored send, or returns null when one is already in flight. A thrown
     * [send] counts as a refusal: it may come after the submit, e.g. from storing the sent swap.
     */
    suspend fun guard(userWalletId: UserWalletId, send: suspend () -> SwapTransactionState): SwapTransactionState? {
        if (!sendingWalletIds.add(userWalletId)) return null
        var isRefused = true
        try {
            return send().also { isRefused = it.isRefusal() }
        } finally {
            if (isRefused) {
                refusedUntilMillis[userWalletId] = currentTimeProvider.nowMillis() + REFUSAL_HOLD_MILLIS
            } else {
                refusedUntilMillis.remove(userWalletId)
            }
            sendingWalletIds.remove(userWalletId)
        }
    }

    /**
     * A refusal a retry will not fix. MaybeSent without a signature counts too: a rebuilt quote gets a fresh
     * blockhash, so a second sponsored tap could swap twice.
     */
    private fun SwapTransactionState.isRefusal(): Boolean =
        when (val error = (this as? SwapTransactionState.Error.SolanaGaslessError)?.error) {
            is SolanaGaslessError.RateLimited,
            is SolanaGaslessError.Rejected,
            is SolanaGaslessError.MaybeSent,
            -> true
            is SolanaGaslessError.ServiceUnavailable -> error.isSponsorshipUnavailable
            else -> false
        }

    private companion object {
        const val REFUSAL_HOLD_MILLIS = 60_000L
    }
}