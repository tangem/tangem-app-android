package com.tangem.feature.tester.presentation.featuretoggles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.DISABLED_FEATURE_TOGGLE_VERSION
import com.tangem.core.configtoggle.feature.FeatureToggleInfo
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.version.Version
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureToggleGroupUM
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureTogglesScreenUM
import com.tangem.feature.tester.presentation.featuretoggles.state.TesterFeatureToggleUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
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
    private val appVersion: Version? = createAppVersion()
    private val streamByToggleName: Map<String, String> = FeatureToggles.entries.associate { it.rawName to it.stream }
    private val streams: List<String> = FeatureToggles.entries.map(FeatureToggles::stream).distinct()
    private var selectedStream: String? = null

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
            searchBar = initSearchBar(),
            streamFilters = createStreamFilters(selectedStream = null),
            featureToggleGroups = mutableFeatureTogglesManager.getTesterFeatureToggleGroups(
                query = "",
                stream = null,
            ),
            onToggleValueChange = ::onToggleValueChange,
            onRestartAppClick = {},
        )
    }

    private fun initSearchBar(): TangemSearch.State {
        return TangemSearch.State(
            placeholderText = resourceReference(com.tangem.core.ui.R.string.common_search),
            query = "",
            onQueryChange = ::onQueryChange,
            isActive = false,
            onActiveChange = ::onActiveChange,
            onClearClick = ::onClearQuery,
        )
    }

    private fun onQueryChange(query: String) {
        val groups = mutableFeatureTogglesManager.getTesterFeatureToggleGroups(
            query = query,
            stream = selectedStream,
        )
        state.update { screenUM ->
            screenUM.copy(
                searchBar = screenUM.searchBar.copy(query = query),
                featureToggleGroups = groups,
            )
        }
    }

    private fun onStreamSelect(stream: String?) {
        selectedStream = stream
        val groups = getCurrentToggleGroups()
        state.update { screenUM ->
            screenUM.copy(
                streamFilters = createStreamFilters(selectedStream = stream),
                featureToggleGroups = groups,
            )
        }
    }

    private fun createStreamFilters(selectedStream: String?): ImmutableList<ChipUM> {
        val all = ChipUM(
            id = 0,
            text = stringReference(value = "All"),
            isSelected = selectedStream == null,
            onClick = { onStreamSelect(stream = null) },
        )
        val byStream = streams.mapIndexed { index, stream ->
            ChipUM(
                id = index + 1,
                text = stringReference(value = stream.replaceFirstChar(Char::uppercaseChar)),
                isSelected = stream == selectedStream,
                onClick = { onStreamSelect(stream = stream) },
            )
        }
        return (listOf(all) + byStream).toImmutableList()
    }

    private fun onActiveChange(isActive: Boolean) {
        state.update { it.copy(searchBar = it.searchBar.copy(isActive = isActive)) }
    }

    private fun onClearQuery() {
        onQueryChange(query = "")
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

            val groups = getCurrentToggleGroups()
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
            val groups = getCurrentToggleGroups()
            state.update { it.copy(topBar = topBar, featureToggleGroups = groups) }
        }
    }

    private fun getCurrentToggleGroups(): ImmutableList<FeatureToggleGroupUM> {
        return mutableFeatureTogglesManager.getTesterFeatureToggleGroups(
            query = state.value.searchBar.query,
            stream = selectedStream,
        )
    }

    private fun MutableFeatureTogglesManager.getTesterFeatureToggleGroups(
        query: String,
        stream: String?,
    ): ImmutableList<FeatureToggleGroupUM> {
        val togglesByStatus = getFeatureToggles()
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { stream == null || streamByToggleName[it.name] == stream }
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

    private fun createAppVersion(): Version? {
        val rawVersion = appInfoProvider.appVersion
        // Drop the build-type suffix (e.g. "6.0-internal" -> "6.0") so the version parses, mirroring
        // DefaultVersionProvider; otherwise every toggle falls back to the "Not planned yet" group.
        val sanitizedVersion = rawVersion.substringBefore(delimiter = '-')
        val version = Version.create(sanitizedVersion)

        if (version == null) {
            TangemLogger.e("Failed to parse app version: raw=$rawVersion, sanitized=$sanitizedVersion")
        }

        return version
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