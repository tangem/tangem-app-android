package com.tangem.tap.features.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.DialogFullScreen
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateProperty")
class RootDetectedWarningComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val securityInfoProvider: DeviceSecurityInfoProvider,
    private val settingsRepository: SettingsRepository,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val isShown = instanceKeeper.getOrCreateSimple { MutableStateFlow(false) }

    suspend fun tryToShowWarningAndWaitContinuation() {
        if (isShown.value) return

        if (settingsRepository.isRootDetectedWarningShown().not() && securityInfoProvider.isSecurityExposed()) {
            isShown.value = true
        }

        isShown.first { it == false } // Wait until the warning is dismissed
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val isShownState by isShown.collectAsStateWithLifecycle()

        if (isShownState) {
            DialogFullScreen(onDismissRequest = {}) {
                RootDetectedWarningContent(
                    modifier = modifier,
                    onContinueClick = remember(this) { ::onContinueClick },
                )
            }
        }
    }

    private fun onContinueClick() {
        componentScope.launch {
            settingsRepository.setRootDetectedWarningShown(true)
            isShown.value = false
        }
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Unit, RootDetectedWarningComponent> {
        override fun create(context: AppComponentContext, params: Unit): RootDetectedWarningComponent
    }
}