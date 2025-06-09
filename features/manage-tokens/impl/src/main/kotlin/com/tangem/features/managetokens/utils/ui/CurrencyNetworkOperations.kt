package com.tangem.features.managetokens.utils.ui

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
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
    getActiveIconRes(rawId.value)
} else {
    getGreyedOutIconRes(rawId.value)
}