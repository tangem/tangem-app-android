package com.tangem.core.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import com.tangem.domain.apptheme.model.AppThemeMode

/**
 * Representing a holder for the application theme mode.
 */
@Stable
interface AppThemeModeHolder {

    /**
     * A [State] representing the current application theme mode.
     */
    val appThemeMode: State<AppThemeMode>
}