package com.tangem.feature.tester.presentation.featuretoggles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.configtoggle.feature.DISABLED_FEATURE_TOGGLE_VERSION
import com.tangem.core.configtoggle.feature.FeatureToggleInfo
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.version.Version
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureToggleGroupUM
import com.tangem.feature.tester.presentation.featuretoggles.state.TesterFeatureToggleUM
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureTogglesScreenUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.info.AppInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for screen with list of feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 * @property appInfoProvider       app info provider
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class FeatureTogglesViewModel @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
    private val appInfoProvider: AppInfoProvider,
) : ViewModel() {

    // Declared before `state` so it is initialized before `initState()` runs in the field initializer.
    private val appVersion: Version? = Version.create(appInfoProvider.appVersion)

    val state: StateFlow<FeatureTogglesScreenUM>
        field = MutableStateFlow(initState())

    private val mutableFeatureTogglesManager: MutableFeatureTogglesManager
        get() = requireNotNull(featureTogglesManager as? MutableFeatureTogglesManager) {
            "Feature toggle manager must be mutable (debug build type)"
        }

    /** Setup navigation state property by router [router] and provides app restart method by [appFinisher] */
    fun setupInteractions(router: InnerTesterRouter, appFinisher: AppFinisher) {
        state.update { current ->
            current.copy(
                topBar = current.topBar.copy(onBackClick = router::back),
                onRestartAppClick = appFinisher::restart,
            )
        }
    }

    private fun initState(): FeatureTogglesScreenUM {
        return FeatureTogglesScreenUM(
            topBar = getConfigSetupState(isPrimarySetup = true),
            appVersion = appInfoProvider.appVersion,
            featureToggleGroups = mutableFeatureTogglesManager.getTesterFeatureToggleGroups(),
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
                    isVisible = !isMatchLocalConfig,
                    onRefreshClick = ::onRefreshClick,
                ),
            )
        } else {
            val topBar = state.value.topBar
            topBar.copy(
                refreshButton = topBar.refreshButton.copy(isVisible = !isMatchLocalConfig),
            )
        }
    }

    private fun onToggleValueChange(name: String, isEnabled: Boolean) {
        viewModelScope.launch {
            mutableFeatureTogglesManager.changeToggle(name = name, isEnabled = isEnabled)

            val groups = mutableFeatureTogglesManager.getTesterFeatureToggleGroups()
            state.update { it.copy(featureToggleGroups = groups) }

            // delay for smoothly update animations
            delay(timeMillis = 300)

            val topBar = getConfigSetupState(isPrimarySetup = false)
            state.update { it.copy(topBar = topBar) }
        }
    }

    private fun onRefreshClick() {
        viewModelScope.launch {
            mutableFeatureTogglesManager.recoverLocalConfig()

            val topBar = getConfigSetupState(isPrimarySetup = false)
            val groups = mutableFeatureTogglesManager.getTesterFeatureToggleGroups()
            state.update { it.copy(topBar = topBar, featureToggleGroups = groups) }
        }
    }

    private fun MutableFeatureTogglesManager.getTesterFeatureToggleGroups(): ImmutableList<FeatureToggleGroupUM> {
        val togglesByStatus = getFeatureToggles()
            .sortedWith(featureToggleComparator)
            .map { info ->
                TesterFeatureToggleUM(
                    name = info.name,
                    version = info.version,
                    status = statusOf(info.version),
                    isEnabled = info.isEnabled,
                )
            }
            .groupBy(TesterFeatureToggleUM::status)

        return TesterFeatureToggleUM.Status.entries
            .mapNotNull { status ->
                togglesByStatus[status]?.let { toggles ->
                    FeatureToggleGroupUM(status = status, toggles = toggles.toImmutableList())
                }
            }
            .toImmutableList()
    }

    private fun statusOf(toggleVersion: String): TesterFeatureToggleUM.Status {
        val toggle = parseToggleVersion(toggleVersion) ?: return TesterFeatureToggleUM.Status.UNDEFINED
        val app = appVersion ?: return TesterFeatureToggleUM.Status.UNDEFINED
        return when {
            toggle > app -> TesterFeatureToggleUM.Status.PLANNED
            toggle < app -> TesterFeatureToggleUM.Status.RELEASED
            else -> TesterFeatureToggleUM.Status.PENDING
        }
    }

    private companion object {
        // Descending by release version; toggles with no planned release ("undefined") sink to the bottom.
        private val featureToggleComparator: Comparator<FeatureToggleInfo> =
            compareByDescending<FeatureToggleInfo> { parseToggleVersion(it.version) }
                .thenBy { it.name }

        // Avoids Version.create() (which logs on parse failure) for the "no planned release" sentinel.
        private fun parseToggleVersion(version: String): Version? =
            if (version == DISABLED_FEATURE_TOGGLE_VERSION) null else Version.create(version)
    }
}