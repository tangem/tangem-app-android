package com.tangem.features.tangempay.closure

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.pay.usecase.CloseTangemPayCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayCloseCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val closeTangemPayCardUseCase: CloseTangemPayCardUseCase,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayCloseCardComponent.Params>()

    val state: StateFlow<TangemPayCloseCardUM>
        field = MutableStateFlow(
            TangemPayCloseCardUM(
                isClosingInProgress = false,
                onCloseClick = ::onConfirm,
                onDismissRequest = ::onDismiss,
            ),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened())
    }

    fun onDismiss() {
        if (state.value.isClosingInProgress) return
        params.listener.onDismissCloseCard()
    }

    private fun onConfirm() {
        if (state.value.isClosingInProgress) return
        analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmed())
        state.update { it.copy(isClosingInProgress = true) }
        modelScope.launch {
            closeTangemPayCardUseCase(
                userWalletId = params.userWalletId,
                cardId = params.cardId,
            ).onLeft {
                state.update { uiState -> uiState.copy(isClosingInProgress = false) }
                uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
                params.listener.onDismissCloseCard()
            }.onRight {
                params.listener.onDismissCloseCard()
            }
        }
    }
}