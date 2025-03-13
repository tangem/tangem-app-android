package com.tangem.tap.domain.tasks

import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.CommandResponse
import com.tangem.operations.sign.MultipleSignCommand
import com.tangem.operations.sign.SignData

class TangemMultiSignHashResponse(
    val signatures: Map<ByteArray, ByteArray>,
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
    val batchId: String?,
) : CommandResponse

class MultiSignHashTask(
    private val dataToSign: List<SignData>,
    private val publicKey: Wallet.PublicKey,
    private val pairWalletPublicKey: ByteArray?,
) : CardSessionRunnable<TangemMultiSignHashResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<TangemMultiSignHashResponse>) {
        MultipleSignCommand(dataToSign, publicKey.seedKey).run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val card = session.environment.card
                    callback(
                        CompletionResult.Success(
                            TangemMultiSignHashResponse(
                                signatures = response.data.associate { it.walletPublicKey to it.signature },
                                totalSignedHashes = response.data.last().totalSignedHashes,
                                remainingSignatures = card?.wallet(publicKey.seedKey)?.remainingSignatures,
                                batchId = card?.batchId,
                            ),
                        ),
                    )
                }
                is CompletionResult.Failure -> {
                    when {
                        response.error is TangemSdkError.WalletNotFound && pairWalletPublicKey != null -> {
                            sign(session, pairWalletPublicKey, callback)
                        }
                        else -> callback(CompletionResult.Failure(response.error))
                    }
                }
            }
        }
    }

    private fun sign(
        session: CardSession,
        publicKey: ByteArray,
        callback: CompletionCallback<TangemMultiSignHashResponse>,
    ) {
        MultipleSignCommand(dataToSign, publicKey).run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val card = session.environment.card
                    callback(
                        CompletionResult.Success(
                            TangemMultiSignHashResponse(
                                signatures = response.data.associate { it.walletPublicKey to it.signature },
                                totalSignedHashes = response.data.last().totalSignedHashes,
                                remainingSignatures = card?.wallet(publicKey)?.remainingSignatures,
                                batchId = card?.batchId,
                            ),
                        ),
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(response.error))
                }
            }
        }
    }
}