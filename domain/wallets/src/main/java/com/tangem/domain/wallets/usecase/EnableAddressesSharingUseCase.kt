package com.tangem.domain.wallets.usecase

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

/**
 * Marks the wallet as one whose token addresses the backend must know, and sends them right away.
 *
 * Called once an MCP agent is authorized for the wallet: from then on every token push carries the addresses,
 * the already saved tokens included, which is what this first sync is for.
 */
class EnableAddressesSharingUseCase(
    private val walletsRepository: WalletsRepository,
    private val accountsCRUDRepository: AccountsCRUDRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        if (walletsRepository.isAddressesSharingRequired(userWalletId)) return

        walletsRepository.setAddressesSharingRequired(userWalletId)
        accountsCRUDRepository.syncTokens(userWalletId)
    }
}