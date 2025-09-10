package com.tangem.features.onramp.alloffers.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.features.onramp.mainv2.entity.OnrampOfferUM
import kotlinx.collections.immutable.ImmutableList

internal sealed interface AllOffersStateUM {

    data object Loading : AllOffersStateUM

    data class Content(
        val methods: ImmutableList<AllOffersPaymentMethodUM>,
        val currentMethod: AllOffersPaymentMethodUM? = null,
        val onBackClicked: () -> Unit,
    ) : AllOffersStateUM

    data class Error(val errorNotification: NotificationUM) : AllOffersStateUM
}

internal data class AllOffersPaymentMethodUM(
    val offers: ImmutableList<OnrampOfferUM>,
    val methodConfig: OnrampPaymentMethodConfig,
    val diff: TextReference?,
    val rate: String,
    val providersCount: Int,
    val isBestRate: Boolean,
)

internal data class OnrampPaymentMethodConfig(
    val method: OnrampPaymentMethod,
    val onClick: () -> Unit,
)