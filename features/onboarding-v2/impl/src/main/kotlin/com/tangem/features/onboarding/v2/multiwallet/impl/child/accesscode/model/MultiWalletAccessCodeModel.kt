package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class MultiWalletAccessCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDismiss = MutableSharedFlow<Unit>()

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
    )

    private fun onContinue() {
        when (_uiState.value.step) {
            MultiWalletAccessCodeUM.Step.Intro -> {
                _uiState.update { it.copy(step = MultiWalletAccessCodeUM.Step.AccessCode) }
            }
            MultiWalletAccessCodeUM.Step.AccessCode -> {
                _uiState.update { it.copy(step = MultiWalletAccessCodeUM.Step.ConfirmAccessCode) }
            }
            MultiWalletAccessCodeUM.Step.ConfirmAccessCode -> {
                // TODO check
                modelScope.launch { onDismiss.emit(Unit) }
            }
        }
    }
}