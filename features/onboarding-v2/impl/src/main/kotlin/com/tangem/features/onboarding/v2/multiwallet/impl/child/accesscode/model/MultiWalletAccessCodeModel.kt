package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class MultiWalletAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())
    private val params = paramsContainer.require<MultiWalletChildParams>()

    val uiState = _uiState.asStateFlow()
    val onDismiss = MutableSharedFlow<Unit>()

    init {
        params.multiWalletState.update {
            it.copy(accessCode = null)
        }
    }

    fun onBack() {
        when (_uiState.value.step) {
            MultiWalletAccessCodeUM.Step.Intro -> {
                modelScope.launch { onDismiss.emit(Unit) }
            }
            MultiWalletAccessCodeUM.Step.AccessCode -> {
                _uiState.update {
                    it.copy(step = MultiWalletAccessCodeUM.Step.Intro)
                }
            }
            MultiWalletAccessCodeUM.Step.ConfirmAccessCode -> {
                _uiState.update {
                    it.copy(step = MultiWalletAccessCodeUM.Step.AccessCode)
                }
            }
        }
    }

    private fun getInitialState() = MultiWalletAccessCodeUM(
        onContinue = ::onContinue,
        onAccessCodeFirstChange = ::onAccessCodeFirstChange,
        onAccessCodeSecondChange = ::onAccessCodeSecondChange,
    )

    private fun onAccessCodeFirstChange(textFieldValue: TextFieldValue) {
        _uiState.update { it.copy(accessCodeFirst = textFieldValue) }
    }

    private fun onAccessCodeSecondChange(textFieldValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                accessCodeSecond = textFieldValue,
                codesNotMatchError = false,
            )
        }
    }

    private fun onContinue() {
        when (_uiState.value.step) {
            MultiWalletAccessCodeUM.Step.Intro -> {
                _uiState.update { it.copy(step = MultiWalletAccessCodeUM.Step.AccessCode) }
            }
            MultiWalletAccessCodeUM.Step.AccessCode -> {
                if (_uiState.value.accessCodeFirst.text.length > 3) {
                    _uiState.update { it.copy(step = MultiWalletAccessCodeUM.Step.ConfirmAccessCode) }
                }
            }
            MultiWalletAccessCodeUM.Step.ConfirmAccessCode -> {
                // TODO check
                if (checkAccessCode()) {
                    params.multiWalletState.update {
                        it.copy(accessCode = OnboardingMultiWalletState.AccessCode(uiState.value.accessCodeFirst.text))
                    }
                    modelScope.launch { onDismiss.emit(Unit) }
                }
            }
        }
    }

    private fun checkAccessCode(): Boolean {
        if (_uiState.value.accessCodeFirst.text == _uiState.value.accessCodeSecond.text) {
            return true
        } else {
            _uiState.update {
                it.copy(codesNotMatchError = true,)
            }
            return false
        }
    }
}
