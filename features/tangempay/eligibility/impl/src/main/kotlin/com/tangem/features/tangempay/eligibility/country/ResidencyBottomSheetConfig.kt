package com.tangem.features.tangempay.eligibility.country

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ResidencyBottomSheetConfig {

    @Serializable
    data class WalletSelector(val walletsIds: List<UserWalletId>) : ResidencyBottomSheetConfig()
}