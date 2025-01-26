package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.state.OnboardingVisaAccessCodeUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaAccessCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    @Suppress("UnusedPrivateMember")
    private val tangemSdkManager: TangemSdkManager,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())
    val uiState = _uiState.asStateFlow()
    val onBack = MutableSharedFlow<Unit>()
    val onDone = MutableSharedFlow<Unit>()

    fun onBack() {
        when (uiState.value.step) {
            OnboardingVisaAccessCodeUM.Step.Enter -> modelScope.launch { onBack.emit(Unit) }
            OnboardingVisaAccessCodeUM.Step.ReEnter ->
                _uiState.update { it.copy(step = OnboardingVisaAccessCodeUM.Step.Enter) }
        }
    }

    private fun getInitialState(): OnboardingVisaAccessCodeUM {
        return OnboardingVisaAccessCodeUM(
            onAccessCodeFirstChange = ::onAccessCodeFirstChange,
            onAccessCodeSecondChange = ::onAccessCodeSecondChange,
            onAccessCodeHideClick = { _uiState.update { it.copy(accessCodeHidden = !it.accessCodeHidden) } },
            onContinue = { onContinue() },
        )
    }

    private fun onAccessCodeFirstChange(textFieldValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                accessCodeFirst = textFieldValue,
                atLeastMinCharsError = false,
            )
        }
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
        when (uiState.value.step) {
            OnboardingVisaAccessCodeUM.Step.Enter -> {
                if (checkAccessCodeMinChars().not()) return
                _uiState.update { it.copy(step = OnboardingVisaAccessCodeUM.Step.ReEnter) }
            }
            OnboardingVisaAccessCodeUM.Step.ReEnter -> {
                if (checkAccessCodesMatch().not()) return
                startActivationProcess(accessCode = uiState.value.accessCodeFirst.text)
            }
        }
    }

    private fun checkAccessCodeMinChars(): Boolean {
        val first = uiState.value.accessCodeFirst.text

        if (first.length < ACCESS_CODE_MIN_LENGTH) {
            _uiState.update { it.copy(atLeastMinCharsError = true) }
            return false
        }

        return true
    }

    private fun checkAccessCodesMatch(): Boolean {
        val first = uiState.value.accessCodeFirst.text
        val second = uiState.value.accessCodeSecond.text

        if (first != second) {
            _uiState.update { it.copy(codesNotMatchError = true) }
            return false
        }

        return true
    }

    @Suppress("UnusedPrivateMember")
    private fun startActivationProcess(accessCode: String) {
        modelScope.launch { onDone.emit(Unit) }

        // modelScope.launch {
        //     @Suppress("UnusedPrivateMember")
        //     val result = tangemSdkManager.activateVisaCard(
        //         accessCode = accessCode,
        //         challengeToSign = null,
        //         activationInput = TODO(),
        //     )
        //
        // when (result) {
        //     is CompletionResult.Success -> {
        //         val response = result.data
        //         TODO()
        //     }
        //     is CompletionResult.Failure -> {
        //         // show alert
        //         TODO()
        //     }
        // }
        // }
    }

    private companion object {
        const val ACCESS_CODE_MIN_LENGTH = 4
    }
}