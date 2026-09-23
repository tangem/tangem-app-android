package com.tangem.features.send.send.joint

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter

/**
 * The fee the Safe refunds the executor for a [SafeTx]: `gasPrice × (safeTxGas + baseGas)` in the network's coin.
 * There is one figure, so the selector shows it as a fixed row.
 */
internal class JointSendFeeConverter(private val network: Network) : Converter<SafeTx, TransactionFee> {

    override fun convert(value: SafeTx): TransactionFee {
        val blockchain = network.toBlockchain()
        val gasLimit = value.safeTxGas + value.baseGas
        val feeWei = value.gasPrice * gasLimit

        return TransactionFee.Single(
            normal = Fee.Ethereum.Legacy(
                amount = Amount(
                    value = feeWei.toBigDecimal().movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                ),
                gasLimit = gasLimit,
                gasPrice = value.gasPrice,
            ),
        )
    }
}