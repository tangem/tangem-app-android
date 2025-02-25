package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference

internal data class WalletDropDownItems(
    val text: TextReference,
    val icon: ImageVector,
    val onClick: () -> Unit,
)