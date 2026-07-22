package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import java.io.File

internal data class TesterActionsContentState(
    val hideAllCurrenciesUM: HideAllCurrenciesUM,
    val toggleHotWalletRestrictionUM: ToggleHotWalletRestrictionUM,
    val usedeskTokenTtlUM: UsedeskTokenTtlUM,
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

    /**
     * State of the Usedesk chat token TTL setting.
     *
     * @param currentLabel human-readable current TTL (e.g. "7 days", "15 minutes")
     * @param presets selectable preset durations
     * @param onPresetSelected applies one of the [presets] (value in milliseconds)
     * @param onCustomMinutesSelected applies a custom TTL entered in minutes
     */
    @Immutable
    data class UsedeskTokenTtlUM(
        val currentLabel: String,
        val presets: ImmutableList<Preset>,
        val onPresetSelected: (Long) -> Unit,
        val onCustomMinutesSelected: (Long) -> Unit,
    ) {
        data class Preset(val label: String, val millis: Long)
    }

    @Immutable
    data class ShareLogsUM(val file: File?)
}