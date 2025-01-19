package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

internal data class SelectableChainRowUM(
    val id: Int,
    @DrawableRes val icon: Int,
    val text: TextReference,
)
