package com.tangem.tap.domain.walletconnect.extensions

import com.tangem.tap.domain.walletconnect2.domain.WcEthereumSignMessage
import com.tangem.tap.domain.walletconnect2.domain.WcEthereumTransaction
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction

fun WCPeerMeta.isDappSupported(): Boolean {
    return !unsupportedDappsList.any { this.url.contains(it) }
}

private val unsupportedDappsList: List<String> = listOf("dydx.exchange")

fun WCEthereumTransaction.toWcEthTransaction(): WcEthereumTransaction {
    return WcEthereumTransaction(
        from = from,
        to = to,
        nonce = nonce,
        gasPrice = gasPrice,
        maxFeePerGas = maxFeePerGas,
        maxPriorityFeePerGas = maxPriorityFeePerGas,
        gas = gas,
        gasLimit = gasLimit,
        value = value,
        data = data,
    )
}

fun WCEthereumSignMessage.toWcEthereumSignMessage(): WcEthereumSignMessage {
    return WcEthereumSignMessage(
        raw = raw,
        type = when (type) {
            WCEthereumSignMessage.WCSignType.MESSAGE -> WcEthereumSignMessage.WCSignType.MESSAGE
            WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE -> WcEthereumSignMessage.WCSignType.PERSONAL_MESSAGE
            WCEthereumSignMessage.WCSignType.TYPED_MESSAGE -> WcEthereumSignMessage.WCSignType.TYPED_MESSAGE
        },
    )
}