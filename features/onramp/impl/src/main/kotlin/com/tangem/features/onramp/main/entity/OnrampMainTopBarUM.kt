package com.tangem.features.onramp.main.entity

import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference

internal data class OnrampMainTopBarUM(
    val title: TextReference,
    val startButtonUM: TopAppBarButtonUM,
    val endButtonUM: TopAppBarButtonUM,
)