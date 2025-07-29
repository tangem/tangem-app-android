package com.tangem.features.hotwallet.setaccesscode

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.setaccesscode.entity.AccessCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class AccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<AccessCodeComponent.Params>()

    internal val uiState: StateFlow<AccessCodeUM>
    field = MutableStateFlow(getInitialState())

    private fun getInitialState() = AccessCodeUM(
        accessCode = "",
        onAccessCodeChange = ::onAccessCodeChange,
        buttonEnabled = false,
        onButtonClick = ::onButtonClick,
    )

    private fun onAccessCodeChange(value: String) {
        uiState.update {
            it.copy(
                accessCode = value,
                buttonEnabled = if (params.isConfirmMode) {
                    value == params.accessCodeToConfirm
                } else {
                    value.length == uiState.value.accessCodeLength
                },
            )
        }
    }

    private fun onButtonClick() {
        if (params.isConfirmMode) {
            params.callbacks.onAccessCodeConfirmed()
        } else {
            params.callbacks.onAccessCodeSet(uiState.value.accessCode)
        }
    }
}