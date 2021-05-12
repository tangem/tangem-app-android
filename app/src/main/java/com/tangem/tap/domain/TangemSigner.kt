package com.tangem.tap.domain

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.commands.wallet.WalletIndex
import com.tangem.common.CompletionResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSigner(
    private val tangemSdk: TangemSdk,
    private val initialMessage: Message,
    private val signerCallback: (SignResponse) -> Unit
) : TransactionSigner {

    override suspend fun sign(
        hash: ByteArray,
        cardId: String,
        walletPublicKey: ByteArray
    ): CompletionResult<ByteArray> =
        suspendCoroutine { continuation ->
            val command = SignCommand(arrayOf(hash), WalletIndex.PublicKey(walletPublicKey))
            tangemSdk.startSessionWithRunnable(
                runnable = command,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        signerCallback(result.data)
                        continuation.resume(CompletionResult.Success(result.data.signatures.first()))
                    }
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }


    override suspend fun sign(
        hashes: List<ByteArray>,
        cardId: String,
        walletPublicKey: ByteArray
    ): CompletionResult<List<ByteArray>> =
        suspendCoroutine { continuation ->
            val command = SignCommand(hashes.toTypedArray(), WalletIndex.PublicKey(walletPublicKey))
            tangemSdk.startSessionWithRunnable(
                runnable = command,
                cardId = cardId,
                initialMessage = initialMessage,
            ) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        signerCallback(result.data)
                        continuation.resume(CompletionResult.Success(result.data.signatures))
                    }
                    is CompletionResult.Failure ->
                        continuation.resume(CompletionResult.Failure(result.error))
                }
            }
        }
}