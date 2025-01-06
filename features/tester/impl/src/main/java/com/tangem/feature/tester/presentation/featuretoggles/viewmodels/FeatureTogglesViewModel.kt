package com.tangem.feature.tester.presentation.featuretoggles.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
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
            topBar = uiState.topBar.copy(onBackClick = router::back),
            onRestartAppClick = appFinisher::restart,
        )
    }

    private fun initState(): FeatureTogglesContentState {
        return FeatureTogglesContentState(
            topBar = getConfigSetupState(isPrimarySetup = true),
            appVersion = appVersionProvider.versionName,
            featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles(),
            onToggleValueChange = ::onToggleValueChange,
            onRestartAppClick = {},
        )
    }

    private fun getConfigSetupState(isPrimarySetup: Boolean): TopBarWithRefreshUM {
        val isMatchLocalConfig = mutableFeatureTogglesManager.isMatchLocalConfig()

        return if (isPrimarySetup) {
            TopBarWithRefreshUM(
                titleResId = R.string.feature_toggles,
                onBackClick = {},
                refreshButton = TopBarWithRefreshUM.RefreshButton(
                    isVisible = false,
                    onRefreshClick = ::onRefreshClick,
                ),
            )
        } else {
            uiState.topBar.copy(
                refreshButton = uiState.topBar.refreshButton.copy(isVisible = isMatchLocalConfig),
            )
        }
    }

    private fun onToggleValueChange(name: String, isEnabled: Boolean) {
        viewModelScope.launch {
            mutableFeatureTogglesManager.changeToggle(name = name, isEnabled = isEnabled)

            uiState = uiState.copy(featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles())

            // delay for smoothly update animations
            delay(timeMillis = 300)

            uiState = uiState.copy(topBar = getConfigSetupState(isPrimarySetup = false))
        }
    }

    private fun onRefreshClick() {
        viewModelScope.launch {
            mutableFeatureTogglesManager.recoverLocalConfig()

            uiState = uiState.copy(
                topBar = getConfigSetupState(isPrimarySetup = false),
                featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles(),
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