package com.tangem.features.walletconnect.transaction.entity.blockaid

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class WcEstimatedWalletChangesUM(
    val items: ImmutableList<WcEstimatedWalletChangeUM>,
)

internal data class WcEstimatedWalletChangeUM(
    @DrawableRes val iconRes: Int,
    val title: TextReference,
    val description: String,
    val tokenIconUrl: String?,
)