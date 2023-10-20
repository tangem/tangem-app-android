package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.blockchains.near.network.NearAccount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.tap.features.send.redux.AddressVerifyAction

internal class AddressValidator {

    suspend fun validateAddress(walletManager: WalletManager, address: String): AddressVerifyAction.Error? {
        val blockchain = walletManager.wallet.blockchain
        val wallet = walletManager.wallet
        return if ((blockchain == Blockchain.Near || blockchain == Blockchain.NearTestnet) &&
            address.length != NEAR_IMPLICIT_ADDRESS_LENGTH
        ) {
            validateNearAddress(walletManager, address)
        } else {
            validateAddress(wallet, address)
        }
    }

    private suspend fun validateNearAddress(
        walletManager: WalletManager,
        address: String,
    ): AddressVerifyAction.Error? {
        val result = (walletManager as? NearWalletManager)?.getAccount(address)
        return if (result is Result.Success && result.data is NearAccount.Full) {
            null
        } else {
            AddressVerifyAction.Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN
        }
    }

    private fun validateAddress(wallet: Wallet, address: String): AddressVerifyAction.Error? {
        return if (wallet.blockchain.validateAddress(address)) {
            if (wallet.addresses.all { it.value != address }) {
                null
            } else {
                AddressVerifyAction.Error.ADDRESS_SAME_AS_WALLET
            }
        } else {
            AddressVerifyAction.Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN
        }
    }

    companion object {
        private const val NEAR_IMPLICIT_ADDRESS_LENGTH = 64
    }
}