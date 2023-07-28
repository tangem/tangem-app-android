package com.tangem.feature.wallet.presentation.router

import com.tangem.domain.wallets.models.UserWalletId

/**
 * Wallet feature screens
 *
 * @property route route string representation
 *
 * @author Andrew Khokhlov on 31/05/2023
 */
internal sealed class WalletRoute(val route: String) {

    object Wallet : WalletRoute(route = "wallet")

    object OrganizeTokens : WalletRoute(route = "wallet/{$userWalletIdKey}/organize_tokens") {

        fun createRoute(userWalletId: UserWalletId) = "wallet/${userWalletId.stringValue}/organize_tokens"
    }

    companion object {
        const val userWalletIdKey = "userWalletId"
    }
}
