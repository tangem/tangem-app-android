package com.tangem.tap.domain

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.models.scan.isRing
import com.tangem.tap.domain.tasks.SignHashesTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TangemSigner(
    private val cardId: String?,
    private val tangemSdk: TangemSdk,
    private val initialMessage: Message,
    private val twinKey: TwinKey?,
    private val signerCallback: (TangemSignerResponse) -> Unit,
) : TransactionSigner {

    override suspend fun sign(
        hashes: List<ByteArray>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<List<ByteArray>> {
        return suspendCancellableCoroutine { continuation ->
            val task = SignHashesTask(hashes, publicKey, twinKey?.getPairKey(publicKey.seedKey))

            tangemSdk.startSessionWithRunnable(
                runnable = task,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        signerCallback(
                            TangemSignerResponse(
                                totalSignedHashes = result.data.totalSignedHashes,
                                remainingSignatures = result.data.remainingSignatures,
                                isRing = result.data.batchId?.let(::isRing) ?: false,
                            ),
                        )
                        if (continuation.isActive) {
                            continuation.resume(CompletionResult.Success(result.data.signatures))
                        }
                    }
                    is CompletionResult.Failure ->
                        if (continuation.isActive) {
                            continuation.resume(CompletionResult.Failure(result.error))
                        }
                }
            }
        }
    }

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        val result = sign(
            hashes = listOf(hash),
            publicKey = publicKey,
        )

        return when (result) {
            is CompletionResult.Success -> CompletionResult.Success(result.data.first())
            is CompletionResult.Failure -> CompletionResult.Failure(result.error)
        }
    }
}

data class TangemSignerResponse(
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
    val isRing: Boolean,
)