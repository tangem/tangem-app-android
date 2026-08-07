package com.tangem.features.forceupdate.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.forceupdate.ForceUpdateComponent

@Immutable
internal data class ForceUpdateUM(
    val mode: ForceUpdateComponent.Mode,
    val accent: Accent,
    val title: TextReference,
    val description: TextReference,
    val onUpdateClick: (() -> Unit)?,
    val onLaterClick: (() -> Unit)?,
    val onSupportClick: (() -> Unit)?,
) {

    enum class Accent { Red, Yellow }
}