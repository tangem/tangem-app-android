package com.tangem.features.managetokens.utils.ui

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM

internal fun CurrencyNetworkUM.select(isSelected: Boolean): CurrencyNetworkUM {
    return copy(
        iconResId = network.id.getIconRes(isSelected),
        isSelected = isSelected,
    )
}

@DrawableRes
internal fun Network.ID.getIconRes(isColored: Boolean): Int = if (isColored) {
    getActiveIconRes(value)
} else {
    getGreyedOutIconRes(value)
}