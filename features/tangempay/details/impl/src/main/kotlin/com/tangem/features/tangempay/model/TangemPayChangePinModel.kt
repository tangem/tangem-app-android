package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import com.tangem.features.tangempay.model.transformers.PinCodeChangeTransformer
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayChangePinModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val uiMessageSender: UiMessageSender,
    private val router: Router,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
) : Model() {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()
    val uiState: StateFlow<TangemPayChangePinUM>
        field = MutableStateFlow(getInitialState())

    private fun onPinCodeChange(pin: String) {
        uiState.update(transformer = PinCodeChangeTransformer(newPin = pin))
    }

    private fun onClickSubmit() {
        modelScope.launch {
            uiState.update { it.copy(submitButtonLoading = true) }
            val result = try {
                cardDetailsRepository.setPin(
                    userWalletId = params.userWalletId,
                    pin = uiState.value.pinCode,
                ).getOrNull()
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            uiState.update { it.copy(submitButtonLoading = false) }
            when (result) {
                SetPinResult.PIN_TOO_WEAK -> {
                    uiMessageSender.send(
                        message = ToastMessage(resourceReference(R.string.tangempay_pin_validation_error_message)),
                    )
                }
                SetPinResult.SUCCESS -> router.push(TangemPayDetailsInnerRoute.ChangePINSuccess)
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