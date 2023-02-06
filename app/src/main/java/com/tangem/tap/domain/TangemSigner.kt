package com.tangem.tap.domain

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.domain.common.CardDTO
import com.tangem.tap.domain.tasks.SignHashesTask
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSigner(
    private val card: CardDTO,
    private val tangemSdk: TangemSdk,
    private val initialMessage: Message,
    private val accessCode: String? = null,
    private val signerCallback: (TangemSignerResponse) -> Unit,
) : TransactionSigner {

    override suspend fun sign(
        hashes: List<ByteArray>,
        publicKey: Wallet.PublicKey
    ): CompletionResult<List<ByteArray>> {
        return suspendCoroutine { continuation ->
            val cardId = if (card.backupStatus?.isActive == true) null else card.cardId

            val task = SignHashesTask(hashes, publicKey)
            tangemSdk.startSessionWithRunnable(
                runnable = task,
                cardId = cardId,
                initialMessage = initialMessage,
                accessCode = accessCode,
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

    override suspend fun sign(
        hash: ByteArray,
        publicKey: Wallet.PublicKey
    ): CompletionResult<ByteArray> {
        val result = sign(
            hashes = listOf(hash),
            publicKey = publicKey
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
)
