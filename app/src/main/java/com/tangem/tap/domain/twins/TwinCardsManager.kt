package com.tangem.tap.domain.twins

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager

class TwinCardsManager(card: CardDTO) {

    private val issuersConfigStorage by lazy(mode = LazyThreadSafetyMode.NONE) {
        store.inject(DaggerGraphState::issuersConfigStorage)
    }

    private val firstCardId: String = card.cardId
    private val publicKey: String = card.issuer.publicKey.toHexString()

    private var currentCardPublicKey: String? = null
    private var secondCardPublicKey: String? = null

    suspend fun createFirstWallet(message: Message): CompletionResult<CreateWalletResponse> {
        val response = tangemSdkManager.createFirstTwinWallet(cardId = firstCardId, initialMessage = message)

        if (response is CompletionResult.Success) {
            currentCardPublicKey = response.data.wallet.publicKey.toHexString()
        }

        return response
    }

    suspend fun createSecondWallet(
        initialMessage: Message,
        preparingMessage: Message,
        creatingWalletMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        val response = tangemSdkManager.createSecondTwinWallet(
            firstPublicKey = requireNotNull(currentCardPublicKey),
            firstCardId = firstCardId,
            issuerKeys = getIssuerKeys(),
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage,
            initialMessage = initialMessage,
        )

        if (response is CompletionResult.Success) {
            secondCardPublicKey = response.data.wallet.publicKey.toHexString()
        }

        return response
    }

    suspend fun complete(message: Message): CompletionResult<ScanResponse> {
        val response = tangemSdkManager.finalizeTwin(
            secondCardPublicKey = requireNotNull(secondCardPublicKey).hexToBytes(),
            issuerKeyPair = getIssuerKeys(),
            cardId = firstCardId,
            initialMessage = message,
        )

        return response
    }

    private suspend fun getIssuerKeys(): KeyPair {
        val issuer = issuersConfigStorage.getConfig().first { it.publicKey == publicKey }

        return KeyPair(
            publicKey = issuer.publicKey.hexToBytes(),
            privateKey = issuer.privateKey.hexToBytes(),
        )
    }
}