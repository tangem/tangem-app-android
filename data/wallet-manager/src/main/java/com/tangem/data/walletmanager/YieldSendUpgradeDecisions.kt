package com.tangem.data.walletmanager

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * The module upgrade a yield send was last priced with, per wallet and network. The version check is soft, so a
 * transfer built again for sending repeats a recent decision instead of checking again and mismatching the gas limit.
 */
internal class YieldSendUpgradeDecisions(
    private val currentTimeMillis: () -> Long,
) {

    private val lock = Any()
    private val decisions = mutableMapOf<Key, Decision>()
    private val generations = mutableMapOf<Key, Long>()
    private val keyLocks = ConcurrentHashMap<Key, Mutex>()

    @Inject
    constructor() : this(System::currentTimeMillis)

    /** A decision priced before a [forget] of the same key is dropped, as the upgrade it priced has been sent. */
    suspend fun remember(
        userWalletId: UserWalletId,
        network: Network,
        price: suspend () -> SmartContractCallData,
    ): SmartContractCallData {
        val key = Key(userWalletId, network.id)
        return keyLock(key).withLock { decide(key, price) }
    }

    /** Overlapping builds for the same key wait for each other, so they all send the decision of the first one. */
    suspend fun applyOrRemember(
        userWalletId: UserWalletId,
        network: Network,
        callData: SmartContractCallData,
        price: suspend () -> SmartContractCallData,
    ): SmartContractCallData {
        val key = Key(userWalletId, network.id)
        return keyLock(key).withLock { applyTo(key, callData) ?: decide(key, price) }
    }

    fun applyTo(userWalletId: UserWalletId, network: Network, callData: SmartContractCallData): SmartContractCallData? {
        return applyTo(Key(userWalletId, network.id), callData)
    }

    fun forget(userWalletId: UserWalletId, network: Network) {
        val key = Key(userWalletId, network.id)
        synchronized(lock) {
            decisions.remove(key)
            generations[key] = generations.getOrDefault(key, 0L) + 1
        }
    }

    private fun keyLock(key: Key): Mutex = keyLocks.getOrPut(key) { Mutex() }

    private suspend fun decide(key: Key, price: suspend () -> SmartContractCallData): SmartContractCallData {
        val generation = synchronized(lock) { generations.getOrDefault(key, 0L) }

        return price().also { pricedCallData ->
            synchronized(lock) {
                if (generations.getOrDefault(key, 0L) == generation) {
                    decisions[key] = Decision(
                        upgradeImplementation = (pricedCallData as? EthereumYieldSupplyUpgradeToAndCallCallData)
                            ?.newImplementation,
                        madeAtMillis = currentTimeMillis(),
                    )
                }
            }
        }
    }

    /** A decision in use is renewed, so a fee reloaded on it stays priced the way the transfer is built. */
    private fun applyTo(key: Key, callData: SmartContractCallData): SmartContractCallData? {
        val decision = synchronized(lock) {
            val now = currentTimeMillis()
            decisions[key]
                ?.takeIf { now - it.madeAtMillis <= DECISION_LIFETIME_MILLIS }
                ?.copy(madeAtMillis = now)
                ?.also { decisions[key] = it }
        } ?: return null

        val upgradeImplementation = decision.upgradeImplementation ?: return callData
        return EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = upgradeImplementation,
            innerCallData = callData,
        )
    }

    private data class Key(val userWalletId: UserWalletId, val networkId: Network.ID)

    private data class Decision(val upgradeImplementation: String?, val madeAtMillis: Long)

    private companion object {
        val DECISION_LIFETIME_MILLIS = 5.minutes.inWholeMilliseconds
    }
}