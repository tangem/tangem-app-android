package com.tangem.features.allservices.impl.entries

import com.tangem.core.ui.extensions.TextReference

/** A titled group of entries; hidden, title included, while none of its entries is visible. */
internal class AllServicesSection(
    val key: String,
    val title: TextReference?,
    val entries: List<AllServicesEntry>,
)