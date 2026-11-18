package com.tangem.features.tangempay.card.closure

import arrow.core.Either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardResult.Result
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.usecase.CloseTangemPayCardUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.tangem.core.ui.R as CoreUiR

@Singleton
internal class TangemPayCardCloseCoordinator @Inject constructor(
    private val closeTangemPayCardUseCase: CloseTangemPayCardUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analytics: AnalyticsEventHandler,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val appCoroutineScope: AppCoroutineScope,
) {

    val closedCards: SharedFlow<TangemPayClosedCard>
        field = MutableSharedFlow(extraBufferCapacity = CLOSED_CARDS_BUFFER)

    suspend fun close(card: TangemPayClosingCard): Either<TangemPayApiError, Unit> = closeTangemPayCardUseCase(
        userWalletId = card.userWalletId,
        cardId = card.cardId,
        onOrderEnded = { isCompleted -> onOrderEnded(card = card, isCompleted = isCompleted) },
    ).onLeft { error ->
        val result = if (error == TangemPayApiError.CardCloseLastCard) Result.LastCard else Result.Error
        analytics.send(TangemPayAnalyticsEvents.CloseCardResult(result))
    }

    fun onCloseErrorDismissed() {
        analytics.send(TangemPayAnalyticsEvents.CloseCardErrorDismissed())
    }

    fun contactSupport(userWalletId: UserWalletId, customerId: String?) {
        analytics.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
        if (customerId == null) return
        appCoroutineScope.launch {
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.TangemPay.FeatureIsBeta(
                    walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    private fun onOrderEnded(card: TangemPayClosingCard, isCompleted: Boolean) {
        if (isCompleted) {
            analytics.send(TangemPayAnalyticsEvents.CloseCardResult(Result.Success))
            closedCards.tryEmit(TangemPayClosedCard(userWalletId = card.userWalletId, cardId = card.cardId))
            uiMessageSender.send(
                SnackbarMessage(message = card.closedMessage(), startIconId = CoreUiR.drawable.ic_success_20),
            )
        } else {
            analytics.send(TangemPayAnalyticsEvents.CloseCardResult(Result.Error))
            uiMessageSender.send(
                ModalMessage(
                    closeCardErrorModalUM(
                        onContactSupportClick = { contactSupport(card.userWalletId, card.customerId) },
                        onOkClick = ::onCloseErrorDismissed,
                    ),
                ),
            )
        }
    }

    private fun TangemPayClosingCard.closedMessage(): TextReference = if (name != null) {
        resourceReference(R.string.tangempay_card_closed_toast_named, wrappedList(name))
    } else {
        resourceReference(R.string.tangempay_card_closed_toast, wrappedList(lastDigits))
    }

    private companion object {
        const val CLOSED_CARDS_BUFFER = 4
    }
}

internal data class TangemPayClosingCard(
    val userWalletId: UserWalletId,
    val cardId: String,
    val customerId: String?,
    val name: String?,
    val lastDigits: String,
)

internal data class TangemPayClosedCard(
    val userWalletId: UserWalletId,
    val cardId: String,
)