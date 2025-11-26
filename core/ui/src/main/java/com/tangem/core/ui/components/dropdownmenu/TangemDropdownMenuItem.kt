package com.tangem.core.ui.components.dropdownmenu

import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.TextReference

data class TangemDropdownMenuItem(
    val title: TextReference,
    val textColor: ColorReference,
    val onClick: () -> Unit,
)