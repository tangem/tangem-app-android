package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import javax.inject.Inject

internal class OnrampRegionRestrictionResolver @Inject constructor(
    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) {

    /** Name of the country detected by IP when onramp is unavailable there; `null` when it is available or unknown. */
    suspend fun resolveRestrictedRegionName(userWallet: UserWallet): String? {
        if (!onrampFeatureToggles.isCountryByIpEnabled) {
            return null
        }
        val country = getOnrampCountryByIpUseCase(userWallet).getOrNull() ?: return null
        return country.name.takeUnless { country.isOnrampAvailable }
    }
}