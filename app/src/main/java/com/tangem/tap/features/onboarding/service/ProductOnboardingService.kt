package com.tangem.tap.features.onboarding.service

import android.util.Log
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.card.Card
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.isPositive
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.UrlBitmapLoader
import com.tangem.tap.domain.extensions.getArtworkUrl
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.topup.TradeCryptoHelper
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.reducers.createAddressesData
import com.tangem.tap.persistence.UsedCardsPrefStorage
import com.tangem.tap.store
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
[REDACTED_AUTHOR]
 */
interface OnboardingService {
    val fromScreen: AppScreen
    val scanResponse: ScanNoteResponse

    //    val cardInfoStorage: UsedCardsPrefStorage
    var walletManager: WalletManager?

    var onInitializationProgress: ((ProgressState) -> Unit)?
    var onInitialized: ((AppScreen) -> Unit)?
    var onError: ((TapError) -> Unit)?

    fun initializeOnboarding()
    fun getArtwork(): OnboardingArtwork
    fun getBalance(): OnboardingWalletBalance
    suspend fun updateBalance(): OnboardingWalletBalance
    fun getToUpUrl(): String?
    fun getAddressData(): AddressData?

    fun activationStarted()
    fun activationFinished()
}

abstract class ProductOnboardingService(
    private val cardInfoStorage: UsedCardsPrefStorage,
    private val walletManagerFactory: WalletManagerFactory,
) : OnboardingService {

    override var walletManager: WalletManager? = null

    override var onInitializationProgress: ((ProgressState) -> Unit)? = null
    override var onInitialized: ((AppScreen) -> Unit)? = null
    override var onError: ((TapError) -> Unit)? = null

    protected var isProceeded = false

    protected var loadedCardArtwork: OnboardingArtwork = OnboardingArtwork.loading()
        set(value) {
            field = value
            tryToProceed()
        }

    protected var loadedBalance: OnboardingWalletBalance = OnboardingWalletBalance.loading()
        set(value) {
            field = value
            tryToProceed()
        }

    protected val internalScope = CoroutineScope(Job() + Dispatchers.IO + coroutineExceptionHandler())

    override fun initializeOnboarding() {
        val card = scanResponse.card
        onInitializationProgress?.invoke(ProgressState.Loading)

        internalScope.launch {
            val artwork = loadCardArtwork(card)
            withMainContext {
                loadedCardArtwork = if (artwork == null) OnboardingArtwork.error() else OnboardingArtwork.done(artwork)
            }
        }
        internalScope.launch { updateBalance() }
    }

    override fun getArtwork(): OnboardingArtwork = loadedCardArtwork

    override fun getBalance(): OnboardingWalletBalance = loadedBalance

    override fun getToUpUrl(): String? {
        val wallet = walletManager?.wallet ?: return null
        val config = store.state.globalState.configManager?.config ?: return null
        val defaultAddress = wallet.address

        return TradeCryptoHelper.getUrl(
                TradeCryptoHelper.Action.Buy,
                wallet.blockchain,
                wallet.blockchain.currency,
                defaultAddress,
                config.moonPayApiKey,
                config.moonPayApiSecretKey
        )
    }

    override fun getAddressData(): AddressData? {
        val wallet = walletManager?.wallet ?: return null

        val addressDataList = wallet.createAddressesData()
        return if (addressDataList.isEmpty()) null
        else addressDataList[0]
    }

    protected suspend fun loadCardArtwork(card: Card): Artwork? {
        return when (val cardInfoResult = OnlineCardVerifier().getCardInfo(card.cardId, card.cardPublicKey)) {
            is Result.Success -> {
                val artworkUrl = card.getArtworkUrl(cardInfoResult.data.artwork?.id) ?: Artwork.DEFAULT_IMG_URL
                loadBitmap(artworkUrl)
            }
            is Result.Failure -> {
                loadBitmap(Artwork.DEFAULT_IMG_URL)
            }
        }
    }

    override suspend fun updateBalance(): OnboardingWalletBalance {
        withMainContext { loadedBalance = OnboardingWalletBalance.loading(loadedBalance.value) }

        val walletManager = walletManager ?: walletManagerFactory.makePrimaryWalletManager(scanResponse).guard {
            val customError = TapError.CustomError("Loading cancelled. Cause: wallet manager didn't created")
            loadedBalance = OnboardingWalletBalance.error(customError)
            return loadedBalance
        }

        this.walletManager = walletManager
        val currency = Currency.Blockchain(walletManager.wallet.blockchain)
        val updatedBalance = when (val result = walletManager.safeUpdate()) {
            is Result.Success -> {
                val wallet = walletManager.wallet
                val valueOfAmount = wallet.amounts[AmountType.Coin]?.value
                if (valueOfAmount == null) {
                    val customError = TapError.CustomError("Amount is NULL")
                    OnboardingWalletBalance.criticalError(customError)
                } else {
                    val balance = if (valueOfAmount.isZero()) {
                        OnboardingWalletBalance.done(valueOfAmount, hasForIncomingTransactions(), currency)
                    } else {
                        OnboardingWalletBalance.done(valueOfAmount, false, currency)
                    }
                    balance
                }
            }
            is Result.Failure -> {
                val error = (result.error as? TapError) ?: TapError.UnknownError
                when (error) {
                    is TapError.WalletManagerUpdate.NoAccountError -> OnboardingWalletBalance.error(error)
//                    NoInternetConnection, WalletManagerUpdate.InternalError
                    else -> OnboardingWalletBalance.criticalError(error)
                }
            }
        }
        withMainContext { loadedBalance = updatedBalance }
        return loadedBalance
    }

    override fun activationStarted() {
        cardInfoStorage.activationStarted(scanResponse.card.cardId)

    }

    override fun activationFinished() {
        cardInfoStorage.activationFinished(scanResponse.card.cardId)
    }

    protected open suspend fun hasForIncomingTransactions(): Boolean {
        return false
    }

    protected open fun tryToProceed() {
        if (isProceeded) return

        loadedBalance.criticalError?.let {
            sendError(it)
            return
        }
        if (!isReadyToProceed()) return

        isProceeded = true
        onInitializationProgress?.invoke(ProgressState.Done)
        onInitialized?.invoke(navigateToScreen())
    }

    protected open fun isReadyToProceed(): Boolean {
        return loadedBalance.state != ProgressState.Loading && loadedCardArtwork.state != ProgressState.Loading
    }

    protected fun sendError(error: TapError) {
        onInitializationProgress?.invoke(ProgressState.Error)
        onError?.invoke(error)
    }

    protected abstract fun navigateToScreen(): AppScreen

    private suspend fun loadBitmap(url: String): Artwork? {
        return withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                UrlBitmapLoader().loadBitmap(url) {
                    val result = when (it) {
                        is Result.Success -> Artwork(url, it.data)
                        is Result.Failure -> null
                    }
                    continuation.resume(result)
                }
            }
        }
    }

    private fun coroutineExceptionHandler(): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        StringWriter().apply {
            throwable.printStackTrace(PrintWriter(this))
            Log.e("Coroutine", this.toString())
            store.dispatchDebugErrorNotification(this.toString())
        }
    }
}

data class OnboardingArtwork(
    val value: Artwork? = null,
    val state: ProgressState
) {
    companion object {
        // balance was not loaded at all when any error occurred
        fun error(): OnboardingArtwork = OnboardingArtwork(state = ProgressState.Error)

        // balance is loading
        fun loading(): OnboardingArtwork = OnboardingArtwork(state = ProgressState.Loading)

        // balance was loaded
        fun done(artwork: Artwork): OnboardingArtwork = OnboardingArtwork(artwork, ProgressState.Done)
    }
}

data class OnboardingWalletBalance(
    val value: BigDecimal = BigDecimal.ZERO,
    val currency: Currency.Blockchain = Currency.Blockchain(Blockchain.Unknown),
    val hasIncomingTransaction: Boolean = false,
    val state: ProgressState,
    val error: TapError? = null,
    val criticalError: TapError? = null,
) {

    val amountToCreateAccount: String?
        get() = if (error is TapError.WalletManagerUpdate.NoAccountError) error.customMessage else null

    companion object {
        fun error(error: TapError): OnboardingWalletBalance = OnboardingWalletBalance(
                state = ProgressState.Error,
                error = error
        )

        fun criticalError(error: TapError): OnboardingWalletBalance = OnboardingWalletBalance(
                state = ProgressState.Error,
                criticalError = error
        )

        fun loading(value: BigDecimal = BigDecimal.ZERO): OnboardingWalletBalance = OnboardingWalletBalance(
                value,
                state = ProgressState.Loading
        )

        fun done(value: BigDecimal, hasTransactions: Boolean, currency: Currency.Blockchain): OnboardingWalletBalance =
                OnboardingWalletBalance(value, currency, hasTransactions, ProgressState.Done)
    }
}

fun ProductOnboardingService.balanceIsToppedUp(): Boolean {
    val balance = getBalance()
    return balance.value.isPositive() || balance.hasIncomingTransaction
}