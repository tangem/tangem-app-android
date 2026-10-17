package com.tangem.features.tangempay.card.gpay

import android.content.Context
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.details.impl.R
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val LOG_TAG = "TangemPay: GoogleWallet"
private const val LAST_DIGITS_LENGTH = 4

@Suppress("LongParameterList")
internal class TangemPayAddToGoogleWalletController @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val userWalletId: UserWalletId,
    @ApplicationContext private val context: Context,
    private val provisioner: GoogleWalletProvisioner,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
    featureToggles: TangemPayFeatureToggles,
) {

    private val logger = TangemLogger.withTag(LOG_TAG)

    private val isEnabled = featureToggles.isGoogleWalletPushProvisioningEnabled

    @RemoveWithToggle(
        toggleName = "TWI_1062_GOOGLE_WALLET_PUSH_PROVISIONING_ENABLED",
        description = "debug switch, drop TWI_1062_GOOGLE_WALLET_BUTTON_ALWAYS_SHOWN and inline false",
    )
    private val isButtonAlwaysShown = featureToggles.isGoogleWalletButtonAlwaysShown

    val entry: StateFlow<Entry>
        field = MutableStateFlow<Entry>(Entry.Hidden)

    private var card: TangemPayCard? = null
    private var frozenState = TangemPayCardFrozenState.Pending
    private var walletStatus: CardStatus? = null
    private var isCardChecked = false
    private var isStarted = false
    private var isAddingCard = false

    private val statusCheckJobHolder = JobHolder()
    private val walletChangesJobHolder = JobHolder()

    fun selectCard(card: TangemPayCard?) {
        if (card?.id != this.card?.id) {
            statusCheckJobHolder.cancel()
            walletStatus = null
            isCardChecked = false
        }
        this.card = card
        if (!isCardChecked) checkWalletStatus()
        updateEntry()
    }

    fun updateFrozenState(frozenState: TangemPayCardFrozenState) {
        this.frozenState = frozenState
        updateEntry()
    }

    fun onStart() {
        isStarted = true
        if (!isEnabled) return
        if (!statusCheckJobHolder.isActive) checkWalletStatus()
        provisioner.walletChanges
            .onEach { checkWalletStatus() }
            .launchIn(scope)
            .saveIn(walletChangesJobHolder)
    }

    fun onStop() {
        isStarted = false
        walletChangesJobHolder.cancel()
    }

    fun onAddClick() {
        val card = card ?: return
        if (entry.value !is Entry.Button || isAddingCard) return
        if (provisioner.isAdditionInProgress) {
            logger.i("Another card is being added to Google Wallet, tap ignored")
            return
        }

        logger.i("Add to Google Wallet tapped, card status: $walletStatus")
        analytics.send(TangemPayAnalyticsEvents.AddToWalletClicked())
        val isVerificationPending = walletStatus == CardStatus.Incomplete
        setAddingCard(isAddingCard = true)
        scope.launch {
            try {
                val result = if (isVerificationPending) {
                    provisioner.resumeAddingCard(
                        lastFourDigits = card.lastDigits,
                        displayName = card.walletDisplayName(),
                    )
                } else {
                    pushCard(card)
                }
                if (result != null) onAddResult(card = card, result = result)
            } finally {
                setAddingCard(isAddingCard = false)
            }
        }
    }

    private suspend fun pushCard(card: TangemPayCard): AddResult? {
        val payEnvironment = if (cardDetailsRepository.isProductionEnvironment()) Environment.PROD else Environment.TEST
        if (payEnvironment != provisioner.environment) {
            logger.e("Google Wallet SDK environment ${provisioner.environment} differs from Tangem Pay $payEnvironment")
            showError()
            return null
        }

        val provisioningData = cardDetailsRepository.getProvisioningData(userWalletId = userWalletId, cardId = card.id)
            .getOrElse { error ->
                logger.e("Failed to get the card provisioning data: $error")
                if (error.isRetryable()) showNetworkError() else showError()
                return null
            }

        if (!isStarted) {
            logger.i("Card screen stopped while the card data was loading, adding skipped")
            return null
        }

        return provisioner.addCard(
            GoogleWalletProvisioner.Card(
                processorCardId = provisioningData.processorCardId,
                cardSecret = provisioningData.cardSecret,
                lastFourDigits = card.lastDigits,
                displayName = card.walletDisplayName(),
                environment = payEnvironment,
            ),
        )
    }

    private fun onAddResult(card: TangemPayCard, result: AddResult) {
        when (result) {
            AddResult.Added, AddResult.AlreadyAdded -> markAsAdded(card)
            AddResult.Cancelled -> Unit
            is AddResult.Failed -> showFailure(result.reason)
        }
        checkWalletStatus()
    }

    private fun markAsAdded(addedCard: TangemPayCard) {
        if (addedCard.id != card?.id) return
        walletStatus = CardStatus.Added
        updateEntry()
    }

    private fun showFailure(reason: FailureReason) {
        when (reason) {
            FailureReason.ADDITION_IN_PROGRESS,
            FailureReason.TOKENIZATION_DECLINED,
            -> Unit
            FailureReason.NETWORK_ERROR -> showNetworkError()
            FailureReason.TOO_MANY_ATTEMPTS -> showRateLimitError()
            FailureReason.UNSUPPORTED -> showWalletUnavailableError()
            FailureReason.NO_ACTIVITY,
            FailureReason.ENVIRONMENT_MISMATCH,
            FailureReason.TOKEN_LOOKUP_FAILED,
            FailureReason.NO_TOKEN_AWAITING_VERIFICATION,
            FailureReason.GOOGLE_WALLET_ERROR,
            FailureReason.MEAWALLET_ERROR,
            FailureReason.UNEXPECTED_ERROR,
            -> showError()
        }
    }

    private fun showError() {
        uiMessageSender.send(TangemPayMessagesFactory.createAddToGoogleWalletError())
    }

    private fun showNetworkError() {
        uiMessageSender.send(TangemPayMessagesFactory.createAddToGoogleWalletNetworkError())
    }

    private fun showRateLimitError() {
        uiMessageSender.send(TangemPayMessagesFactory.createAddToGoogleWalletRateLimitError())
    }

    private fun showWalletUnavailableError() {
        uiMessageSender.send(TangemPayMessagesFactory.createAddToGoogleWalletUnavailableError())
    }

    private fun checkWalletStatus() {
        val checkedCard = card?.takeIf { isEnabled && it.canBeAddedToWallet() } ?: return
        isCardChecked = true
        scope.launch {
            walletStatus = provisioner.getCardStatus(lastFourDigits = checkedCard.lastDigits)
            updateEntry()
        }.saveIn(statusCheckJobHolder)
    }

    private fun setAddingCard(isAddingCard: Boolean) {
        this.isAddingCard = isAddingCard
        updateEntry()
    }

    private fun updateEntry() {
        entry.value = resolveEntry()
    }

    private fun resolveEntry(): Entry {
        val isUnfrozen = frozenState == TangemPayCardFrozenState.Unfrozen
        if (!isEnabled) return if (isUnfrozen) Entry.ManualGuide else Entry.Hidden
        if (card?.canBeAddedToWallet() != true || !isUnfrozen) return Entry.Hidden

        val status = walletStatus
        return when {
            isButtonAlwaysShown || status == CardStatus.NotAdded || status == CardStatus.Incomplete -> {
                Entry.Button(isLoading = isAddingCard)
            }
            status == CardStatus.Unsupported -> Entry.ManualGuide
            else -> Entry.Hidden
        }
    }

    private fun TangemPayCard.canBeAddedToWallet(): Boolean = state == TangemPayCardState.Active &&
        cardStatus.isActive &&
        !isPlaceholder &&
        lastDigits.length == LAST_DIGITS_LENGTH

    private fun TangemPayCard.walletDisplayName(): String =
        displayName?.value ?: context.getString(R.string.tangempay_title)

    private fun UniversalError.isRetryable(): Boolean =
        this == VisaApiError.UnknownWithoutCode || this == VisaApiError.ServerUnavailable

    sealed interface Entry {
        data object Hidden : Entry
        data object ManualGuide : Entry
        data class Button(val isLoading: Boolean) : Entry
    }

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope, userWalletId: UserWalletId): TangemPayAddToGoogleWalletController
    }
}