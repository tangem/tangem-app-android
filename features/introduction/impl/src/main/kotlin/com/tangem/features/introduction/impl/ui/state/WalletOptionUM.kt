package com.tangem.features.introduction.impl.ui.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

internal data class WalletOptionUM(
    @DrawableRes val iconRes: Int,
    val title: TextReference,
    val description: TextReference,
    val onClick: () -> Unit,
    val isLoading: Boolean = false,
)