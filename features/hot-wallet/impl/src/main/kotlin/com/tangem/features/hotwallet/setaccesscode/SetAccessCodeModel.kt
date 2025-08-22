package com.tangem.features.hotwallet.setaccesscode

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.setaccesscode.entity.SetAccessCodeUM
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

    fun onBack() {
        when (uiState.value.step) {
            SetAccessCodeUM.Step.AccessCode -> {
                params.callbacks.onBackClick()
            }
            SetAccessCodeUM.Step.ConfirmAccessCode -> {
                uiState.update {
                    it.copy(
                        step = SetAccessCodeUM.Step.AccessCode,
                        accessCodeSecond = "",
                    )
                }
            }
        }
    }

    private fun getInitialState() = SetAccessCodeUM(
        step = SetAccessCodeUM.Step.AccessCode,
        accessCodeFirst = "",
        accessCodeSecond = "",
        onAccessCodeFirstChange = ::onAccessCodeFirstChange,
        onAccessCodeSecondChange = ::onAccessCodeSecondChange,
        buttonEnabled = false,
        onContinue = ::onContinue,
    )

    private fun onAccessCodeFirstChange(value: String) {
        uiState.update {
            it.copy(
                accessCodeFirst = value,
                buttonEnabled = value.length == uiState.value.accessCodeLength,
            )
        }
    }

    private fun onAccessCodeSecondChange(value: String) {
        uiState.update {
            it.copy(
                accessCodeSecond = value,
                buttonEnabled = uiState.value.accessCodeFirst == uiState.value.accessCodeSecond,
            )
        }
    }

    private fun onContinue() {
        when (uiState.value.step) {
            SetAccessCodeUM.Step.AccessCode -> {
                uiState.update {
                    it.copy(
                        step = SetAccessCodeUM.Step.ConfirmAccessCode,
                    )
                }
            }
            SetAccessCodeUM.Step.ConfirmAccessCode -> {
                params.callbacks.onAccessCodeSet()
            }
        }
    }
}