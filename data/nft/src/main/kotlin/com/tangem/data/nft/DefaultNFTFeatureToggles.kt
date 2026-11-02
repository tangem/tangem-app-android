package com.tangem.data.nft

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.nft.NFTFeatureToggles
import javax.inject.Inject

internal class DefaultNFTFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : NFTFeatureToggles {

    override val isSolanaPnftAndCoreNftEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1822_SOLANA_PNFT_AND_METAPLEX_CORE_ENABLED)
}