package com.tangem.features.onramp.providers.model.previewData

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.SelectPaymentAndProviderUM
import com.tangem.features.onramp.providers.entity.SelectProviderUM
import kotlinx.collections.immutable.persistentListOf

internal object SelectProviderPreviewData {

    private val paymentMethods = persistentListOf(
        OnrampPaymentMethod(
            id = "card",
            name = "Card",
            imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
            type = PaymentMethodType.CARD,
        ),
    )

    private val providerListUM = SelectProviderUM(
        paymentMethod = paymentMethods.first(),
        providers = persistentListOf(
            ProviderListItemUM.Available(
                providerId = "mercuryo",
                imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/mercuryo1024.png",
                name = "Mercuryo",
                rate = "1 BTC",
                isSelected = true,
                isBestRate = true,
                diffRate = TextReference.EMPTY,
                onClick = {},
            ),
            ProviderListItemUM.Available(
                providerId = "mercuryo",
                imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/mercuryo1024.png",
                name = "Mercuryo",
                rate = "1 BTC",
                isSelected = false,
                isBestRate = false,
                diffRate = stringReference("-0.03%"),
                onClick = {},
            ),
            ProviderListItemUM.Unavailable(
                providerId = "mercuryo",
                imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/mercuryo1024.png",
                name = "Mercuryo",
                subtitle = stringReference("Avaiable from 0,00413 BTC"),
            ),
        ),
    )

    val state = SelectPaymentAndProviderUM(
        paymentMethods = paymentMethods,
        selectedPaymentMethod = providerListUM,
        isPaymentMethodClickEnabled = false,
        onPaymentMethodClick = {},
    )
}