package com.tangem.features.introduction.impl.ui.state

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference

internal data class WalletOptionUM(
    val icon: ImageVector,
    val title: TextReference,
    val description: TextReference,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val onClick: () -> Unit,
)