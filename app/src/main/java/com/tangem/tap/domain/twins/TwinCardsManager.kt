package com.tangem.tap.domain.twins

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.Message
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.readAssetAsString
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.network.createMoshi
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(private val scanNoteResponse: ScanNoteResponse, context: Context) {

    private val currentCardId: String = scanNoteResponse.card.cardId

    private var currentCardPublicKey: String? = null
    private var secondCardPublicKey: String? = null

    private val issuerKeyPair: KeyPair = getIssuerKeys(
        context, scanNoteResponse.card.issuer.publicKey.toHexString()
    )

    suspend fun createFirstWallet(message: Message): SimpleResult {
        val response = tangemSdkManager.runTaskAsync(
            CreateFirstTwinWalletTask(), currentCardId, message
        )
        when (response) {
            is CompletionResult.Success -> {
                currentCardPublicKey = response.data.wallet.publicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> {
                (response.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.CreateWallet,
                        card = scanNoteResponse.card
                    )
                }
                return SimpleResult.failure(response.error)
            }
        }

    }


    suspend fun createSecondWallet(
        initialMessage: Message,
        preparingMessage: Message,
        creatingWalletMessage: Message,
    ): SimpleResult {
        val task = CreateSecondTwinWalletTask(
            firstPublicKey = currentCardPublicKey!!,
            firstCardId = currentCardId,
            issuerKeys = issuerKeyPair,
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage
        )
        val response = tangemSdkManager.runTaskAsync(
                task, null, initialMessage
        )
        when (response) {
            is CompletionResult.Success -> {
                secondCardPublicKey = response.data.wallet.publicKey.toHexString()
                return SimpleResult.Success
            }
            is CompletionResult.Failure -> {
                (response.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.CreateWallet,
                        card = scanNoteResponse.card
                    )
                }
                return SimpleResult.failure(response.error)
            }
        }

    }

    suspend fun complete(message: Message): Result<ScanNoteResponse> {
        val response = tangemSdkManager.runTaskAsync(
            FinalizeTwinTask(secondCardPublicKey!!.hexToBytes(), issuerKeyPair),
            currentCardId, message
        )
        return when (response) {
            is CompletionResult.Success -> Result.Success(response.data)
            is CompletionResult.Failure -> {
                (response.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.WriteIssuerData,
                        card = scanNoteResponse.card
                    )
                }
                Result.failure(response.error)
            }
        }
    }

    companion object {
        fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
            if (issuerData.size < 65) return false
            val publicKey = issuerData.sliceArray(0 until 65)
            val signedKey = issuerData.sliceArray(65 until issuerData.size)
            return (cardWalletPublicKey != null &&
                    CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey))
        }

        private fun getIssuerKeys(context: Context, publicKey: String): KeyPair {
            val issuer = getIssuers(context).first { it.publicKey == publicKey }
            return KeyPair(
                publicKey = issuer.publicKey.hexToBytes(),
                privateKey = issuer.privateKey.hexToBytes()
            )
        }

        private fun getAdapter(): JsonAdapter<List<Issuer>> {
            return createMoshi().adapter(
                Types.newParameterizedType(List::class.java, Issuer::class.java)
            )
        }

        private fun getIssuers(context: Context): List<Issuer> {
            val file = context.readAssetAsString("tangem-app-config/issuers")
            return getAdapter().fromJson(file)!!
        }
    }
}

private class Issuer(
    val id: String,
    val privateKey: String,
    val publicKey: String,
)