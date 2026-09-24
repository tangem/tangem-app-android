package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion

interface TangemPayCountrySelectorComponent : ComposableContentComponent {

    sealed interface Params {

        val onDismiss: () -> Unit

        /**
         * @property selectedCountryCode already picked country, shown in its own section above the full list.
         * @property selectedRegionCode already picked region, highlighted in the region step only when it belongs
         * to [selectedCountryCode].
         */
        data class Countries(
            override val onDismiss: () -> Unit,
            val onSelected: (country: TangemPayCountry, region: TangemPayRegion?) -> Unit,
            val selectedCountryCode: String? = null,
            val selectedRegionCode: String? = null,
        ) : Params

        /** @property selectedRegionCode already picked region, shown in its own section above the full list. */
        data class Regions(
            val countryCode: String,
            override val onDismiss: () -> Unit,
            val onSelected: (region: TangemPayRegion) -> Unit,
            val selectedRegionCode: String? = null,
        ) : Params
    }

    interface Factory : ComponentFactory<Params, TangemPayCountrySelectorComponent>
}