package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import kotlinx.coroutines.flow.*
import com.tangem.features.tangempay.model.transformers.PinCodeChangeTransformer
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayChangePinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
) : Model() {

    val uiState: StateFlow<TangemPayChangePinUM>
        field = MutableStateFlow(getInitialState())

    private fun onPinCodeChange(pin: String) {
        uiState.update(transformer = PinCodeChangeTransformer(newPin = pin))
    }

    private fun onClickSubmit() {
        modelScope.launch {
            uiState.update { it.copy(submitButtonLoading = true) }
            val result = cardDetailsRepository.setPin(uiState.value.pinCode).getOrNull()
            uiState.update { it.copy(submitButtonLoading = false) }
            when (result) {
                SetPinResult.SUCCESS -> router.push(TangemPayDetailsInnerRoute.ChangePINSuccess)
                SetPinResult.PIN_TOO_WEAK,
                SetPinResult.DECRYPTION_ERROR,
                SetPinResult.UNKNOWN_ERROR,
                null,
                -> Unit // TODO: [REDACTED_TASK_KEY] - add error handling once the requirements arrive
            }
        }
    }

    private fun getInitialState(): TangemPayChangePinUM {
        return TangemPayChangePinUM(
            pinCode = "",
            onPinCodeChange = ::onPinCodeChange,
            onSubmitClick = ::onClickSubmit,
            submitButtonEnabled = false,
            submitButtonLoading = false,
            error = null,
        )
    }
}