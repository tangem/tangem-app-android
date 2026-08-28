package com.tangem.data.walletmanager

import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime cache of the balance providers of joint account networks, one per wallet and network — the counterpart
 * of `WalletManagersStore` for addresses the wallet does not own. Kept apart from the wallet managers so that no
 * consumer of "all wallet managers of the wallet" (WalletConnect, token removal) gets a Safe among them.
 */
@Singleton
internal class ExternalAddressBalanceProvidersStore @Inject constructor() {

    private val providers = ConcurrentHashMap<Key, ExternalAddressBalanceProvider>()

    fun getSyncOrNull(userWalletId: UserWalletId, network: Network): ExternalAddressBalanceProvider? {
        return providers[Key(userWalletId, network.id)]
    }

    fun store(userWalletId: UserWalletId, network: Network, provider: ExternalAddressBalanceProvider) {
        providers[Key(userWalletId, network.id)] = provider
    }

    fun remove(userWalletId: UserWalletId, networks: Set<Network>) {
        networks.forEach { network -> providers.remove(Key(userWalletId, network.id)) }
    }

    private data class Key(val userWalletId: UserWalletId, val networkId: Network.ID)
}