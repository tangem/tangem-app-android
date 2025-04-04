package com.tangem.core.ui.components.dropdownmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference

data class TangemDropdownMenuItem(
    val title: TextReference,
    val textColorProvider: @Composable () -> Color,
    val onClick: () -> Unit,
)