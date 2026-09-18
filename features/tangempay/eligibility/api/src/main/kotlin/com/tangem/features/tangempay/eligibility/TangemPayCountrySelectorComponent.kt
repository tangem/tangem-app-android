package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion

interface TangemPayCountrySelectorComponent : ComposableContentComponent {

    sealed interface Params {

        val onDismiss: () -> Unit

        /**
         * @property shouldIncludeRegions `false` keeps the sheet on the country list: an address form picks the US
         * state from its own region field, while residency onboarding needs the state right after the country.
         */
        data class Countries(
            override val onDismiss: () -> Unit,
            val onSelected: (country: TangemPayCountry, region: TangemPayRegion?) -> Unit,
            val shouldIncludeRegions: Boolean = true,
        ) : Params

        data class Regions(
            val countryCode: String,
            override val onDismiss: () -> Unit,
            val onSelected: (region: TangemPayRegion) -> Unit,
        ) : Params
    }

    interface Factory : ComponentFactory<Params, TangemPayCountrySelectorComponent>
}