package com.tangem.tap.features.details.ui.cardsettings.coderecovery.model

import androidx.compose.runtime.Stable
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.AccessCodeRecoveryScreenState
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.isAccessCodeRecoveryEnabled
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class AccessCodeRecoveryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val cardSettingsInteractor: CardSettingsInteractor,
) : Model() {

    private val scannedScanResponse = cardSettingsInteractor.scannedScanResponse.value
        ?: error("Scan response is null")

    val screenState = MutableStateFlow(
        value = getInitialState(),
    )

    private fun getInitialState(): AccessCodeRecoveryScreenState {
        val isEnabled = isAccessCodeRecoveryEnabled(
            typeResolver = scannedScanResponse.cardTypesResolver,
            card = scannedScanResponse.card,
        )

        return AccessCodeRecoveryScreenState(
            enabledOnCard = isEnabled,
            enabledSelection = isEnabled,
            isSaveChangesEnabled = false,
            onSaveChangesClick = ::saveChanges,
            onOptionClick = ::selectOption,
        )
    }

    private fun saveChanges() = modelScope.launch {
        val isEnabled = screenState.value.enabledSelection

        tangemSdkManager
            .setAccessCodeRecoveryEnabled(scannedScanResponse.card.cardId, isEnabled)
            .doOnSuccess {
                Analytics.send(
                    Settings.CardSettings.AccessCodeRecoveryChanged(
                        AnalyticsParam.AccessCodeRecoveryStatus.from(isEnabled),
                    ),
                )

                cardSettingsInteractor.update { scanResponse ->
                    scanResponse.copy(
                        card = scanResponse.card.copy(
                            userSettings = scanResponse.card.userSettings?.copy(
                                isUserCodeRecoveryAllowed = isEnabled,
                            ),
                        ),
                    )
                }

                store.dispatchNavigationAction(AppRouter::pop)
            }
    }

    private fun selectOption(isEnabled: Boolean) {
        screenState.update {
            it.copy(
                enabledSelection = isEnabled,
                isSaveChangesEnabled = isEnabled != it.enabledOnCard,
            )
        }
    }
}