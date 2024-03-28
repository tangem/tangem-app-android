package com.tangem.tap.domain.walletconnect.extensions

import com.tangem.tap.domain.walletconnect2.domain.WcEthereumTransaction
import com.tangem.tap.domain.walletconnect2.domain.WcSignMessage
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction

internal fun WCPeerMeta.isDappSupported(): Boolean {
    return !unsupportedDappsList.any { this.url.contains(it) }
}

private val unsupportedDappsList: List<String> = listOf("dydx.exchange")

internal fun WCEthereumTransaction.toWcEthTransaction(): WcEthereumTransaction {
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

internal fun WCEthereumSignMessage.toWcEthereumSignMessage(): WcSignMessage {
    return WcSignMessage(
        raw = raw,
        type = when (type) {
            WCEthereumSignMessage.WCSignType.MESSAGE -> WcSignMessage.WCSignType.MESSAGE
            WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE -> WcSignMessage.WCSignType.PERSONAL_MESSAGE
            WCEthereumSignMessage.WCSignType.TYPED_MESSAGE -> WcSignMessage.WCSignType.TYPED_MESSAGE
        },
    )
}
