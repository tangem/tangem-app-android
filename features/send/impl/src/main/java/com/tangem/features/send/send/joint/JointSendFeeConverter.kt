package com.tangem.features.send.send.joint

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter
import java.math.BigInteger

/**
 * The fee the Safe refunds the executor for a [SafeTx]: `gasPrice × (safeTxGas + baseGas)` in its gas token.
 * There is one figure, so the selector shows it as a fixed row. A token refund is the gasless token fee type, which
 * the confirm step already reads as "the fee is paid in the selected token".
 */
internal class JointSendFeeConverter(
    private val network: Network,
    private val feeToken: CryptoCurrency.Token? = null,
    private val gasToken: SafeGasToken = SafeGasToken.Coin,
) : Converter<SafeTx, TransactionFee> {

    override fun convert(value: SafeTx): TransactionFee {
        val gasLimit = value.safeTxGas + value.baseGas
        val feeUnits = value.gasPrice * gasLimit

        val tokenGas = gasToken as? SafeGasToken.Token
        val fee = if (feeToken == null || tokenGas == null) {
            val blockchain = network.toBlockchain()
            Fee.Ethereum.Legacy(
                amount = Amount(
                    value = feeUnits.toBigDecimal().movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                ),
                gasLimit = gasLimit,
                gasPrice = value.gasPrice,
            )
        } else {
            Fee.Ethereum.TokenCurrency(
                amount = Amount(
                    token = Token(
                        symbol = feeToken.symbol,
                        contractAddress = feeToken.contractAddress,
                        decimals = feeToken.decimals,
                    ),
                    value = feeUnits.toBigDecimal().movePointLeft(feeToken.decimals),
                ),
                gasLimit = gasLimit,
                coinPriceInToken = tokenGas.nativeToTokenRate.movePointRight(tokenGas.decimals).toBigInteger(),
                feeTransferGasLimit = BigInteger.ZERO,
                baseGas = value.baseGas,
            )
        }

        return TransactionFee.Single(normal = fee)
    }
}