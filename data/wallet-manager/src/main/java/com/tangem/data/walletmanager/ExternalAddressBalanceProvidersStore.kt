package com.tangem.data.walletmanager

import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime cache of the balance providers of joint account networks, one per wallet and network — the counterpart
 * of `WalletManagersStore` for addresses the wallet does not own. Kept apart from the wallet managers so that no
 * consumer of "all wallet managers of the wallet" (WalletConnect, token removal) gets a Safe among them.
 */
@Singleton
internal class ExternalAddressBalanceProvidersStore @Inject constructor() {

    private val store = RuntimeSharedMapStore<Key, ExternalAddressBalanceProvider>()

    suspend fun getSyncOrNull(userWalletId: UserWalletId, network: Network): ExternalAddressBalanceProvider? {
        return store.getSyncOrNull(key = Key(userWalletId, network.id))
    }

    suspend fun store(userWalletId: UserWalletId, network: Network, provider: ExternalAddressBalanceProvider) {
        store.store(key = Key(userWalletId, network.id), value = provider)
    }

    suspend fun remove(userWalletId: UserWalletId, networks: Set<Network>) {
        store.remove(keys = networks.map { network -> Key(userWalletId, network.id) })
    }

    private data class Key(val userWalletId: UserWalletId, val networkId: Network.ID)
}