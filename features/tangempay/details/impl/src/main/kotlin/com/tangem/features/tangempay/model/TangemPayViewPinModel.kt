package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayViewPinComponent
import com.tangem.features.tangempay.entity.TangemPayViewPinUM
import com.tangem.features.tangempay.model.transformers.TangemPayViewPinErrorStateTransformer
import com.tangem.features.tangempay.model.transformers.TangemPayViewPinSuccessStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayViewPinModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
) : Model() {

    private val params = paramsContainer.require<TangemPayViewPinComponent.Params>()

    val uiState: StateFlow<TangemPayViewPinUM>
        field = MutableStateFlow(getInitialState())

    init {
        getCardPin()
    }

    private fun getCardPin() {
        modelScope.launch {
            cardDetailsRepository.getPin(userWalletId = params.walletId, cardId = params.cardId)
                .onRight { pin ->
                    if (!pin.isNullOrEmpty()) {
                        uiState.update(TangemPayViewPinSuccessStateTransformer(pin, params.listener::onClickChangePin))
                    } else {
                        uiState.update(TangemPayViewPinErrorStateTransformer())
                    }
                }
                .onLeft {
                    uiState.update(TangemPayViewPinErrorStateTransformer())
                }
        }
    }

    private fun getInitialState(): TangemPayViewPinUM {
        return TangemPayViewPinUM.Loading(onDismiss = ::onDismiss)
    }

    fun onDismiss() {
        params.listener.onDismissViewPin()
    }
}