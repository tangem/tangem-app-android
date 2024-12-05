package com.tangem.features.onramp.providers.model.previewData

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.ProviderListPaymentMethodUM
import com.tangem.features.onramp.providers.entity.ProviderListUM
import kotlinx.collections.immutable.persistentListOf

internal object SelectProviderPreviewData {

    val state = ProviderListUM(
        paymentMethod = ProviderListPaymentMethodUM(
            id = "card",
            name = "Card",
            imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
            onClick = {},
        ),
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
}