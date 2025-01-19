package com.tangem.features.onboarding.v2.visa.impl.child.pincode.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state.OnboardingVisaPinCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaPinCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    private fun getInitialState(): OnboardingVisaPinCodeUM {
        return OnboardingVisaPinCodeUM(
            onPinCodeChange = ::onPinCodeChange,
            onSubmitClick = ::onSubmitClick,
        )
    }

    private fun onPinCodeChange(pin: String) {
        if (pin.all { it.isDigit() }) _uiState.update { it.copy(pinCode = pin) }
    }

    private fun onSubmitClick() {
        if (checkPinCode(_uiState.value.pinCode).not()) return

        // TODO

        modelScope.launch { onDone.emit(Unit) }
    }

    private fun checkPinCode(pin: String): Boolean {
        return pin.length == PIN_CODE_LENGTH
    }

    private companion object {
        const val PIN_CODE_LENGTH = 4
    }
}
