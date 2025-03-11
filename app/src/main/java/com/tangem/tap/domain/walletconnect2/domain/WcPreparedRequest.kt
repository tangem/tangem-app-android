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

    class SolanaSignTransaction(
        override val preparedRequestData: GenericTransactionData.SingleHash,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)

    class SolanaSignMultipleTransactions(
        override val preparedRequestData: GenericTransactionData.MultipleHashes,
        topic: String,
        requestId: Long,
        derivationPath: String?,
    ) : WcPreparedRequest(preparedRequestData, topic, requestId, derivationPath)
}

sealed interface GenericTransactionData {
    val dAppName: String
    val type: TransactionType

    data class SingleHash(
        val hashToSign: ByteArray,
        override val dAppName: String,
        override val type: TransactionType,
    ) : GenericTransactionData

    data class MultipleHashes(
        val hashesToSign: List<ByteArray>,
        override val dAppName: String,
        override val type: TransactionType,
    ) : GenericTransactionData
}

enum class TransactionType {
    SOLANA_TX,
}