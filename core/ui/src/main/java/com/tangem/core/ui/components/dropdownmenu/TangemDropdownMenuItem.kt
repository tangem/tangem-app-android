package com.tangem.core.ui.components.dropdownmenu

import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.TextReference

@Deprecated("Use TangemMenu.UiState.Item of the DS3 TangemMenu")
data class TangemDropdownMenuItem(
    val title: TextReference,
    val textColor: ColorReference,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)