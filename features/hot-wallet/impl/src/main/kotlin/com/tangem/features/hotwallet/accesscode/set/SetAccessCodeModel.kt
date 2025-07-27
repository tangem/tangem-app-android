package com.tangem.features.hotwallet.accesscode.set

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.accesscode.set.entity.SetAccessCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class SetAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<SetAccessCodeComponent.Params>()

    internal val uiState: StateFlow<SetAccessCodeUM>
    field = MutableStateFlow(getInitialState())

    private fun getInitialState() = SetAccessCodeUM(
        accessCode = "",
        onAccessCodeChange = ::onAccessCodeChange,
        buttonEnabled = false,
        onContinue = ::onContinue,
    )

    private fun onAccessCodeChange(value: String) {
        uiState.update {
            it.copy(
                accessCode = value,
                buttonEnabled = value.length == uiState.value.accessCodeLength,
            )
        }
    }

    private fun onContinue() {
        params.callbacks.onAccessCodeSet(uiState.value.accessCode)
    }
}