package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.Immutable
import java.io.File

internal data class TesterActionsContentState(
    val hideAllCurrenciesUM: HideAllCurrenciesUM,
    val toggleHotWalletRestrictionUM: ToggleHotWalletRestrictionUM,
    val shareLogsUM: ShareLogsUM,
    val onBackClick: () -> Unit,
) {
    @Immutable
    sealed class HideAllCurrenciesUM {
        data class Clickable(val onClick: () -> Unit) : HideAllCurrenciesUM()

        data object Progress : HideAllCurrenciesUM()
    }

    data class ToggleHotWalletRestrictionUM(
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    )

    @Immutable
    data class ShareLogsUM(val file: File?)
}