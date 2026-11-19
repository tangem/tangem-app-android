package com.tangem.features.tangempay.card.closure

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened.CardState
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardPopupDismissed.Action
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.FreezeCardClicked.Source as FreezeSource
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
internal class TangemPayCloseCardConfirmModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val closeCoordinator: TangemPayCardCloseCoordinator,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayCloseCardConfirmComponent.Params>()
    private val card = params.card

    private var isClosingInProgress = false
    private var isActionTaken = false
    private var isFinished = false

    val state: StateFlow<MessageModalUM>
        field = MutableStateFlow(confirmModal())

    init {
        val cardState = if (params.canFreeze) CardState.Active else CardState.Frozen
        analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened(cardState))
    }

    fun onDismiss() {
        if (isClosingInProgress) return
        if (!isActionTaken) {
            isActionTaken = true
            analytics.send(TangemPayAnalyticsEvents.CloseCardPopupDismissed(Action.CloseButton))
        }
        finish { params.listener.onCloseCardConfirmDismissed() }
    }

    private fun onCloseCard() {
        if (isClosingInProgress || isFinished) return
        isActionTaken = true
        analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmed())
        setClosingInProgress(true)
        modelScope.launch {
            closeCoordinator.close(card)
                .onRight { finish { params.listener.onCloseCardConfirmDismissed() } }
                .onLeft(::onCloseRefused)
        }
    }

    private fun onCloseRefused(error: TangemPayApiError) {
        setClosingInProgress(false)
        if (error == TangemPayApiError.CardCloseLastCard) {
            uiMessageSender.send(
                SnackbarMessage(
                    message = resourceReference(R.string.tangempay_card_close_last_card_disabled),
                    startIconId = CoreUiR.drawable.ic_alert_circle_24,
                ),
            )
            finish { params.listener.onCloseCardConfirmDismissed() }
        } else {
            state.value = closeCardErrorModalUM(
                onContactSupportClick = { closeCoordinator.contactSupport(card.userWalletId, card.customerId) },
                onOkClick = closeCoordinator::onCloseErrorDismissed,
            )
        }
    }

    private fun onFreezeInstead() {
        if (isClosingInProgress || isFinished) return
        isActionTaken = true
        analytics.send(TangemPayAnalyticsEvents.FreezeCardClicked(FreezeSource.ClosePopup))
        finish { params.listener.onFreezeInsteadOfClose() }
    }

    private fun onKeepCard() {
        if (isClosingInProgress || isActionTaken) return
        isActionTaken = true
        analytics.send(TangemPayAnalyticsEvents.CloseCardPopupDismissed(Action.KeepCard))
    }

    private fun setClosingInProgress(isInProgress: Boolean) {
        isClosingInProgress = isInProgress
        state.value = confirmModal()
    }

    private inline fun finish(notifyListener: () -> Unit) {
        if (isFinished) return
        isFinished = true
        notifyListener()
    }

    private fun confirmModal(): MessageModalUM = closeCardConfirmModalUM(
        cardName = card.name,
        lastDigits = card.lastDigits,
        canFreeze = params.canFreeze,
        isClosingInProgress = isClosingInProgress,
        onCloseCardClick = ::onCloseCard,
        onFreezeInsteadClick = ::onFreezeInstead,
        onKeepCardClick = ::onKeepCard,
    )
}