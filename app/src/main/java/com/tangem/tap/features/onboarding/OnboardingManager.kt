package com.tangem.tap.features.onboarding

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.common.services.Result
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.attestation.api.models.CardVerifyAndGetInfo
import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.getOrLoadCardArtworkUrl
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.domain.model.hasPendingTransactions
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.utils.extensions.isPositive
import timber.log.Timber
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@Deprecated("Remove when navigation refactoring will be implemented")
class OnboardingManager(var scanResponse: ScanResponse) {

    private val cardRepository: CardRepository by lazy { store.inject(DaggerGraphState::cardRepository) }

    private val onlineCardVerifier: OnlineCardVerifier by lazy { store.inject(DaggerGraphState::onlineCardVerifier) }

    private var cardInfo: Result<CardVerifyAndGetInfo.Response.Item>? = null

    suspend fun loadArtworkUrl(): String {
        val cardInfo = cardInfo
            ?: onlineCardVerifier.getCardInfo(scanResponse.card.cardId, scanResponse.card.cardPublicKey)

        this.cardInfo = cardInfo

        return scanResponse.card.getOrLoadCardArtworkUrl(cardInfo, onlineCardVerifier)
    }

    suspend fun updateBalance(walletManager: WalletManager): OnboardingWalletBalance {
        val isDemoCard = scanResponse.isDemoCard()
        val balance = when (val result = walletManager.safeUpdate(isDemoCard)) {
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

    suspend fun startActivation(cardId: String) {
        cardRepository.startCardActivation(cardId)
    }

    suspend fun finishActivation(cardId: String) {
        cardRepository.finishCardActivation(cardId)
    }

    suspend fun finishActivation(cardIds: List<String>) {
        cardRepository.finishCardsActivation(cardIds)
    }

    suspend fun isActivationStarted(cardId: String): Boolean = cardRepository.isActivationStarted(cardId)

    suspend fun isActivationFinished(cardId: String): Boolean = cardRepository.isActivationFinished(cardId)

    suspend fun isActivationInProgress(cardId: String): Boolean = cardRepository.isActivationInProgress(cardId)
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