package com.tangem.features.walletconnect.transaction.entity.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
internal data class WcNetworkInfoUM(
    val name: String,
    @DrawableRes val iconRes: Int,
)