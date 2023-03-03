package com.tangem.feature.tester.presentation.featuretoggles.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.core.featuretoggle.manager.MutableFeatureTogglesManager
import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import com.tangem.feature.tester.presentation.featuretoggles.state.FeatureTogglesContentState
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for screen with list of feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
 * @author Andrew Khokhlov on 08/02/2023
 */
@HiltViewModel
internal class FeatureTogglesViewModel @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : ViewModel() {

    /** Current ui state */
    var uiState: FeatureTogglesContentState by mutableStateOf(initState())
        private set

    private val mutableFeatureTogglesManager: MutableFeatureTogglesManager
        get() = requireNotNull(featureTogglesManager as? MutableFeatureTogglesManager) {
            "Feature toggle manager must be mutable (debug build type)"
        }

    /** Setup navigation state property by router [router] */
    fun setupNavigation(router: InnerTesterRouter) {
        uiState = uiState.copy(onBackClick = router::back)
    }

    private fun initState(): FeatureTogglesContentState {
        return FeatureTogglesContentState(
            featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles(),
            onBackClick = {},
            onToggleValueChange = ::onToggleValueChange,
        )
    }

    private fun onToggleValueChange(name: String, isEnabled: Boolean) {
        mutableFeatureTogglesManager.changeToggle(name = name, isEnabled = isEnabled)

        uiState = uiState.copy(featureToggles = mutableFeatureTogglesManager.getTesterFeatureToggles())
    }

    private fun MutableFeatureTogglesManager.getTesterFeatureToggles(): List<TesterFeatureToggle> {
        return this
            .getFeatureToggles()
            .map { TesterFeatureToggle(it.key, it.value) }
    }
}
