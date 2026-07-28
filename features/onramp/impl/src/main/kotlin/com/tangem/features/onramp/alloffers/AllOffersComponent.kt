package com.tangem.features.onramp.alloffers

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.features.marketing.api.MarketingBannerComponent

internal interface AllOffersComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: () -> Unit,
        val openRedirectPage: (quote: OnrampProviderWithQuote.Data) -> Unit,
        val amountCurrencyCode: String,
        // Marketing banner components are created and owned by the parent onramp-main component and passed
        // down so this sheet reuses their models (and their amount-gated request flows) instead of building
        // its own: [marketingBannerComponent] renders the standalone banner, [linkedMarketingBannerComponent]
        // renders the per-provider LINKED_TO_PROVIDER banner next to each offer.
        val marketingBannerComponent: MarketingBannerComponent,
        val linkedMarketingBannerComponent: MarketingBannerComponent,
    )

    interface Factory : ComponentFactory<Params, AllOffersComponent>
}