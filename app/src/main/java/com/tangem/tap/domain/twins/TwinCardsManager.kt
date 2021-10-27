package com.tangem.tap.domain.twins

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.Message
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.network.createMoshi
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(private val card: Card, assetReader: AssetReader) {

    private val currentCardId: String = card.cardId

    private var currentCardPublicKey: String? = null
    private var secondCardPublicKey: String? = null

    private val issuerKeyPair: KeyPair = getIssuerKeys(assetReader, card.issuer.publicKey.toHexString())

    suspend fun createFirstWallet(message: Message): CompletionResult<CreateWalletResponse> {
        val response = tangemSdkManager.runTaskAsync(
            CreateFirstTwinWalletTask(), currentCardId, message
        )
        when (response) {
            is CompletionResult.Success -> {
                currentCardPublicKey = response.data.wallet.publicKey.toHexString()
                return response
            }
            is CompletionResult.Failure -> {
                (response.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.CreateWallet,
                        card = card
                    )
                }
                return response
            }
        }

    }


    suspend fun createSecondWallet(
        initialMessage: Message,
        preparingMessage: Message,
        creatingWalletMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        val task = CreateSecondTwinWalletTask(
            firstPublicKey = currentCardPublicKey!!,
            firstCardId = currentCardId,
            issuerKeys = issuerKeyPair,
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage
        )
        val response = tangemSdkManager.runTaskAsync(task, null, initialMessage)
        when (response) {
            is CompletionResult.Success -> {
                secondCardPublicKey = response.data.wallet.publicKey.toHexString()
            }
            is CompletionResult.Failure -> {
                (response.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.CreateWallet,
                        card = card
                    )
                }
            }
        }
        return response
    }

    suspend fun complete(message: Message): Result<ScanResponse> {
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
                        card = card
                    )
                }
                Result.failure(response.error)
            }
        }
    }

    companion object {
        private fun getIssuerKeys(reader: AssetReader, publicKey: String): KeyPair {
            val issuer = getIssuers(reader).first { it.publicKey == publicKey }
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

        private fun getIssuers(reader: AssetReader): List<Issuer> {
            val file = reader.readAssetAsString("tangem-app-config/issuers")
            return getAdapter().fromJson(file)!!
        }
    }
}

interface AssetReader {
    fun readAssetAsString(name: String): String
}

private class Issuer(
    val id: String,
    val privateKey: String,
    val publicKey: String,
)