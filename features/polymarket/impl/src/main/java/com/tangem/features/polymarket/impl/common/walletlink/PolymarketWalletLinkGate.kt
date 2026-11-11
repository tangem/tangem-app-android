package com.tangem.features.polymarket.impl.common.walletlink

import arrow.core.getOrElse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.EnsureWalletLinkedUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

internal class PolymarketWalletLinkGate @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val ensureWalletLinkedUseCase: EnsureWalletLinkedUseCase,
) {

    suspend fun ensureLinked(userWalletId: UserWalletId): Boolean {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse { error ->
            TangemLogger.e("Wallet link gate: wallet $userWalletId not found ($error)")
            return false
        }

        val isLinked = try {
            ensureWalletLinkedUseCase(userWallet)
        } catch (e: TimeoutCancellationException) {
            TangemLogger.e("Wallet link gate: the link sheet never appeared for wallet $userWalletId", e)
            false
        }
        if (!isLinked) TangemLogger.i("Wallet link gate: wallet $userWalletId is not linked to the device")
        return isLinked
    }
}