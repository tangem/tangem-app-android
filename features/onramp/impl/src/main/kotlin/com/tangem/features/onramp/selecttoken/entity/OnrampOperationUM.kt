package com.tangem.features.onramp.selecttoken.entity

import androidx.annotation.StringRes

internal data class OnrampOperationUM(
    @StringRes val titleResId: Int,
    val onBackClick: () -> Unit,
    val isHotCryptoVisible: Boolean,
)