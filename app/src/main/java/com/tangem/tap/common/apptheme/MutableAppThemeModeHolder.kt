package com.tangem.tap.common.apptheme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.domain.apptheme.model.AppThemeMode

internal object MutableAppThemeModeHolder : AppThemeModeHolder {

    override val appThemeMode: MutableState<AppThemeMode> = mutableStateOf(AppThemeMode.DEFAULT)

    var value: AppThemeMode
        set(value) {
            appThemeMode.value = value
        }
        get() = appThemeMode.value

    var isDarkThemeActive: Boolean = false
}