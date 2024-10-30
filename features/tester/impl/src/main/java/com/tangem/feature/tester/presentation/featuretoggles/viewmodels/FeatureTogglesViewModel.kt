package com.tangem.feature.tester.presentation.featuretoggles.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureTogglesContentState
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.version.AppVersionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for screen with list of feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 * @property appVersionProvider    app version provider
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class FeatureTogglesViewModel @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
    private val appVersionProvider: AppVersionProvider,
) : ViewModel() {

    /** Current ui state */
    var uiState: FeatureTogglesContentState by mutableStateOf(initState())
        private set

    private val mutableFeatureTogglesManager: MutableFeatureTogglesManager
        get() = requireNotNull(featureTogglesManager as? MutableFeatureTogglesManager) {
            "Feature toggle manager must be mutable (debug build type)"
        }

    /** Setup navigation state property by router [router] and provides app restart method by [appFinisher] */
    fun setupInteractions(router: InnerTesterRouter, appFinisher: AppFinisher) {
        uiState = uiState.copy(
            onBackClick = router::back,
            onRestartAppClick = appFinisher::restart,
        )
    }

    private fun initState(): FeatureTogglesContentState {
        return FeatureTogglesContentState(
            topBarState = getConfigSetupState(),
            appVersion = appVersionProvider.versionName,
            featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles(),
            onToggleValueChange = ::onToggleValueChange,
            onBackClick = {},
            onRestartAppClick = {},
        )
    }

    private fun onToggleValueChange(name: String, isEnabled: Boolean) {
        viewModelScope.launch {
            mutableFeatureTogglesManager.changeToggle(name = name, isEnabled = isEnabled)

            uiState = uiState.copy(featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles())

            // delay for smoothly update animations
            delay(timeMillis = 300)

            uiState = uiState.copy(topBarState = getConfigSetupState())
        }
    }

    private fun getConfigSetupState(): FeatureTogglesContentState.TopBarState {
        return if (mutableFeatureTogglesManager.isMatchLocalConfig()) {
            FeatureTogglesContentState.TopBarState.ConfigSetup
        } else {
            FeatureTogglesContentState.TopBarState.CustomSetup(
                onRecoverClick = {
                    viewModelScope.launch {
                        mutableFeatureTogglesManager.recoverLocalConfig()

                        uiState = uiState.copy(
                            topBarState = getConfigSetupState(),
                            featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles(),
                        )
                    }
                },
            )
        }
    }

    private fun MutableFeatureTogglesManager.getTesterFeatureToggles(): ImmutableList<TesterFeatureToggle> {
        return this
            .getFeatureToggles()
            .map { TesterFeatureToggle(it.key, it.value) }
            .toImmutableList()
    }
}