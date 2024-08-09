package com.tangem.features.markets.details.impl.ui.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class LinksUM(
    val officialLinks: ImmutableList<Link>,
    val social: ImmutableList<Link>,
    val repository: ImmutableList<Link>,
    val blockchainSite: ImmutableList<Link>,
    val onLinkClick: (Link) -> Unit,
) {
    data class Link(
        @DrawableRes val iconRes: Int,
        val title: TextReference,
        val url: String,
    )
}