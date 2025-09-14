package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository

internal class DefaultAuthProvider(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val shouldUseNewListRepository: Boolean = false,
) : AuthProvider {

    override suspend fun getCardPublicKey(): String {
        val userWallet = getSelectedWallet()

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardPublicKey.toHexString()
    }

    override suspend fun getCardId(): String {
        val userWallet = getSelectedWallet()

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardId
    }

    override suspend fun getCardsPublicKeys(): Map<String, String> {
        return getWallets().filterIsInstance<UserWallet.Cold>().associate {
            it.scanResponse.card.cardId to it.scanResponse.card.cardPublicKey.toHexString()
        }
    }

    private suspend fun getWallets(): List<UserWallet> {
        return if (shouldUseNewListRepository) {
            userWalletsListRepository.userWalletsSync()
        } else {
            userWalletsListManager.userWalletsSync
        }
    }

    private suspend fun getSelectedWallet(): UserWallet? {
        return if (shouldUseNewListRepository) {
            userWalletsListRepository.selectedUserWalletSync()
        } else {
            userWalletsListManager.selectedUserWalletSync
        }
    }
}