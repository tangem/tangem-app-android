package com.tangem.domain.common.wallets

import com.tangem.domain.models.wallet.UserWallet

/**
 * Handler invoked when a user wallet becomes the active one.
 *
 * Side effects (analytics tracking context, Tangem SDK display config, access code request policy, etc.)
 * follow switch-latest semantics: if a new selection arrives while a previous one is still being processed,
 * the in-flight job is cancelled and only the latest selection is applied.
 */
interface UserWalletSelectedHandler {

    suspend operator fun invoke(userWallet: UserWallet)
}