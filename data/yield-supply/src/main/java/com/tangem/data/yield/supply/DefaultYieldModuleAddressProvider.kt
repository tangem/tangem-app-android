package com.tangem.data.yield.supply

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class DefaultYieldModuleAddressProvider(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldModuleAddressProvider {

    private data class Key(val userWalletId: UserWalletId, val networkRawId: String)

    private val cache = ConcurrentHashMap<Key, String>()
    private val mutex = Mutex()

    override suspend fun getOrFetch(userWalletId: UserWalletId, network: Network): String? {
        val key = Key(userWalletId, network.rawId)
        cache[key]?.let { return it }
        return withContext(dispatchers.io) {
            mutex.withLock {
                cache[key]?.let { return@withLock it }
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = network.toBlockchain(),
                    derivationPath = network.derivationPath.value,
                ) ?: error("Wallet manager not found for $network")
                // SDK returns ZERO_ADDRESS on internal failure (e.g. RPC error). Treat that as
                // "unavailable" so callers are forced by the type system to fall back instead
                // of using it as a destination.
                val address = walletManager.getYieldModuleAddress()
                    .takeIf { it != EthereumUtils.ZERO_ADDRESS }
                if (address != null) cache[key] = address
                address
            }
        }
    }

    override fun invalidate(userWalletId: UserWalletId?) {
        if (userWalletId == null) {
            cache.clear()
        } else {
            cache.keys.removeAll { it.userWalletId == userWalletId }
        }
    }
}