package com.tangem.store.features.cardsettings.impl.coderecovery.model

import androidx.compose.runtime.Stable
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.store.features.cardsettings.impl.coderecovery.AccessCodeRecoveryScreenState
import com.tangem.store.features.cardsettings.impl.common.CardSettingsAnalyticsEvent
import com.tangem.store.features.cardsettings.impl.common.utils.isAccessCodeRecoveryEnabled
import com.tangem.store.features.cardsettings.impl.domain.CardSettingsInteractor
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
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
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
            isEnabledOnCard = isEnabled,
            isEnabledSelection = isEnabled,
            isSaveChangesEnabled = false,
            onSaveChangesClick = ::saveChanges,
            onOptionClick = ::selectOption,
        )
    }

    private fun saveChanges() = modelScope.launch {
        val isEnabled = screenState.value.isEnabledSelection

        tangemSdkManager
            .setAccessCodeRecoveryEnabled(scannedScanResponse.card.cardId, isEnabled)
            .doOnSuccess {
                analyticsEventHandler.send(
                    CardSettingsAnalyticsEvent.AccessCodeRecoveryChanged(
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

                router.pop()
            }
    }

    private fun selectOption(isEnabled: Boolean) {
        screenState.update { state ->
            state.copy(
                isEnabledSelection = isEnabled,
                isSaveChangesEnabled = isEnabled != state.isEnabledOnCard,
            )
        }
    }
}