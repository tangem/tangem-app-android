package com.tangem.features.jointaccount.creation.promo.ui.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class JointAccountPromoUM(
    val title: TextReference,
    val subtitle: TextReference,
    val points: ImmutableList<PointUM>,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    data class PointUM(
        val id: String,
        val icon: TangemIconUM,
        val lead: TextReference,
        val description: TextReference,
    )
}