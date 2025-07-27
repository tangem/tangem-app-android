package com.tangem.features.hotwallet.accesscode.confirm

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.accesscode.confirm.entity.ConfirmAccessCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class ConfirmAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<ConfirmAccessCodeComponent.Params>()

    internal val uiState: StateFlow<ConfirmAccessCodeUM>
    field = MutableStateFlow(getInitialState())

    private fun getInitialState() = ConfirmAccessCodeUM(
        accessCode = "",
        onAccessCodeChange = ::onAccessCodeChange,
        buttonEnabled = false,
        onConfirm = ::onConfirm,
    )

    private fun onAccessCodeChange(value: String) {
        uiState.update {
            it.copy(
                accessCode = value,
                buttonEnabled = value == params.accessCodeToConfirm,
            )
        }
    }

    private fun onConfirm() {
        params.callbacks.onAccessCodeConfirmed()
    }
}