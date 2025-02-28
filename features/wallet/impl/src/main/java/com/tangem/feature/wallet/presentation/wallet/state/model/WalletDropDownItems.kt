package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

internal data class WalletDropDownItems(
    val text: TextReference,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
)