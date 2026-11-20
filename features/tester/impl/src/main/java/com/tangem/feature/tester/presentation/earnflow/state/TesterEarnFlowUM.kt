package com.tangem.feature.tester.presentation.earnflow.state

import com.tangem.features.earn.flow.api.EarnFlowComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** [launch] is set while the flow is open on top of the settings. */
internal data class TesterEarnFlowUM(
    val onBackClick: () -> Unit = {},
    val target: String = "Loading the selected wallet…",
    val presets: ImmutableList<OptionUM> = persistentListOf(),
    val actions: ImmutableList<OptionUM> = persistentListOf(),
    val switches: ImmutableList<SwitchUM> = persistentListOf(),
    val isOpenEnabled: Boolean = false,
    val onOpenClick: () -> Unit = {},
    val launch: EarnFlowComponent.Params? = null,
    val onFlowClose: () -> Unit = {},
) {

    data class OptionUM(val title: String, val isSelected: Boolean, val onClick: () -> Unit)

    data class SwitchUM(val title: String, val isChecked: Boolean, val onCheckedChange: (Boolean) -> Unit)
}