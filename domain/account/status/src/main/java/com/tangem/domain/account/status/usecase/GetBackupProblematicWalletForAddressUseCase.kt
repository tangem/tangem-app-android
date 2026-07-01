package com.tangem.domain.account.status.usecase

import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase

/**
 * Resolves [address] to one of the user's own wallets and returns its [UserWalletId] when that wallet
 * has a backup error (e.g. a CardLinked card), or `null` otherwise.
 *
 * Shared by the send and swap flows to block topping up a wallet with a backup problem.
 */
class GetBackupProblematicWalletForAddressUseCase(
    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
) {

    suspend operator fun invoke(address: String): UserWalletId? {
        val walletId = getAccountCurrencyByAddressUseCase(address).getOrNull()?.account?.userWalletId ?: return null
        val wallet = getUserWalletUseCase(walletId).getOrNull() ?: return null
        return walletId.takeIf { isWalletBackupProblematicUseCase(wallet) }
    }
}