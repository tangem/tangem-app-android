package com.tangem.tap.domain.tasks

import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.CommandResponse
import com.tangem.operations.sign.SignHashesCommand

class TangemSignHashesResponse(
    val signatures: List<ByteArray>,
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
) : CommandResponse

class SignHashesTask(
    private val hashes: Collection<ByteArray>,
    private val publicKey: Wallet.PublicKey,
) : CardSessionRunnable<TangemSignHashesResponse> {
    override fun run(session: CardSession, callback: CompletionCallback<TangemSignHashesResponse>) {
        SignHashesCommand(hashes.toTypedArray(), publicKey.seedKey, publicKey.derivationPath).run(session) { response ->
            when (response) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(TangemSignHashesResponse(
                        response.data.signatures,
                        response.data.totalSignedHashes,
                        session.environment.card?.wallet(publicKey.seedKey)?.remainingSignatures
                    )))
                }
                is CompletionResult.Failure ->
                    callback(CompletionResult.Failure(response.error))
            }
        }
    }
}