package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MINIMUM_ACCESS_CODE_LENGTH = 4

@ComponentScoped
internal class MultiWalletAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val backupServiceHolder: BackupServiceHolder,
    private val analyticsHandler: AnalyticsEventHandler,
) : Model() {

    private val _uiState = MutableStateFlow(getInitialState())
    private val params = paramsContainer.require<MultiWalletChildParams>()

    val uiState = _uiState.asStateFlow()
    val onDismiss = MutableSharedFlow<Unit>()

    init {
        analyticsHandler.send(OnboardingEvent.Backup.SettingAccessCodeStarted)

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
                    it.copy(
                        step = MultiWalletAccessCodeUM.Step.Intro,
                        accessCodeFirst = TextFieldValue(""),
                        accessCodeSecond = TextFieldValue(""),
                    )
                }
            }
            MultiWalletAccessCodeUM.Step.ConfirmAccessCode -> {
                _uiState.update {
                    it.copy(
                        step = MultiWalletAccessCodeUM.Step.AccessCode,
                        accessCodeSecond = TextFieldValue(""),
                    )
                }
            }
        }
    }

    private fun getInitialState() = MultiWalletAccessCodeUM(
        onAccessCodeFirstChange = ::onAccessCodeFirstChange,
        onAccessCodeSecondChange = ::onAccessCodeSecondChange,
        onContinue = ::onContinue,
        onAccessCodeHideClick = ::onAccessCodeHideClick,
    )

    private fun onAccessCodeHideClick() {
        _uiState.update {
            it.copy(accessCodeHidden = !it.accessCodeHidden)
        }
    }

    private fun onAccessCodeFirstChange(textFieldValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                accessCodeFirst = textFieldValue,
                atLeast4CharError = false,
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
        when (_uiState.value.step) {
            MultiWalletAccessCodeUM.Step.Intro -> {
                _uiState.update { it.copy(step = MultiWalletAccessCodeUM.Step.AccessCode) }
            }
            MultiWalletAccessCodeUM.Step.AccessCode -> {
                if (_uiState.value.accessCodeFirst.text.length >= MINIMUM_ACCESS_CODE_LENGTH) {
                    _uiState.update {
                        it.copy(
                            step = MultiWalletAccessCodeUM.Step.ConfirmAccessCode,
                            atLeast4CharError = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(atLeast4CharError = true)
                    }
                }

                analyticsHandler.send(OnboardingEvent.Backup.AccessCodeEntered)
            }
            MultiWalletAccessCodeUM.Step.ConfirmAccessCode -> {
                if (checkAccessCode()) {
                    backupServiceHolder.backupService.get()?.setAccessCode(_uiState.value.accessCodeFirst.text)

                    // make on done
                    params.multiWalletState.update {
                        it.copy(accessCode = OnboardingMultiWalletState.AccessCode(uiState.value.accessCodeFirst.text))
                    }
                    modelScope.launch { onDismiss.emit(Unit) }
                }

                analyticsHandler.send(OnboardingEvent.Backup.AccessCodeReEntered)
            }
        }
    }

    private fun checkAccessCode(): Boolean {
        if (_uiState.value.accessCodeFirst.text == _uiState.value.accessCodeSecond.text) {
            return true
        } else {
            _uiState.update {
                it.copy(codesNotMatchError = true)
            }
            return false
        }
    }
}