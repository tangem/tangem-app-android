package com.tangem.features.commonfeatures.api.addtoportfolio

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface AddToPortfolioComponent : ComposableBottomSheetComponent {

    /**
     * @param isRegionRestricted the asset is blocked in the user's region: adding it to a portfolio stays
     * allowed, but the token actions shown afterwards close buying and swapping and say why.
     */
    data class Params(
        val addToPortfolioManager: AddToPortfolioManager,
        val isRegionRestricted: Boolean = false,
    )

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}