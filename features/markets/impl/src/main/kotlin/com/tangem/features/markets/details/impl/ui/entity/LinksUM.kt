package com.tangem.features.markets.details.impl.ui.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class LinksUM(
    val officialLinks: PersistentList<LinkUM>,
    val social: PersistentList<LinkUM>,
    val repository: PersistentList<LinkUM>,
    val blockchainSite: PersistentList<LinkUM>,
    val onLinkClick: (LinkUM) -> Unit,
) {
    @Immutable
    data class LinkUM(
        @DrawableRes val iconRes: Int,
        val title: TextReference,
        val url: String,
    )
}
