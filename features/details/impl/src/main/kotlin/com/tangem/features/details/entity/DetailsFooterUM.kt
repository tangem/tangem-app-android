package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
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