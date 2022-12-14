package com.tangem.tap.domain.twins

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.Message
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.common.AssetReader
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(
    card: CardDTO,
    assetReader: AssetReader,
) {
    private val firstCardId: String = card.cardId
    private var secondCardId: String? = null

    private var currentCardPublicKey: String? = null
    var secondCardPublicKey: String? = null
        private set

    private val issuerKeyPair: KeyPair = getIssuerKeys(assetReader, card.issuer.publicKey.toHexString())

    suspend fun createFirstWallet(message: Message): CompletionResult<CreateWalletResponse> {
        val response = tangemSdkManager.runTaskAsync(
            runnable = CreateFirstTwinWalletTask(firstCardId),
            cardId = firstCardId,
            initialMessage = message,
        )
        when (response) {
            is CompletionResult.Success -> currentCardPublicKey = response.data.wallet.publicKey.toHexString()
            is CompletionResult.Failure -> {}
        }
        return response
    }

    suspend fun createSecondWallet(
        initialMessage: Message,
        preparingMessage: Message,
        creatingWalletMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        val task = CreateSecondTwinWalletTask(
            firstPublicKey = currentCardPublicKey!!,
            firstCardId = firstCardId,
            issuerKeys = issuerKeyPair,
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage,
        )
        val response = tangemSdkManager.runTaskAsync(task, null, initialMessage)
        when (response) {
            is CompletionResult.Success -> {
                secondCardPublicKey = response.data.wallet.publicKey.toHexString()
                secondCardId = response.data.cardId
            }
            is CompletionResult.Failure -> {}
        }
        return response
    }

    suspend fun complete(message: Message): Result<ScanResponse> {
        val response = tangemSdkManager.runTaskAsync(
            runnable = FinalizeTwinTask(secondCardPublicKey!!.hexToBytes(), issuerKeyPair),
            cardId = firstCardId,
            initialMessage = message,
        )
        return when (response) {
            is CompletionResult.Success -> Result.Success(response.data)
            is CompletionResult.Failure -> Result.fromTangemSdkError(response.error)
        }
    }

    companion object {
        private fun getIssuerKeys(reader: AssetReader, publicKey: String): KeyPair {
            val issuer = getIssuers(reader).first { it.publicKey == publicKey }
            return KeyPair(
                publicKey = issuer.publicKey.hexToBytes(),
                privateKey = issuer.privateKey.hexToBytes(),
            )
        }

        private fun getAdapter(): JsonAdapter<List<Issuer>> {
            return MoshiConverter.defaultMoshi().adapter(
                Types.newParameterizedType(List::class.java, Issuer::class.java),
            )
        }

        private fun getIssuers(reader: AssetReader): List<Issuer> {
            val file = reader.readAssetAsString("tangem-app-config/issuers")
            return getAdapter().fromJson(file)!!
        }
    }
}

private class Issuer(
    val privateKey: String,
    val publicKey: String,
)
