package com.tangem.domain.common.wallets

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Sealed class representing actions that can transform the user wallets list.
 */
sealed class UserWalletTransformAction {

    /**
     * The transformation block to apply to the current wallets list.
     */
    abstract fun transform(list: List<UserWallet>): List<UserWallet>

    /**
     * Reorders user wallets
     *
     * @param action The transformation block that takes the current list of user wallets
     *               and returns a list of user wallet IDs in the desired order.
     */
    data class Reorder(
        private val action: (List<UserWallet>) -> List<UserWalletId>,
    ) : UserWalletTransformAction() {

        override fun transform(list: List<UserWallet>): List<UserWallet> {
            val walletById = list.associateBy { it.walletId }
            val newOrderIds = action(list)
            require(walletById.keys == newOrderIds.toSet()) {
                "Reorder action must return the same set of wallet IDs as the input list"
            }
            return newOrderIds.mapNotNull { walletById[it] }
        }
    }
}