package com.tangem.features.onramp.providers.model.previewData

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.onramp.model.*
import com.tangem.features.onramp.providers.entity.PaymentProviderUM
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.SelectPaymentAndProviderUM
import com.tangem.features.onramp.providers.entity.SelectProviderResult
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object SelectProviderPreviewData {

    private val paymentMethod = OnrampPaymentMethod(
        id = "card",
        name = "Card",
        imageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/PaymentMethods/visa-mc.png",
        type = PaymentMethodType.CARD,
    )

    private val provider = OnrampProvider(
        id = "mercuryo",
        info = OnrampProviderInfo(
            name = "Mercuryo",
            imageLarge = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/mercuryo1024.png",
            termsOfUseLink = null,
            privacyPolicyLink = null,
        ),
        paymentMethods = listOf(),
    )

    private val providerListUM = PaymentProviderUM(
        paymentMethod = paymentMethod,
        providers = persistentListOf(
            ProviderListItemUM.Available.Content(
                providerId = provider.id,
                imageUrl = provider.info.imageLarge,
                name = provider.info.name,
                rate = "1 BTC",
                isSelected = true,
                isBestRate = true,
                diffRate = TextReference.EMPTY,
                providerResult = SelectProviderResult.ProviderWithQuote(
                    paymentMethod = paymentMethod,
                    provider = provider,
                    fromAmount = OnrampAmount(symbol = "$", value = BigDecimal.ONE, decimals = 2),
                    toAmount = OnrampAmount(symbol = "BTC", value = BigDecimal.ONE, decimals = 18),

                ),
                onClick = {},
            ),
            ProviderListItemUM.Available.Content(
                providerId = provider.id,
                imageUrl = provider.info.imageLarge,
                name = provider.info.name,
                rate = "1 BTC",
                isSelected = false,
                isBestRate = false,
                diffRate = stringReference("-0.03%"),
                providerResult = SelectProviderResult.ProviderWithQuote(
                    paymentMethod = paymentMethod,
                    provider = provider,
                    fromAmount = OnrampAmount(symbol = "$", value = BigDecimal.ONE, decimals = 2),
                    toAmount = OnrampAmount(symbol = "BTC", value = BigDecimal.ONE, decimals = 18),
                ),
                onClick = {},
            ),
            ProviderListItemUM.Unavailable(
                providerId = provider.id,
                imageUrl = provider.info.imageLarge,
                name = provider.info.name,
                subtitle = stringReference("Avaiable from 0,00413 BTC"),
            ),
        ),
    )

    val state = SelectPaymentAndProviderUM(
        paymentMethods = persistentListOf(providerListUM),
        selectedPaymentMethod = providerListUM,
        selectedProviderId = provider.id,
        isPaymentMethodClickEnabled = false,
        onPaymentMethodClick = {},
    )
}