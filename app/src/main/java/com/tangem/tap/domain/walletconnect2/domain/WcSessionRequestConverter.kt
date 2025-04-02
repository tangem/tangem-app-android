package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.domain.models.BnbData
import com.tangem.tap.domain.walletconnect2.domain.models.EthTransactionData
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.features.details.redux.walletconnect.WcEthTransactionType
import okio.ByteString.Companion.decodeBase64

internal class WcSessionRequestConverter(
    private val blockchainHelper: WcBlockchainHelper,
    private val sessionsRepository: WalletConnectSessionsRepository,
    private val sdkHelper: WalletConnectSdkHelper,
) {

    @Suppress("LongMethod")
    suspend fun prepareRequest(
        sessionRequest: WalletConnectEvents.SessionRequest,
        userWalletId: String,
    ): Result<WcPreparedRequest> = runCatching {
        val networkId = requireNotNull(blockchainHelper.chainIdToNetworkIdOrNull(sessionRequest.chainId.orEmpty())) {
            "Failed to get network ID for chain ID: ${sessionRequest.chainId}"
        }
        val derivationPath = getDerivationPath(
            sessionsRepository = sessionsRepository,
            sessionRequest = sessionRequest,
            userWalletId = userWalletId,
            walletAddress = getWalletAddress(sessionRequest.request),
        )

        when (val request = sessionRequest.request) {
            is WcRequest.EthSendTransaction -> {
                val data = sdkHelper.prepareTransactionData(
                    EthTransactionData(
                        transaction = request.data,
                        networkId = networkId,
                        rawDerivationPath = derivationPath,
                        id = sessionRequest.id,
                        topic = sessionRequest.topic,
                        type = WcEthTransactionType.EthSendTransaction,
                        metaName = sessionRequest.metaName,
                        metaUrl = sessionRequest.metaUrl,
                    ),
                )

                WcPreparedRequest.EthTransaction(
                    preparedRequestData = data,
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.EthSignTransaction -> {
                val data = sdkHelper.prepareTransactionData(
                    EthTransactionData(
                        transaction = request.data,
                        networkId = networkId,
                        rawDerivationPath = derivationPath,
                        id = sessionRequest.id,
                        topic = sessionRequest.topic,
                        type = WcEthTransactionType.EthSignTransaction,
                        metaName = sessionRequest.metaName,
                        metaUrl = sessionRequest.metaUrl,
                    ),
                )

                WcPreparedRequest.EthTransaction(
                    preparedRequestData = data,
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.EthSign -> {
                val data = sdkHelper.prepareDataForPersonalSign(
                    request.data,
                    sessionRequest.topic,
                    sessionRequest.metaName,
                    sessionRequest.id,
                )
                WcPreparedRequest.EthSign(
                    preparedRequestData = data,
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.BnbTrade -> {
                val data = sdkHelper.prepareBnbTradeOrder(request.data)
                WcPreparedRequest.BnbTransaction(
                    BnbData(
                        data = data,
                        topic = sessionRequest.topic,
                        requestId = sessionRequest.id,
                        dAppName = sessionRequest.metaName,
                    ),
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.BnbTransfer -> {
                val data = sdkHelper.prepareBnbTransferOrder(request.data)
                WcPreparedRequest.BnbTransaction(
                    BnbData(
                        data = data,
                        topic = sessionRequest.topic,
                        requestId = sessionRequest.id,
                        dAppName = sessionRequest.metaName,
                    ),
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.SolanaSignRequest -> {
                val transaction = request.data.transaction

                WcPreparedRequest.SolanaSignTransaction(
                    preparedRequestData = GenericTransactionData.SingleHash(
                        hashToSign = transaction.prepareSolanaTransaction(),
                        dAppName = sessionRequest.metaName,
                        type = TransactionType.SOLANA_TX,
                    ),
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            is WcRequest.SolanaSignTransactions -> {
                val transactions = request.data.transactions
                WcPreparedRequest.SolanaSignMultipleTransactions(
                    preparedRequestData = GenericTransactionData.MultipleHashes(
                        hashesToSign = transactions.map { it.prepareSolanaTransaction() }, //
                        dAppName = sessionRequest.metaName,
                        type = TransactionType.SOLANA_TX,
                    ),
                    topic = sessionRequest.topic,
                    requestId = sessionRequest.id,
                    derivationPath = derivationPath,
                )
            }
            else -> throw WalletConnectError.UnsupportedMethod
        }
    }

    /**
     * Input transaction in Base64 string
     */
    private fun String.prepareSolanaTransaction(): ByteArray {
        return this.decodeBase64()
            ?.toByteArray()
            ?.drop(SOLANA_SIGNATURE_PLACEHOLDER_LENGTH)
            ?.toByteArray() ?: ByteArray(0)
    }

    private fun getWalletAddress(request: WcRequest): String? {
        return when (request) {
            is WcRequest.BnbTrade -> request.data.accountNumber
            is WcRequest.BnbTransfer -> request.data.accountNumber
            is WcRequest.EthSendTransaction -> request.data.from
            is WcRequest.EthSignTransaction -> request.data.from
            is WcRequest.EthSign -> request.data.address
            is WcRequest.SolanaSignRequest -> request.data.feePayer
            else -> null
        }
    }

    private suspend fun getDerivationPath(
        sessionsRepository: WalletConnectSessionsRepository,
        sessionRequest: WalletConnectEvents.SessionRequest,
        userWalletId: String,
        walletAddress: String?,
    ): String? {
        return sessionsRepository.loadSessions(userWalletId)
            .firstOrNull { it.topic == sessionRequest.topic }
            ?.accounts?.firstOrNull {
                // if walletAddress == null take first account
                it.chainId == sessionRequest.chainId &&
                    (walletAddress == null || it.walletAddress.lowercase() == walletAddress.lowercase())
            }?.derivationPath
    }

    companion object {
        private const val SOLANA_SIGNATURE_PLACEHOLDER_LENGTH = 65
    }
}