package com.tangem.features.earn.lending.impl.provider

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import java.math.BigDecimal
import java.math.BigInteger

/** Ethereum transaction fixtures of the lending tests, every argument defaulted */
internal object LendingTestTransactions {

    const val MODULE = "0x1111111111111111111111111111111111111111"
    const val FACTORY = "0x2222222222222222222222222222222222222222"
    const val OWNER = "0x4444444444444444444444444444444444444444"
    const val LATEST = "0x6666666666666666666666666666666666666666"
    const val OLD = "0x7777777777777777777777777777777777777777"

    fun coin(value: BigDecimal = BigDecimal.ZERO) = Amount(value = value, blockchain = Blockchain.Ethereum)

    fun wrapped(inner: SmartContractCallData, implementation: String = LATEST) =
        EthereumYieldSupplyUpgradeToAndCallCallData(implementation, inner)

    fun createTx(
        callData: SmartContractCallData? = null,
        destination: String = MODULE,
        value: BigDecimal = BigDecimal.ZERO,
        source: String = OWNER,
        fee: Fee? = null,
    ) = TransactionData.Uncompiled(
        amount = coin(value),
        fee = fee,
        sourceAddress = source,
        destinationAddress = destination,
        extras = callData?.let { EthereumTransactionExtras(callData = it) },
    )

    fun ethFee(gasLimit: BigInteger, maxFeePerGas: Long = 1) = Fee.Ethereum.EIP1559(
        amount = coin(),
        gasLimit = gasLimit,
        maxFeePerGas = BigInteger.valueOf(maxFeePerGas),
        priorityFee = BigInteger.ONE,
    )
}