package com.tangem.features.details.state

import androidx.annotation.DrawableRes
import kotlinx.collections.immutable.ImmutableList

data class DetailsFooter(
    val appVersion: String,
    val socials: ImmutableList<Social>,
) {

    data class Social(
        val id: String,
        @DrawableRes
        val iconResId: Int,
        val onClick: () -> Unit,
    )
}
