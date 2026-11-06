package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade

/**
 * A gasless yield-supply send runs on the bare `send`: the module is upgraded only when a yield withdraw pays
 * the fee, see [ResolveGaslessFeePlanUseCase]. Any other module call keeps its wrapper: a swap is wrapped
 * because the SDK refuses to route it through an outdated module at all.
 */
internal fun TransactionData.unwrapYieldSendUpgrade(): TransactionData {
    val callData = ((this as? TransactionData.Uncompiled)?.extras as? EthereumTransactionExtras)?.callData
    val upgradedCall = (callData as? EthereumYieldSupplyUpgradeToAndCallCallData)?.innerCallData
    return if (upgradedCall is EthereumYieldSupplySendCallData) unwrapYieldModuleUpgrade() else this
}