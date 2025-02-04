package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.Immutable
import com.tangem.domain.apptheme.model.AppThemeMode
import java.io.File

internal data class TesterActionsContentState(
    val hideAllCurrenciesUM: HideAllCurrenciesUM,
    val toggleAppThemeUM: ToggleAppThemeUM,
    val shareLogsUM: ShareLogsUM,
    val onBackClick: () -> Unit,
) {
    sealed class HideAllCurrenciesUM {
        data class Clickable(val onClick: () -> Unit) : HideAllCurrenciesUM()

        data object Progress : HideAllCurrenciesUM()
    }

    data class ToggleAppThemeUM(
        val currentAppTheme: AppThemeMode,
        val onClick: () -> Unit,
    )

    @Immutable
    data class ShareLogsUM(val file: File?)
}