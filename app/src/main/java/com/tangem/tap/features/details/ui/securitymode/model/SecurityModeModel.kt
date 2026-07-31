package com.tangem.tap.features.details.ui.securitymode.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.analytics.events.TangemSdkErrorEvent
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.getAllowedSecurityOptions
import com.tangem.tap.features.details.ui.common.utils.getCurrentSecurityOption
import com.tangem.tap.features.details.ui.securitymode.SecurityModeScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val appRouter: AppRouter,
) : Model() {

    private val scannedScanResponse = cardSettingsInteractor.scannedScanResponse.value
        ?: error("Scan response is null")

    /**
     * Security option that is set on the card at the moment the screen is opened. A successful [saveChanges] closes
     * the screen, so this option stays valid for the whole lifetime of the model.
     */
    private val actualSecurityOption = getCurrentSecurityOption(scannedScanResponse.card)

    val screenState = MutableStateFlow(value = getInitialState())

    private fun getInitialState(): SecurityModeScreenState {
        val card = scannedScanResponse.card
        val cardTypesResolver = scannedScanResponse.cardTypesResolver

        val allowedSecurityOptions = getAllowedSecurityOptions(card, cardTypesResolver, actualSecurityOption)

        return SecurityModeScreenState(
            availableOptions = allowedSecurityOptions.toList(),
            selectedSecurityMode = actualSecurityOption,
            isSaveChangesEnabled = false,
            onNewModeSelected = ::selectOption,
            onSaveChangesClicked = ::saveChanges,
        )
    }

    private fun selectOption(securityOption: SecurityOption) {
        screenState.update { state ->
            state.copy(
                selectedSecurityMode = securityOption,
                isSaveChangesEnabled = securityOption != actualSecurityOption,
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

            when (result) {
                is CompletionResult.Success -> {
                    // The scan response is shared between all card settings screens, so it must reflect the card
                    // state only after the user code has actually been changed
                    cardSettingsInteractor.update { scanResponse ->
                        scanResponse.copy(
                            card = scanResponse.card.copy(
                                isAccessCodeSet = selectedOption == SecurityOption.AccessCode,
                                isPasscodeSet = selectedOption == SecurityOption.PassCode,
                            ),
                        )
                    }

                    val paramValue = AnalyticsParam.SecurityMode.from(selectedOption)
                    analyticsEventHandler.send(Settings.CardSettings.SecurityModeChanged(paramValue))

                    appRouter.pop()
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