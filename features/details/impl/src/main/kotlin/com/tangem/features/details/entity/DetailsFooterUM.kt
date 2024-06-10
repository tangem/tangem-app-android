package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import kotlinx.collections.immutable.ImmutableList

internal data class DetailsFooterUM(
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