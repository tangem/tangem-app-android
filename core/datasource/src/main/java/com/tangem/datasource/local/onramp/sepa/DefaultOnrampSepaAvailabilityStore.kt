package com.tangem.datasource.local.onramp.sepa

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultOnrampSepaAvailabilityStore(
    val dataStore: StringKeyDataStore<Boolean>,
) : OnrampSepaAvailabilityStore, StringKeyDataStoreDecorator<OnrampSepaAvailabilityStoreKey, Boolean>(
    wrappedDataStore = dataStore,
) {
    override fun provideStringKey(key: OnrampSepaAvailabilityStoreKey) = with(key) {
        buildString {
            append(userWallet.walletId.toString())
            append("_")
            append(country.code)
            append("_")
            append(cryptoCurrency.id.value)
        }
    }
}