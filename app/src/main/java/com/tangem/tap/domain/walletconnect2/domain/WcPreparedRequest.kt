package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.tap.domain.walletconnect2.domain.models.BnbData
import com.tangem.tap.features.details.redux.walletconnect.WcPersonalSignData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionData

sealed class WcPreparedRequest(
    open val preparedRequestData: Any,
    val topic: String,
    val requestId: Long,
    val derivationPath: String?,
) {
    class EthSign(
        override val preparedRequestData: WcPersonalSignData,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)

    class EthTransaction(
        override val preparedRequestData: WcTransactionData,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)

    class BnbTransaction(
        override val preparedRequestData: BnbData,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)

    class SignTransaction(
        override val preparedRequestData: WcGenericTransactionData,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)
}

data class WcGenericTransactionData(
    val hashToSign: ByteArray,
    val dAppName: String,
    val type: TransactionType,
)

enum class TransactionType {
    SOLANA_TX,
}
