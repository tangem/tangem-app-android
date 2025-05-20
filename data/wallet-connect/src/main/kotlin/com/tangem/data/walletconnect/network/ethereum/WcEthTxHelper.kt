package com.tangem.data.walletconnect.network.ethereum

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import java.math.BigDecimal

internal object WcEthTxHelper {
    private val MANTLE_FEE_ESTIMATE_MULTIPLIER = BigDecimal("1.8")

    fun getDAppFee(network: Network, txParams: WcEthTransactionParams): Fee.Ethereum.Legacy? {
        val gasLimit = txParams.gas?.hexToBigDecimal() ?: return null
        val gasPrice = txParams.gasPrice?.hexToBigDecimal() ?: return null

        val blockchain = Blockchain.fromId(network.rawId)

        var feeDecimal = (gasLimit * gasPrice)
            .movePointLeft(blockchain.decimals())
        if (blockchain == Blockchain.Mantle) {
            feeDecimal = feeDecimal.multiply(MANTLE_FEE_ESTIMATE_MULTIPLIER)
        }

        val feeAmount = Amount(feeDecimal, blockchain)
        return Fee.Ethereum.Legacy(feeAmount, gasLimit.toBigInteger(), gasPrice.toBigInteger())
    }

    fun createTransactionData(
        dAppFee: Fee.Ethereum.Legacy?,
        network: Network,
        txParams: WcEthTransactionParams,
    ): TransactionData.Uncompiled? {
        val destinationAddress = txParams.to ?: return null
        val blockchain = Blockchain.fromId(network.rawId)
        val value = (txParams.value ?: "0")
            .hexToBigDecimal()
            .movePointLeft(blockchain.decimals())

        val callData = CompiledSmartContractCallData(txParams.data.removePrefix(HEX_PREFIX).hexToBytes())
        return TransactionData.Uncompiled(
            amount = Amount(value, blockchain),
            fee = dAppFee,
            sourceAddress = txParams.from,
            destinationAddress = destinationAddress,
            extras = EthereumTransactionExtras(
                callData = callData,
                nonce = txParams.nonce?.hexToBigDecimal()?.toBigInteger(),
            ),
        )
    }
}