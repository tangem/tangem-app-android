package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion

interface TangemPayCountrySelectorComponent : ComposableContentComponent {

    sealed interface Params {

        val onDismiss: () -> Unit

        data class Countries(
            override val onDismiss: () -> Unit,
            val onSelected: (country: TangemPayCountry, region: TangemPayRegion?) -> Unit,
        ) : Params

        data class Regions(
            val countryCode: String,
            override val onDismiss: () -> Unit,
            val onSelected: (region: TangemPayRegion) -> Unit,
        ) : Params
    }

    interface Factory : ComponentFactory<Params, TangemPayCountrySelectorComponent>
}