package com.tangem.tap.domain.tasks

import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.CommandResponse
import com.tangem.operations.sign.SignHashesCommand

class TangemSignHashesResponse(
    val signatures: List<ByteArray>,
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
    val batchId: String?,
) : CommandResponse

class SignHashesTask(
    private val hashes: Collection<ByteArray>,
    private val publicKey: Wallet.PublicKey,
    private val pairWalletPublicKey: ByteArray?,
) : CardSessionRunnable<TangemSignHashesResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<TangemSignHashesResponse>) {
        SignHashesCommand(hashes.toTypedArray(), publicKey.seedKey, publicKey.derivationPath).run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val card = session.environment.card
                    callback(
                        CompletionResult.Success(
                            TangemSignHashesResponse(
                                signatures = response.data.signatures,
                                totalSignedHashes = response.data.totalSignedHashes,
                                remainingSignatures = card?.wallet(publicKey.seedKey)?.remainingSignatures,
                                batchId = card?.batchId,
                            ),
                        ),
                    )
                }
                is CompletionResult.Failure -> {
                    when {
                        response.error is TangemSdkError.WalletNotFound && pairWalletPublicKey != null -> {
                            sign(session, pairWalletPublicKey, publicKey.derivationPath, callback)
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
        derivationPath: DerivationPath?,
        callback: CompletionCallback<TangemSignHashesResponse>,
    ) {
        SignHashesCommand(hashes.toTypedArray(), publicKey, derivationPath).run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    val card = session.environment.card
                    callback(
                        CompletionResult.Success(
                            TangemSignHashesResponse(
                                signatures = response.data.signatures,
                                totalSignedHashes = response.data.totalSignedHashes,
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