package com.tangem.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.core.ui.theme.AppThemeModeHolder

@Stable
interface UiDependencies {

    val vibratorHapticManager: VibratorHapticManager

    val appThemeModeHolder: AppThemeModeHolder

    val globalSnackbarHostState: SnackbarHostState

    val eventMessageHandler: EventMessageHandler
}
