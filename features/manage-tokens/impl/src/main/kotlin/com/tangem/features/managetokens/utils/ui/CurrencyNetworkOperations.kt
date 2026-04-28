package com.tangem.features.managetokens.utils.ui

import androidx.annotation.DrawableRes
import com.tangem.common.ui.extensions.greyedOutIconResId
import com.tangem.common.ui.extensions.iconResId
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM

internal fun CurrencyNetworkUM.select(isSelected: Boolean): CurrencyNetworkUM {
    return copy(
        iconResId = network.id.getIconRes(isSelected),
        isSelected = isSelected,
    )
}

@DrawableRes
internal fun Network.ID.getIconRes(isColored: Boolean): Int = if (isColored) {
    this.iconResId
} else {
    this.greyedOutIconResId
}