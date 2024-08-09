package com.tangem.feature.tester.presentation.environments.state

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableSet

/**
 * Content state of environment toggles screen
 *
 * @property title               title
 * @property apiInfoList         environment toggles list
 * @property onEnvironmentSelect the lambda to be invoked when button is pressed
 * @property onBackClick         the lambda to be invoked when back button is pressed
 * @property onApplyChangesClick the lambda to be invoked when apply changes button is pressed
 */
internal data class EnvironmentTogglesScreenUM(
    @StringRes val title: Int,
    val apiInfoList: ImmutableSet<ApiInfoUM>,
    val onEnvironmentSelect: (id: String, environment: String) -> Unit,
    val onBackClick: () -> Unit,
    val onApplyChangesClick: () -> Unit,
) {

    /**
     * Api info
     *
     * @property name         api name
     * @property select       select environment
     * @property url          select url
     * @property environments list of environments
     */
    data class ApiInfoUM(
        val name: String,
        val select: String,
        val url: String,
        val environments: ImmutableSet<String>,
    )
}
