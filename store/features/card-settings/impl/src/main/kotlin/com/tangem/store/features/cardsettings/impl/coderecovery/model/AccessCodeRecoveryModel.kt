package com.tangem.store.features.cardsettings.impl.coderecovery.model

import androidx.compose.runtime.Stable
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.store.features.cardsettings.impl.coderecovery.AccessCodeRecoveryScreenState
import com.tangem.store.features.cardsettings.impl.common.CardSettingsAnalyticsEvent
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

    private val scannedScanResponse = cardSettingsInteractor.requireScanResponse()

    val screenState = MutableStateFlow(
        value = getInitialState(),
    )

    private fun getInitialState(): AccessCodeRecoveryScreenState {
        val isEnabled = cardSettingsInteractor.isAccessCodeRecoveryEnabled(scannedScanResponse)

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

                cardSettingsInteractor.updateAccessCodeRecovery(isEnabled)

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