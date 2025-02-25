package com.tangem.tap.features.details.ui.securitymode.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.getAllowedSecurityOptions
import com.tangem.tap.features.details.ui.common.utils.getCurrentSecurityOption
import com.tangem.tap.features.details.ui.securitymode.SecurityModeScreenState
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class SecurityModeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val cardSettingsInteractor: CardSettingsInteractor,
) : Model() {

    private val scannedScanResponse = cardSettingsInteractor.scannedScanResponse.value
        ?: error("Scan response is null")

    val screenState = MutableStateFlow(value = getInitialState())

    private fun getInitialState(): SecurityModeScreenState {
        val card = scannedScanResponse.card
        val cardTypesResolver = scannedScanResponse.cardTypesResolver

        val currentSecurityOption = getCurrentSecurityOption(card)
        val allowedSecurityOptions = getAllowedSecurityOptions(card, cardTypesResolver, currentSecurityOption)

        return SecurityModeScreenState(
            availableOptions = allowedSecurityOptions.toList(),
            selectedSecurityMode = currentSecurityOption,
            isSaveChangesEnabled = false,
            onNewModeSelected = ::selectOption,
            onSaveChangesClicked = ::saveChanges,
        )
    }

    private fun selectOption(securityOption: SecurityOption) {
        screenState.update { state ->
            state.copy(
                selectedSecurityMode = securityOption,
                isSaveChangesEnabled = securityOption != getCurrentSecurityOption(scannedScanResponse.card),
            )
        }
    }

    private fun saveChanges() {
        val cardId = scannedScanResponse.card.cardId
        val selectedOption = screenState.value.selectedSecurityMode

        modelScope.launch {
            val result = when (selectedOption) {
                SecurityOption.LongTap -> tangemSdkManager.setLongTap(cardId)
                SecurityOption.PassCode -> tangemSdkManager.setPasscode(cardId)
                SecurityOption.AccessCode -> tangemSdkManager.setAccessCode(cardId)
            }

            cardSettingsInteractor.update {
                it.copy(
                    card = it.card.copy(
                        isAccessCodeSet = selectedOption == SecurityOption.AccessCode,
                        isPasscodeSet = selectedOption == SecurityOption.PassCode,
                    ),
                )
            }

            val paramValue = AnalyticsParam.SecurityMode.from(selectedOption)
            when (result) {
                is CompletionResult.Success -> {
                    Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue))

                    store.dispatchNavigationAction(AppRouter::pop)
                }
                is CompletionResult.Failure -> {
                    val error = result.error
                    if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                        Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue, error))
                    }
                }
                else -> Unit
            }
        }
    }
}