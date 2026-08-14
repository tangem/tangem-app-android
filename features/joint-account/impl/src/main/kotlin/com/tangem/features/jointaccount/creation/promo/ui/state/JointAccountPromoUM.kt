package com.tangem.features.jointaccount.creation.promo.ui.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class JointAccountPromoUM(
    val title: TextReference,
    val subtitle: TextReference,
    val benefits: ImmutableList<BenefitUM>,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    data class BenefitUM(
        val id: String,
        val icon: TangemIconUM,
        val title: TextReference,
        val subtitle: TextReference,
    )
}