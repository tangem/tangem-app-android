package com.tangem.features.onramp.redirect.entity

import com.tangem.core.ui.extensions.TextReference

internal data class OnrampRedirectUM(
    val topBarConfig: OnrampRedirectTopBarUM,
    val providerImageUrl: String,
    val title: TextReference,
    val subtitle: TextReference,
)