package com.tangem.features.earn.lending.impl.provider

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.lib.crypto.BlockchainUtils

internal val TransactionData.Uncompiled.callData: SmartContractCallData?
    get() = (extras as? EthereumTransactionExtras)?.callData

internal fun SmartContractCallData?.isSameCallAs(other: SmartContractCallData?): Boolean =
    this?.methodId == other?.methodId && this?.data.contentEquals(other?.data)

internal fun String.isSameAddressAs(other: String, networkId: String): Boolean =
    equals(other, ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(networkId))

internal fun TransactionData.Uncompiled.isSentTo(address: String, networkId: String): Boolean =
    destinationAddress.isSameAddressAs(address, networkId)

/** Whether both transactions do the same; the fee is left out, as it is priced apart from what a transaction does */
internal fun TransactionData.Uncompiled.isSameTransactionAs(
    other: TransactionData.Uncompiled,
    networkId: String,
): Boolean {
    val value = amount.value
    val otherValue = other.amount.value
    val isSameValue = if (value != null && otherValue != null) value.compareTo(otherValue) == 0 else value == otherValue
    return isSentTo(other.destinationAddress, networkId) && isSameValue && callData.isSameCallAs(other.callData)
}