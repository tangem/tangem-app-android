package com.tangem.data.wallets

import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletAddressServiceRepository

class DefaultWalletAddressServiceRepository(
    private val walletManagersFacade: WalletManagersFacade,
) : WalletAddressServiceRepository {
    override suspend fun validate(userWalletId: UserWalletId, network: Network, address: String): Boolean {
        val blockchain = Blockchain.fromId(network.id.value)

        return if (blockchain.isNear()) {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = blockchain,
                derivationPath = network.derivationPath.value,
            ) ?: return false
            (walletManager as? NearWalletManager)?.validateAddress(address) ?: false
        } else {
            blockchain.validateAddress(address)
        }
    }

    private fun Blockchain.isNear(): Boolean {
        return this == Blockchain.Near || this == Blockchain.NearTestnet
    }
}