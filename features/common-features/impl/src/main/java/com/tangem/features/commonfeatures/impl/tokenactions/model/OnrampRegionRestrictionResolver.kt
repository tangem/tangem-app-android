package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import javax.inject.Inject

internal class OnrampRegionRestrictionResolver @Inject constructor(
    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) {

    val isEnabled: Boolean get() = onrampFeatureToggles.isCountryByIpEnabled

    suspend fun resolve(userWallet: UserWallet): OnrampRegionState {
        val country = getOnrampCountryByIpUseCase(userWallet).getOrNull() ?: return OnrampRegionState.Unknown
        return if (country.isOnrampAvailable) OnrampRegionState.Allowed else OnrampRegionState.Restricted(country.name)
    }
}

internal sealed interface OnrampRegionState {
    data object Checking : OnrampRegionState
    data object Allowed : OnrampRegionState
    data class Restricted(val countryName: String) : OnrampRegionState
    data object Unknown : OnrampRegionState
}