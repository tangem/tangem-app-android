package com.tangem.tap.domain.twins

import com.tangem.KeyPair
import com.tangem.Message
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(private val scanNoteResponse: ScanNoteResponse) {

    private val currentCardId: String = scanNoteResponse.card.cardId

    private var currentCardPublicKey: String? = null
    private var secondCardPublicKey: String? = null

    suspend fun createFirstWallet(message: Message): SimpleResult {
        val response = tangemSdkManager.runTaskAsync(
                CreateFirstTwinWalletTask(), currentCardId, message
        )
        when (response) {
            is CompletionResult.Success -> {
                currentCardPublicKey = response.data.walletPublicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> return SimpleResult.failure(response.error)
        }

    }


    suspend fun createSecondWallet(
            initialMessage: Message,
            preparingMessage: Message,
            creatingWalletMessage: Message
    ): SimpleResult {
        val task = CreateSecondTwinWalletTask(
            firstPublicKey = currentCardPublicKey!!,
            firstCardId = currentCardId,
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage
        )
        val response = tangemSdkManager.runTaskAsync(
                task, null, initialMessage
        )
        when (response) {
            is CompletionResult.Success -> {
                secondCardPublicKey = response.data.walletPublicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> return SimpleResult.failure(response.error)
        }

    }

    suspend fun complete(message: Message): Result<ScanNoteResponse> {
        val response = tangemSdkManager.runTaskAsync(
                FinalizeTwinTask(secondCardPublicKey!!.hexToBytes(), issuerKeys),
                currentCardId, message
        )
        return when (response) {
            is CompletionResult.Success -> Result.Success(response.data)
            is CompletionResult.Failure -> Result.failure(response.error)
        }
    }

    companion object {
        val issuerKeys = KeyPair(
                privateKey = "F9F4C50636C9E6FC65F92655BD5C21C85A5F6A34DCD0F1E75FCEA1980FE242F5".hexToBytes(),
                publicKey = ("048196AA4B410AC44A3B9CCE18E7BE226AEA070ACC83A9CF67540F" +
                        "AC49AF25129F6A538A28AD6341358E3C4F9963064F" +
                        "7E365372A651D374E5C23CDD37FD099BF2").hexToBytes()
        )

        fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
            if (issuerData.size < 65) return false
            val publicKey = issuerData.sliceArray(0 until 65)
            val signedKey = issuerData.sliceArray(65 until issuerData.size)
            return (cardWalletPublicKey != null &&
                    CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey))
        }
    }
}