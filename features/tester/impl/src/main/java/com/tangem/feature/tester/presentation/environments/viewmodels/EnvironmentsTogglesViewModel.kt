package com.tangem.feature.tester.presentation.environments.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.MutableApiConfigsManager
import com.tangem.feature.tester.impl.BuildConfig
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.environments.state.EnvironmentTogglesScreenUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for screen with list of environment toggles
 *
 * @param apiConfigsManager manager for getting information about the api configs
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class EnvironmentsTogglesViewModel @Inject constructor(
    apiConfigsManager: ApiConfigsManager,
) : ViewModel() {

    /** Current ui state */
    val uiState: StateFlow<EnvironmentTogglesScreenUM>
        get() = _uiState

    private val _uiState = MutableStateFlow(value = getInitialState())

    private val mutableApiConfigsManager: MutableApiConfigsManager =
        requireNotNull(apiConfigsManager as? MutableApiConfigsManager) {
            "MutableApiConfigsManager isn't available in build type ${BuildConfig.BUILD_TYPE}."
        }

    init {
        subscribeOnApiConfigs()
    }

    /** Setup navigation state property by router [router] */
    fun setupNavigation(router: InnerTesterRouter) {
        _uiState.update {
            it.copy(onBackClick = router::back)
        }
    }

    private fun subscribeOnApiConfigs() {
        mutableApiConfigsManager.configs
            .onEach { configs ->
                _uiState.update {
                    it.copy(apiInfoList = configs.toUiModel())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun Map<ApiConfig, ApiEnvironment>.toUiModel(): ImmutableSet<EnvironmentTogglesScreenUM.ApiInfoUM> {
        return map {
            val (config, currentEnvironment) = it

            EnvironmentTogglesScreenUM.ApiInfoUM(
                name = config.id.name,
                select = currentEnvironment.name,
                url = config.environmentConfigs.firstOrNull { it.environment == currentEnvironment }?.baseUrl
                    ?: error("Current environment's url isn't found"),
                environments = config.environmentConfigs
                    .map { environmentConfig -> environmentConfig.environment.name }
                    .toImmutableSet(),
            )
        }
            .toImmutableSet()
    }

    private fun getInitialState(): EnvironmentTogglesScreenUM {
        return EnvironmentTogglesScreenUM(
            title = R.string.environment_toggles,
            apiInfoList = persistentSetOf(),
            onEnvironmentSelect = ::onToggleValueChange,
            onBackClick = {},
        )
    }

    private fun onToggleValueChange(id: String, name: String) {
        viewModelScope.launch {
            mutableApiConfigsManager.changeEnvironment(id = id, environment = ApiEnvironment.valueOf(name))
        }
    }
}