package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsBlockStateFactory
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.DetailsHiddenStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealProgressStateTransformer
import com.tangem.features.tangempay.model.transformers.DetailsRevealedStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.utils.transformer.update as transformerUpdate

@Stable
@ModelScoped
internal class TangemPayCardDetailsBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
) : Model() {

    private val params: TangemPayCardDetailsBlockComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayCardDetailsBlockStateFactory(
        cardNumberEnd = params.params.config.cardNumberEnd,
        onReveal = ::revealCardDetails,
        onCopy = ::copyData,
    )

    val uiState: StateFlow<TangemPayCardDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val revealCardDetailsJobHolder = JobHolder()

    init {
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

    private fun subscribeToCardFrozenState() {
        cardDetailsRepository
            .cardFrozenState(params.params.config.cardId)
            .onEach { uiState.update { state -> state.copy(cardFrozenState = it) } }
            .launchIn(modelScope)
    }

    private fun revealCardDetails() {
        modelScope.launch {
            uiState.transformerUpdate(
                transformer = DetailsRevealProgressStateTransformer(onClickHide = ::hideCardDetails),
            )
            cardDetailsRepository.revealCardDetails()
                .onRight { cardDetails ->
                    uiState.transformerUpdate(
                        transformer = DetailsRevealedStateTransformer(
                            details = cardDetails,
                            onClickHide = ::hideCardDetails,
                        ),
                    )
                }
                .onLeft {
                    uiState.transformerUpdate(transformer = DetailsHiddenStateTransformer(stateFactory))
                    showError()
                }
        }.saveIn(revealCardDetailsJobHolder)
    }

    fun hideCardDetails() {
        modelScope.launch {
            revealCardDetailsJobHolder.cancel()
            uiState.transformerUpdate(transformer = DetailsHiddenStateTransformer(stateFactory))
        }
    }

    private fun showError() {
        uiMessageSender.send(
            SnackbarMessage(TextReference.Res(R.string.tangempay_card_details_error_text)),
        )
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text.filterNot { it.isWhitespace() }, isSensitive = true)
    }
}