package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet

internal class DefaultAuthProvider(private val userWalletsListManager: UserWalletsListManager) : AuthProvider {

    override fun getCardPublicKey(): String {
        val userWallet = userWalletsListManager.selectedUserWalletSync

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardPublicKey.toHexString()
    }

    override fun getCardId(): String {
        val userWallet = userWalletsListManager.selectedUserWalletSync

        if (userWallet !is UserWallet.Cold) {
            return ""
        }

        return userWallet.scanResponse.card.cardId
    }

    override fun getCardsPublicKeys(): Map<String, String> {
        return userWalletsListManager.userWalletsSync.filterIsInstance<UserWallet.Cold>().associate {
            it.scanResponse.card.cardId to it.scanResponse.card.cardPublicKey.toHexString()
        }
    }
}