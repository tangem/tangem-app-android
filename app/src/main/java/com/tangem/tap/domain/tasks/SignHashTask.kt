package com.tangem.tap.domain.tasks

import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.CommandResponse
import com.tangem.operations.sign.SignHashCommand

class TangemSignHashResponse(
    val signature: ByteArray,
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
) : CommandResponse

class SignHashTask(
    private val hash: ByteArray,
    private val publicKey: Wallet.PublicKey,
) : CardSessionRunnable<TangemSignHashResponse> {
    override fun run(session: CardSession, callback: CompletionCallback<TangemSignHashResponse>) {
        SignHashCommand(hash, publicKey.seedKey, publicKey.derivationPath).run(session) { response ->

            when (response) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(TangemSignHashResponse(
                        response.data.signature,
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
