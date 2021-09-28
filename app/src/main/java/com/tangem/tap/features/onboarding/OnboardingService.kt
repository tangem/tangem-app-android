package com.tangem.tap.features.onboarding

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.card.Card
import com.tangem.common.extensions.guard
import com.tangem.common.services.Result
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.TapWorkarounds.isTangemNote
import com.tangem.tap.domain.TapWorkarounds.isTangemWallet
import com.tangem.tap.domain.UrlBitmapLoader
import com.tangem.tap.domain.extensions.getArtworkUrl
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.twins.isTangemTwin
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction
import com.tangem.tap.features.onboarding.redux.OnboardingData
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.persistence.UsedCardsPrefStorage
import com.tangem.tap.store
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class OnboardingService(
    private val tapWalletManager: TapWalletManager,
    private val cardInfoStorage: UsedCardsPrefStorage
) {

    var onReadyToProceed: ((OnboardingData) -> Unit)? = null
        set(value) {
            field = value
            onboardingDataLinker.onReady = { value?.invoke(it) }
        }
    var onFailedToProceedToOnboardingCase: (suspend (ScanNoteResponse) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val onboardingDataLinker = OnboardingDataLinker()

    suspend fun onSuccess(response: ScanNoteResponse) {
        onboardingDataLinker.scanNoteResponse = response
        val card = response.card
        val hasWallets = card.hasWallets()

        if (!card.isTangemNote() && !card.isTangemWallet() && !card.isTangemTwin()) {
            if (hasWallets) {
                cardInfoStorage.activate(card.cardId)
                onFailedToProceedToOnboardingCase?.invoke(response)
                return
            } else {
                cardInfoStorage.activate(card.cardId) // вызвать только после того, как был создан кошелек
                onboardingDataLinker.navigateTo = AppScreen.OnboardingOther
            }
        }
        if (cardInfoStorage.wasActivated(card.cardId)) {
            onFailedToProceedToOnboardingCase?.invoke(response)
            return
        }

        loadCardImage(card) { onboardingDataLinker.cardArtwork = it }

        if (hasWallets) {
            val walletManagerFactory = tapWalletManager.walletManagerFactory
            when {
                card.isTangemNote() -> loadBalanceNote(walletManagerFactory, response)
                card.isTangemWallet() -> loadBalanceWallet(walletManagerFactory, response)
                card.isTangemTwin() -> loadBalanceTwins(walletManagerFactory, response)
            }
        } else {
            onboardingDataLinker.valueOfWalletAmount = BigDecimal.ZERO
        }

        onboardingDataLinker.navigateTo = when {
            card.isTangemNote() -> AppScreen.OnboardingNote
            card.isTangemWallet() -> AppScreen.OnboardingWallet
            card.isTangemTwin() -> AppScreen.OnboardingTwins
            else -> AppScreen.OnboardingOther
        }
    }

    private suspend fun loadBalanceNote(walletManagerFactory: WalletManagerFactory, response: ScanNoteResponse) {
        val walletManager: WalletManager = walletManagerFactory.makePrimaryWalletManager(response).guard {
            onError?.invoke("Can't proceed: primary wallet manager didn't create")
            return
        }
        walletManager.update()
        store.dispatch(OnboardingNoteAction.SetWalletManager(walletManager))

        val wallet = walletManager.wallet
        val valueOfAmount = wallet.amounts[AmountType.Coin]?.value
        if (valueOfAmount == null) {
            onError?.invoke("Amount can't be NULL")
            return
        }

        onboardingDataLinker.valueOfWalletAmount = valueOfAmount
    }

    private suspend fun loadBalanceWallet(walletManagerFactory: WalletManagerFactory, response: ScanNoteResponse) {

    }

    private suspend fun loadBalanceTwins(walletManagerFactory: WalletManagerFactory, response: ScanNoteResponse) {

    }

    private suspend fun loadCardImage(card: Card, callback: (Artwork?) -> Unit) {
        fun loadBitmap(url: String, callback: (Artwork?) -> Unit) {
            UrlBitmapLoader().loadBitmap(url) {
                when (it) {
                    is Result.Success -> callback(Artwork(url, it.data))
                    is Result.Failure -> callback(null)
                }
            }
        }

        when (val cardInfoResult = OnlineCardVerifier().getCardInfo(card.cardId, card.cardPublicKey)) {
            is Result.Success -> {
                val artworkUrl = card.getArtworkUrl(cardInfoResult.data.artwork?.id) ?: Artwork.DEFAULT_IMG_URL
                loadBitmap(artworkUrl, callback)
            }
            is Result.Failure -> loadBitmap(Artwork.DEFAULT_IMG_URL, callback)
        }
    }

}

class OnboardingDataLinker {
    var onReady: ((OnboardingData) -> Unit)? = null

    internal var scanNoteResponse: ScanNoteResponse? = null
        set(value) {
            field = value
            tryToProceed()
        }
    internal var cardArtwork: Artwork? = null
        set(value) {
            field = value
            tryToProceed()
        }
    internal var valueOfWalletAmount: BigDecimal? = null
        set(value) {
            field = value
            tryToProceed()
        }
    internal var navigateTo: AppScreen? = null
        set(value) {
            field = value
            tryToProceed()
        }

    private fun tryToProceed() {
        if (!isReady()) return

        val data = OnboardingData(scanNoteResponse!!, cardArtwork!!, valueOfWalletAmount, navigateTo!!)
        onReady?.invoke(data)
    }

    private fun isReady(): Boolean {
        val objList = mutableListOf(scanNoteResponse, cardArtwork, valueOfWalletAmount, navigateTo)
        return !objList.any { it == null }
    }
}