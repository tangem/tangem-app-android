package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.commonfeatures.impl.R
import com.tangem.utils.CountryNames

internal object TokenActionsRegionRestriction {

    /**
     * The notice explaining why the trading rows are inert.
     *
     * Names the region when the platform can name it, and falls back to the region-less wording when it
     * cannot — a bare country code reads worse than saying nothing. The restriction itself is reported
     * either way.
     *
     * @param countryCode ISO 3166-1 alpha-2 code of the user's region, e.g. `GB`
     */
    fun banner(countryCode: String?): TangemMessageBanner.State {
        val regionName = CountryNames.resolveDisplayName(countryCode = countryCode)

        return TangemMessageBanner.State(
            title = resourceReference(R.string.buy_swap_region_restriction_title),
            description = if (regionName == null) {
                resourceReference(R.string.buy_swap_region_restriction_description)
            } else {
                resourceReference(
                    id = R.string.buy_swap_region_restriction_description_region,
                    formatArgs = wrappedList(regionName),
                )
            },
            variant = TangemMessageBanner.Variant.Warning,
            iconEnd = TangemIconUM.Icon(
                imageVector = Icons.ic_info_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}