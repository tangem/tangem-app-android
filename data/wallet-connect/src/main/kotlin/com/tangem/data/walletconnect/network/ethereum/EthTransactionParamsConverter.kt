package com.tangem.data.walletconnect.network.ethereum

import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthTransaction
import com.tangem.utils.converter.Converter

internal class EthTransactionParamsConverter(
    private val context: WcMethodUseCaseContext,
) : Converter<WcEthTransactionParams, WcEthTransaction?> {

    override fun convert(value: WcEthTransactionParams): WcEthTransaction? {
        val dAppFee = WcEthTxHelper.getDAppFee(context.network, value)
        val transactionData = WcEthTxHelper.createTransactionData(
            dAppFee = dAppFee,
            network = context.network,
            txParams = value,
        )
        transactionData ?: return null
        return WcEthTransaction(
            dAppFee = dAppFee,
            transactionData = transactionData,
        )
    }
}