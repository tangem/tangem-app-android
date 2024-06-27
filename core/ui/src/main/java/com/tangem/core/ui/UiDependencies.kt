package com.tangem.core.ui

import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.theme.AppThemeModeHolder

interface UiDependencies {

    val hapticManager: HapticManager

    val appThemeModeHolder: AppThemeModeHolder
}
