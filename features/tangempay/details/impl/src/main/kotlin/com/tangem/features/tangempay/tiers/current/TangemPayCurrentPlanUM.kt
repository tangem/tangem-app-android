package com.tangem.features.tangempay.tiers.current

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayCurrentPlanUM(
    val planName: TextReference,
    val notification: Notification?,
    val sections: ImmutableList<Section>,
    val onBackClick: () -> Unit,
    val onChangePlanClick: () -> Unit,
) {
    @Immutable
    data class Notification(
        val text: TextReference,
        val button: Button? = null,
    ) {
        @Immutable
        data class Button(
            val text: TextReference,
            val onClick: () -> Unit,
        )
    }

    @Immutable
    data class Section(
        val header: TextReference,
        val items: ImmutableList<InfoItem>,
    )

    @Immutable
    data class InfoItem(
        val label: TextReference,
        val value: TextReference,
    )
}