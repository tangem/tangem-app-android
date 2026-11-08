package com.tangem.store.features.cardsettings.impl.securitymode.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.api.analytics.TangemSdkErrorEvent
import com.tangem.store.features.cardsettings.impl.common.CardSettingsAnalyticsEvent
import com.tangem.store.features.cardsettings.impl.domain.CardSettingsInteractor
import com.tangem.store.features.cardsettings.impl.domain.SecurityOption
import com.tangem.store.features.cardsettings.impl.securitymode.ui.state.SecurityModeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class SecurityModeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val cardSettingsInteractor: CardSettingsInteractor,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsErrorHandler: AnalyticsErrorHandler,
    private val router: Router,
) : Model() {

    private val scannedScanResponse = cardSettingsInteractor.requireScanResponse()

    /**
     * Security option that is set on the card at the moment the screen is opened. A successful [saveChanges] closes
     * the screen, so this option stays valid for the whole lifetime of the model.
     */
    private val actualSecurityOption = cardSettingsInteractor.getSecurityOption(scannedScanResponse.card)

    val uiState: StateFlow<SecurityModeUM>
        field = MutableStateFlow(getInitialState())

    private fun getInitialState(): SecurityModeUM {
        val allowedSecurityOptions = cardSettingsInteractor.getAllowedSecurityOptions(scannedScanResponse)

        return SecurityModeUM(
            availableOptions = allowedSecurityOptions.toImmutableList(),
            selectedSecurityMode = actualSecurityOption,
            isSaveChangesEnabled = false,
            onNewModeSelected = ::selectOption,
            onSaveChangesClicked = ::saveChanges,
        )
    }

    private fun selectOption(securityOption: SecurityOption) {
        uiState.update { state ->
            state.copy(
                selectedSecurityMode = securityOption,
                isSaveChangesEnabled = securityOption != actualSecurityOption,
            )
        }
    }

    private fun saveChanges() {
        val cardId = scannedScanResponse.card.cardId
        val selectedOption = uiState.value.selectedSecurityMode

        modelScope.launch {
            val result = when (selectedOption) {
                SecurityOption.LongTap -> tangemSdkManager.setLongTap(cardId)
                SecurityOption.PassCode -> tangemSdkManager.setPasscode(cardId)
                SecurityOption.AccessCode -> tangemSdkManager.setAccessCode(cardId)
            }

            when (result) {
                is CompletionResult.Success -> {
                    // The scan response is shared between all card settings screens, so it must reflect the card
                    // state only after the user code has actually been changed
                    cardSettingsInteractor.updateSecurityOption(selectedOption)

                    val paramValue = selectedOption.toAnalyticsSecurityMode()
                    analyticsEventHandler.send(CardSettingsAnalyticsEvent.SecurityModeChanged(paramValue))

                    router.pop()
                }
                is CompletionResult.Failure -> {
                    // The user cancelled the operation or it failed, so the card keeps its previous security option
                    selectOption(actualSecurityOption)

                    val error = result.error
                    if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                        analyticsErrorHandler.sendErrorEvent(TangemSdkErrorEvent(error))
                    }
                }
            }
        }
    }
}

private fun SecurityOption.toAnalyticsSecurityMode(): CardSettingsAnalyticsEvent.SecurityModeChanged.Mode =
    when (this) {
        SecurityOption.AccessCode -> CardSettingsAnalyticsEvent.SecurityModeChanged.Mode.AccessCode
        SecurityOption.PassCode -> CardSettingsAnalyticsEvent.SecurityModeChanged.Mode.Passcode
        SecurityOption.LongTap -> CardSettingsAnalyticsEvent.SecurityModeChanged.Mode.LongTap
    }