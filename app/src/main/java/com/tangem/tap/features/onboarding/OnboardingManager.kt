package com.tangem.tap.features.onboarding

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.operations.attestation.CardVerifyAndGetInfo
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.tap.common.extensions.isPositive
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.getOrLoadCardArtworkUrl
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.hasPendingTransactions
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.persistence.UsedCardsPrefStorage
import timber.log.Timber
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 17/10/2021.
 */
class OnboardingManager(
    var scanResponse: ScanResponse,
    val usedCardsPrefStorage: UsedCardsPrefStorage,
) {

    var cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null
        private set

    suspend fun loadArtworkUrl(): String {
        val cardInfo = cardInfo
            ?: OnlineCardVerifier().getCardInfo(scanResponse.card.cardId, scanResponse.card.cardPublicKey)
        this.cardInfo = cardInfo
        return scanResponse.card.getOrLoadCardArtworkUrl(cardInfo)
    }

    suspend fun updateBalance(walletManager: WalletManager): OnboardingWalletBalance {
        val balance = when (val result = walletManager.safeUpdate()) {
            is Result.Success -> {
                val wallet = walletManager.wallet
                val valueOfAmount = wallet.amounts[AmountType.Coin]?.value
                if (valueOfAmount == null) {
                    val customError = TapError.CustomError("Amount is NULL")
                    OnboardingWalletBalance.criticalError(customError)
                } else {
                    val balance = if (valueOfAmount.isZero()) {
                        OnboardingWalletBalance.done(valueOfAmount, wallet.hasPendingTransactions())
                    } else {
                        OnboardingWalletBalance.done(valueOfAmount, false)
                    }
                    balance
                }
            }
            is Result.Failure -> {
                val error = result.error as? TapError ?: TapError.UnknownError
                when (error) {
                    is TapError.WalletManager.NoAccountError -> OnboardingWalletBalance.error(error)
                    // NoInternetConnection, WalletManager.InternalError
                    else -> {
                        Timber.e(error.localizedMessage)
                        OnboardingWalletBalance.criticalError(TapError.WalletManager.BlockchainIsUnreachableTryLater)
                    }
                }
            }
        }

        return balance.copy(
            currency = Currency.Blockchain(
                blockchain = walletManager.wallet.blockchain,
                derivationPath = walletManager.wallet.publicKey.derivationPath?.rawPath,
            ),
        )
    }

    fun activationStarted(cardId: String) {
        usedCardsPrefStorage.activationStarted(cardId)
    }

    fun activationFinished(cardId: String) {
        usedCardsPrefStorage.activationFinished(cardId)
    }

    fun isActivationFinished(cardId: String): Boolean {
        return usedCardsPrefStorage.isActivationFinished(cardId)
    }

    fun isActivationStarted(cardId: String): Boolean {
        return usedCardsPrefStorage.isActivationStarted(cardId)
    }
}

data class OnboardingWalletBalance(
    val value: BigDecimal = BigDecimal.ZERO,
    val currency: Currency = Currency.Blockchain(Blockchain.Unknown, null),
    val hasIncomingTransaction: Boolean = false,
    val state: ProgressState,
    val error: TapError? = null,
    val criticalError: TapError? = null,
) {

    val amountToCreateAccount: String?
        get() = if (error is TapError.WalletManager.NoAccountError) error.customMessage else null

    fun balanceIsToppedUp(): Boolean = value.isPositive() || hasIncomingTransaction

    companion object {
        fun error(error: TapError): OnboardingWalletBalance = OnboardingWalletBalance(
            state = ProgressState.Error,
            error = error,
        )

        fun criticalError(error: TapError): OnboardingWalletBalance = OnboardingWalletBalance(
            state = ProgressState.Error,
            criticalError = error,
        )

        fun loading(value: BigDecimal = BigDecimal.ZERO): OnboardingWalletBalance = OnboardingWalletBalance(
            value,
            state = ProgressState.Loading,
        )

        fun done(value: BigDecimal, hasTransactions: Boolean): OnboardingWalletBalance = OnboardingWalletBalance(
            value,
            hasIncomingTransaction = hasTransactions,
            state = ProgressState.Done,
        )
    }
}
