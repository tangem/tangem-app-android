package com.tangem.features.addressbook

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultAddressBookFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : AddressBookFeatureToggles {
    override val isAddressBookEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_83_ADDRESS_BOOK_ENABLED)
}