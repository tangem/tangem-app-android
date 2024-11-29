package com.tangem.datasource.local.onramp.paymentmethods

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultOnrampPaymentMethodsStore(
    dataStore: StringKeyDataStore<List<PaymentMethodDTO>>,
) : OnrampPaymentMethodsStore, StringKeyDataStoreDecorator<String, List<PaymentMethodDTO>>(dataStore) {

    override fun provideStringKey(key: String): String = key
}