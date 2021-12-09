package com.tangem.tap.domain

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.tap.domain.tasks.SignHashTask
import com.tangem.tap.domain.tasks.SignHashesTask
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSigner(
    private val tangemSdk: TangemSdk,
    private val initialMessage: Message,
    private val signerCallback: (TangemSignerResponse) -> Unit,
) : TransactionSigner {

    override suspend fun sign(hash: ByteArray, cardId: String, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        return suspendCoroutine { continuation ->
            val command = SignHashTask(hash, publicKey)
            tangemSdk.startSessionWithRunnable(
                runnable = command,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        signerCallback(
                            TangemSignerResponse(
                                result.data.totalSignedHashes,
                                result.data.remainingSignatures
                            )
                        )
                        continuation.resume(CompletionResult.Success(result.data.signature))
                    }
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    override suspend fun sign(hashes: List<ByteArray>, cardId: String, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>> {
        return suspendCoroutine { continuation ->
            val task = SignHashesTask(hashes, publicKey)
            tangemSdk.startSessionWithRunnable(
                runnable = task,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        signerCallback(
                            TangemSignerResponse(
                                result.data.totalSignedHashes,
                                result.data.remainingSignatures
                            )
                        )
                        continuation.resume(CompletionResult.Success(result.data.signatures))
                    }
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}

data class TangemSignerResponse(
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
)