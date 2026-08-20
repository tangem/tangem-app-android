package com.tangem.features.onramp.alloffers.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampOfferUM
import kotlinx.collections.immutable.ImmutableList

internal sealed interface AllOffersStateUM {

    data object Loading : AllOffersStateUM

    data class Content(
        val methods: ImmutableList<AllOffersPaymentMethodUM>,
        val currentMethod: AllOffersPaymentMethodUM? = null,
        val restrictedNotification: NotificationUM? = null,
        val onBackClicked: () -> Unit,
    ) : AllOffersStateUM

    data class Error(val errorNotification: NotificationUM) : AllOffersStateUM
}

internal data object OnrampRegionRestrictionNotification : NotificationUM.Warning(
    title = resourceReference(R.string.express_onramp_restrictions_title),
    subtitle = resourceReference(R.string.express_onramp_restrictions_text),
)

internal data class AllOffersPaymentMethodUM(
    val offers: ImmutableList<OnrampOfferUM>,
    val methodConfig: OnrampPaymentMethodConfig,
    val diff: TextReference?,
    val rate: String,
    val providersCount: Int,
    val isBestRate: Boolean,
    val paymentMethodStatus: PaymentMethodStatus,
)

internal data class OnrampPaymentMethodConfig(
    val method: OnrampPaymentMethod,
    val onClick: () -> Unit,
)