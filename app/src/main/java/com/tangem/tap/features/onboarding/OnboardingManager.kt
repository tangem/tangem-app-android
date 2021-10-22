package com.tangem.tap.features.onboarding

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.extensions.isPositive
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.UrlBitmapLoader
import com.tangem.tap.domain.extensions.getOrLoadCardArtworkUrl
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.wallet.models.hasPendingTransactions
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.persistence.UsedCardsPrefStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
[REDACTED_AUTHOR]
 */
class OnboardingManager(
    var scanResponse: ScanResponse,
    val usedCardsPrefStorage: UsedCardsPrefStorage,
) {

    var cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null
        private set

    suspend fun loadArtwork(): Artwork {
        cardInfo = OnlineCardVerifier().getCardInfo(scanResponse.card.cardId, scanResponse.card.cardPublicKey)
        return loadArtwork(scanResponse.card.getOrLoadCardArtworkUrl(cardInfo))
    }

    suspend fun loadArtwork(url: String): Artwork {
        return withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                UrlBitmapLoader().loadBitmap(url) {
                    val result = when (it) {
                        is Result.Success -> Artwork(url, it.data)
                        is Result.Failure -> Artwork(url, null)
                    }
                    continuation.resume(result)
                }
            }
        }
    }

    suspend fun updateBalance(walletManager: WalletManager): OnboardingWalletBalance {
        return when (val result = walletManager.safeUpdate()) {
            is Result.Success -> {
                val wallet = walletManager.wallet
                val valueOfAmount = wallet.amounts[AmountType.Coin]?.value
                if (valueOfAmount == null) {
                    val customError = TapError.CustomError("Amount is NULL")
                    OnboardingWalletBalance.criticalError(customError)
                } else {
                    val currency = Currency.Blockchain(walletManager.wallet.blockchain)
                    val balance = if (valueOfAmount.isZero()) {
                        OnboardingWalletBalance.done(valueOfAmount, wallet.hasPendingTransactions(), currency)
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
                    // NoInternetConnection, WalletManagerUpdate.InternalError
                    else -> OnboardingWalletBalance.criticalError(error)
                }
            }
        }
    }

    fun activationStarted(cardId: String) {
        usedCardsPrefStorage.activationStarted(cardId)
    }

    fun activationFinished(cardId: String) {
        usedCardsPrefStorage.activationFinished(cardId)
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

    fun balanceIsToppedUp(): Boolean = value.isPositive() || hasIncomingTransaction

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