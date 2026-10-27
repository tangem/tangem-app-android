package com.tangem.data.walletmanager

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * The module upgrade a yield send fee was last estimated with, per wallet and network.
 *
 * The version check is soft, so it can fail for the estimate and pass for the transfer, and the transfer would then
 * send the upgrade-wrapped call with the gas limit of the bare one. While the estimate is recent enough to belong to
 * the same send, the transfer repeats its decision instead of checking again.
 */
internal class YieldSendUpgradeDecisions(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    private val decisions = ConcurrentHashMap<Key, Decision>()

    fun remember(userWalletId: UserWalletId, network: Network, estimatedCallData: SmartContractCallData) {
        decisions[Key(userWalletId, network.id)] = Decision(
            upgradeImplementation = (estimatedCallData as? EthereumYieldSupplyUpgradeToAndCallCallData)
                ?.newImplementation,
            madeAtMillis = currentTimeMillis(),
        )
    }

    fun applyTo(userWalletId: UserWalletId, network: Network, callData: SmartContractCallData): SmartContractCallData? {
        val decision = decisions[Key(userWalletId, network.id)]
            ?.takeIf { currentTimeMillis() - it.madeAtMillis <= DECISION_LIFETIME_MILLIS }
            ?: return null

        val upgradeImplementation = decision.upgradeImplementation ?: return callData
        return EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = upgradeImplementation,
            innerCallData = callData,
        )
    }

    private data class Key(val userWalletId: UserWalletId, val networkId: Network.ID)

    private class Decision(val upgradeImplementation: String?, val madeAtMillis: Long)

    private companion object {
        val DECISION_LIFETIME_MILLIS = 5.minutes.inWholeMilliseconds
    }
}