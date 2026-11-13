package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import javax.inject.Inject

internal class OnrampRegionRestrictionResolver @Inject constructor(
    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase,
    private val featureTogglesManager: FeatureTogglesManager,
) {

    /** Name of the country detected by IP when onramp is unavailable there; `null` when it is available or unknown. */
    suspend fun resolveRestrictedRegionName(userWallet: UserWallet): String? {
        if (!featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18954_ONRAMP_COUNTRY_BY_IP_ENABLED)) {
            return null
        }
        val country = getOnrampCountryByIpUseCase(userWallet).getOrNull() ?: return null
        return country.name.takeUnless { country.isOnrampAvailable }
    }
}