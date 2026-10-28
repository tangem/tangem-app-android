package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import io.mockk.mockk

internal val bareYieldSendCallData = EthereumYieldSupplySendCallData(
    tokenContractAddress = "0x0000000000000000000000000000000000000001",
    destinationAddress = "0x0000000000000000000000000000000000000002",
    amount = mockk(relaxed = true),
)

private val sentAmount: Amount = mockk(relaxed = true)

internal fun uncompiledWithCallData(callData: SmartContractCallData?) = TransactionData.Uncompiled(
    amount = sentAmount,
    fee = null,
    sourceAddress = "0xsource",
    destinationAddress = "0xmodule",
    extras = EthereumTransactionExtras(callData = callData),
)

internal fun wrappedYieldSendTransaction() = uncompiledWithCallData(
    callData = EthereumYieldSupplyUpgradeToAndCallCallData(
        newImplementation = "0x0000000000000000000000000000000000000003",
        innerCallData = bareYieldSendCallData,
    ),
)

internal fun bareYieldSendTransaction() = uncompiledWithCallData(callData = bareYieldSendCallData)