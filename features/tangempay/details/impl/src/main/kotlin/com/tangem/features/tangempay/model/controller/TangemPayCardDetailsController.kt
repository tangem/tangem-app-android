package com.tangem.features.tangempay.model.controller

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.findCardWithId
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.CardDataType
import com.tangem.features.tangempay.entity.TangemPayCardDetailsBlockStateFactory
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.DetailsHiddenStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealProgressStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealedStateTransformer
import com.tangem.features.tangempay.model.transformers.TangemPayCardDetailsUpdateNameTransformer
import com.tangem.utils.StringsSigns
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.tangem.utils.transformer.update as transformerUpdate

private const val SHOW_DETAILS_TIME = 30_000L

/**
 * Owns the UI state of a single TangemPay card-detail block (the flip card with reveal PAN/CVV,
 * freeze badge, display-name editing). Several controllers can be alive at once — e.g. one per card
 * in a swipe pager — so all logic is scoped to [cardId] and reveal/hide is coordinated through the
 * card-scoped [CardDetailsEventListener].
 *

 * lifecycle is owned by the host model, which passes a child [scope] and calls [dispose] when the
 * card disappears.
 */
@Suppress("LongParameterList")
@Stable
internal class TangemPayCardDetailsController @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val card: TangemPayCard,
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val config: Config,
    @Assisted private val onEditNameClick: () -> Unit,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val analytics: AnalyticsEventHandler,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    val cardId: String = card.id

    private var frozenStateJob: Job? = null

    private val stateFactory = TangemPayCardDetailsBlockStateFactory(
        cardNumberEnd = card.lastDigits,
        displayName = card.displayName,
        isEditingNameEnabled = config.isEditingNameEnabled,
        onEditNameClick = onEditNameClick,
        onReveal = ::requestReveal,
        onCopy = ::copyData,
        shouldShowCardDetailsButtonOnCard = config.shouldShowCardDetailsButtonOnCard,
    )

    val uiState: StateFlow<TangemPayCardDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val revealCardDetailsJobHolder = JobHolder()
    private val showCardDetailsTimerJobHolder = JobHolder()

    init {
        subscribeToCardChanges()
        scope.launch {
            cardDetailsEventListener.event.collect { event ->
                when (event) {
                    is CardDetailsEvent.Show -> if (event.cardId == cardId) revealCardDetails() else hideCardDetails()
                    is CardDetailsEvent.Hide -> if (event.cardId == cardId) hideCardDetails()
                    CardDetailsEvent.HideAll -> hideCardDetails()
                }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun subscribeToCardChanges() {
        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { state ->
                val status = state.value
                if (status is PaymentAccountStatusValue.Loaded && status.source == StatusSource.ACTUAL) {
                    val card = status.findCardWithId(cardId) ?: return@onEach
                    if (card.state != TangemPayCardState.Active) {
                        requestHide()
                    }
                    card.displayName?.let { uiState.update(TangemPayCardDetailsUpdateNameTransformer(it)) }
                    uiState.update { uiState ->
                        uiState.copy(
                            numberShort = "${StringsSigns.ASTERISK}${card.lastDigits}",
                            cardFrozenState = card.frozenState,
                            isActionsAvailable = card.state == TangemPayCardState.Active,
                        )
                    }
                    subscribeToCardFrozenState(card.id)
                }
            }
            .launchIn(scope)
    }

    private fun subscribeToCardFrozenState(cardId: String) {
        frozenStateJob?.cancel()
        frozenStateJob = cardDetailsRepository.cardFrozenState(cardId)
            .onEach { cardFrozenState ->
                if (cardFrozenState == TangemPayCardFrozenState.Pending) {
                    uiState.update { state -> state.copy(cardFrozenState = TangemPayCardFrozenState.Pending) }
                }
            }
            .launchIn(scope)
    }

    private fun requestReveal() {
        cardDetailsEventListener.send(CardDetailsEvent.Show(cardId))
    }

    private fun requestHide() {
        cardDetailsEventListener.send(CardDetailsEvent.Hide(cardId))
    }

    private fun revealCardDetails() {
        analytics.send(TangemPayAnalyticsEvents.ViewCardDetailsClicked())
        scope.launch {
            uiState.transformerUpdate(
                transformer = DetailsRevealProgressStateTransformer(onClickHide = ::requestHide),
            )
            cardDetailsRepository.revealCardDetails(userWalletId, cardId)
                .onRight { cardDetails ->
                    uiState.transformerUpdate(
                        transformer = DetailsRevealedStateTransformer(
                            details = cardDetails,
                            onClickHide = ::requestHide,
                        ),
                    )
                    launchShowDetailsTimer()
                }
                .onLeft {
                    uiState.transformerUpdate(
                        transformer = DetailsHiddenStateTransformer(
                            stateFactory = stateFactory,
                            shouldShowCardDetailsButtonOnCard = config.shouldShowCardDetailsButtonOnCard,
                        ),
                    )
                    showError()
                }
        }.saveIn(revealCardDetailsJobHolder)
    }

    private fun launchShowDetailsTimer() {
        scope.launch {
            delay(SHOW_DETAILS_TIME)
            requestHide()
        }.saveIn(showCardDetailsTimerJobHolder)
    }

    private fun hideCardDetails() {
        revealCardDetailsJobHolder.cancel()
        uiState.transformerUpdate(
            transformer = DetailsHiddenStateTransformer(
                stateFactory = stateFactory,
                shouldShowCardDetailsButtonOnCard = config.shouldShowCardDetailsButtonOnCard,
            ),
        )
    }

    private fun showError() {
        uiMessageSender.send(
            SnackbarMessage(TextReference.Res(R.string.tangempay_card_details_error_text)),
        )
    }

    private fun copyData(text: String, type: CardDataType) {
        val event = when (type) {
            CardDataType.Number -> TangemPayAnalyticsEvents.CopyCardNumberClicked()
            CardDataType.Expiry -> TangemPayAnalyticsEvents.CopyCardExpiryClicked()
            CardDataType.CVV -> TangemPayAnalyticsEvents.CopyCardCVVClicked()
        }
        analytics.send(event)
        clipboardManager.setText(text = text.filterNot { it.isWhitespace() }, isSensitive = true)
    }

    /** Per-block configuration that does not depend on live card data. */
    data class Config(
        val isEditingNameEnabled: Boolean,
        val shouldShowCardDetailsButtonOnCard: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            scope: CoroutineScope,
            card: TangemPayCard,
            userWalletId: UserWalletId,
            config: Config,
            onEditNameClick: () -> Unit,
        ): TangemPayCardDetailsController
    }
}