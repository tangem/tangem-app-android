package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.hasCardWithId
import com.tangem.domain.models.account.requireCardWithId
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsBlockStateFactory
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.DetailsHiddenStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealProgressStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealedStateTransformer
import com.tangem.features.tangempay.model.transformers.TangemPayCardDetailsUpdateNameTransformer
import com.tangem.features.tangempay.navigation.TangemPayCardDetailsInnerRoute
import com.tangem.utils.StringsSigns
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.utils.transformer.update as transformerUpdate

private const val SHOW_DETAILS_TIME = 30_000L

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayCardDetailsBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    private val params: TangemPayCardDetailsBlockComponent.Params = paramsContainer.require()
    private val card = params.card

    private val stateFactory = TangemPayCardDetailsBlockStateFactory(
        cardNumberEnd = card.lastDigits,
        displayName = card.displayName,
        isEditingNameEnabled = params.isEditingNameEnabled,
        onEditNameClick = ::startEditingDisplayName,
        onReveal = ::revealCardDetails,
        onCopy = ::copyData,
    )

    val uiState: StateFlow<TangemPayCardDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val revealCardDetailsJobHolder = JobHolder()
    private val showCardDetailsTimerJobHolder = JobHolder()

    init {
        subscribeToCardChanges(cardId = card.id, userWalletId = params.userWalletId)
        subscribeToCardFrozenState()
        modelScope.launch {
            cardDetailsEventListener.event.collectLatest { event ->
                when (event) {
                    CardDetailsEvent.Hide -> hideCardDetails()
                    CardDetailsEvent.Show -> revealCardDetails()
                }
            }
        }
    }

    private fun subscribeToCardChanges(cardId: String, userWalletId: UserWalletId) {
        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { state ->
                val status = state.value
                if (status is PaymentAccountStatusValue.Loaded &&
                    status.source == StatusSource.ACTUAL &&
                    status.hasCardWithId(cardId)
                ) {
                    val card = status.requireCardWithId(cardId)
                    if (card.isReissuing) {
                        hideCardDetails()
                    }
                    card.displayName?.let { uiState.update(TangemPayCardDetailsUpdateNameTransformer(it)) }
                    uiState.update { uiState ->
                        uiState.copy(
                            numberShort = "${StringsSigns.ASTERISK}${card.lastDigits}",
                            cardFrozenState = card.frozenState,
                            isActionsAvailable = !card.isReissuing,
                        )
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun subscribeToCardFrozenState() {
        cardDetailsRepository.cardFrozenState(card.id)
            .onEach { cardFrozenState ->
                if (cardFrozenState == TangemPayCardFrozenState.Pending) {
                    uiState.update { state -> state.copy(cardFrozenState = TangemPayCardFrozenState.Pending) }
                }
            }
            .launchIn(modelScope)
    }

    private fun revealCardDetails() {
        analytics.send(TangemPayAnalyticsEvents.ViewCardDetailsClicked())
        modelScope.launch {
            uiState.transformerUpdate(
                transformer = DetailsRevealProgressStateTransformer(onClickHide = ::hideCardDetails),
            )
            cardDetailsRepository.revealCardDetails(params.userWalletId)
                .onRight { cardDetails ->
                    uiState.transformerUpdate(
                        transformer = DetailsRevealedStateTransformer(
                            details = cardDetails,
                            onClickHide = ::hideCardDetails,
                        ),
                    )
                    launchShowDetailsTimer()
                }
                .onLeft {
                    uiState.transformerUpdate(transformer = DetailsHiddenStateTransformer(stateFactory))
                    showError()
                }
        }.saveIn(revealCardDetailsJobHolder)
    }

    private fun launchShowDetailsTimer() {
        modelScope.launch {
            delay(SHOW_DETAILS_TIME)
            hideCardDetails()
        }.saveIn(showCardDetailsTimerJobHolder)
    }

    fun hideCardDetails() {
        revealCardDetailsJobHolder.cancel()
        uiState.transformerUpdate(transformer = DetailsHiddenStateTransformer(stateFactory))
    }

    private fun showError() {
        uiMessageSender.send(
            SnackbarMessage(TextReference.Res(R.string.tangempay_card_details_error_text)),
        )
    }

    private fun startEditingDisplayName() {
        router.push(TangemPayCardDetailsInnerRoute.EditCardDisplayName)
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
}

internal enum class CardDataType {
    Number, Expiry, CVV
}