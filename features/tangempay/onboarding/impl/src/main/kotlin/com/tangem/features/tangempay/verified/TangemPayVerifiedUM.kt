package com.tangem.features.tangempay.verified

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import javax.annotation.concurrent.Immutable

@Immutable
internal sealed interface TangemPayVerifiedUM {

    val onCloseClick: () -> Unit

    data class Loading(override val onCloseClick: () -> Unit) : TangemPayVerifiedUM

    data class Content(
        val description: TextReference,
        val moneyAccount: ProductUM,
        val visaCard: ProductUM,
        val primaryButton: ButtonUM,
        override val onCloseClick: () -> Unit,
    ) : TangemPayVerifiedUM

    data class ProductUM(
        val title: TextReference,
        val subtitle: TextReference,
        val isAvailable: Boolean,
        val badge: TangemBadgeUM,
    )

    data class ButtonUM(
        val text: TextReference,
        val iconEnd: TangemIconUM?,
        val onClick: () -> Unit,
    )
}